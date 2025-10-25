package com.softclass.fingerprint;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class EmployeeView {

    private final EmployeeController employeeController = new EmployeeController();
    private final TableView<Employee> table = new TableView<>();
    private final TextField txtName = new TextField();
    private final TextField txtDocument = new TextField();
    private final Button btnSave = new Button("Guardar");
    private final Button btnEnroll = new Button("Capturar Huella");
    private final Button btnUpdate = new Button("Actualizar");

    private FingerprintService fingerprintService;

    public void showWindow() {
        Stage stage = new Stage();

        try {
            Database.init();
            fingerprintService = new FingerprintService();
        } catch (Exception e) {
            showError("Error inicializando lector o base: " + e.getMessage());
            return;
        }

        // --- Formulario ---
        GridPane form = new GridPane();
        form.setPadding(new Insets(10));
        form.setHgap(10);
        form.setVgap(10);
        form.add(new Label("Nombre:"), 0, 0);
        form.add(txtName, 1, 0);
        form.add(new Label("Documento:"), 0, 1);
        form.add(txtDocument, 1, 1);
        form.add(btnSave, 0, 2);
        form.add(btnEnroll, 1, 2);
        form.add(btnUpdate, 2, 2);

        // --- Tabla ---
        TableColumn<Employee, String> colName = new TableColumn<>("Nombre");
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().name));

        TableColumn<Employee, String> colDoc = new TableColumn<>("Documento");
        colDoc.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().document));

        TableColumn<Employee, String> colFP = new TableColumn<>("Huella Registrada");
        colFP.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().fingerprintBase64 != null ? "✅ Sí" : "No"
        ));

        table.getColumns().addAll(colName, colDoc, colFP);
        refreshTable();

        btnSave.setOnAction(e -> onSave());
        btnEnroll.setOnAction(e -> onEnroll());
        btnUpdate.setOnAction(e -> onUpdate());

        VBox root = new VBox(10, form, table);
        root.setPadding(new Insets(10));

        stage.setTitle("Gestión de Empleados");
        stage.setScene(new Scene(root, 600, 400));

        stage.setOnCloseRequest(e -> {
            if (fingerprintService != null) fingerprintService.close();
        });

        stage.show();
    }

    private void onSave() {
        try {
            Employee e = new Employee();
            e.name = txtName.getText();
            e.document = txtDocument.getText();
            employeeController.save(e);
            refreshTable();
            txtName.clear();
            txtDocument.clear();
            showAlert("Empleado guardado correctamente");
        } catch (SQLException ex) {
            showError("Error al guardar empleado: " + ex.getMessage());
        }
    }

    private void onUpdate() {
        Employee selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selecciona un empleado para actualizar");
            return;
        }
        try {
            selected.name = txtName.getText();
            selected.document = txtDocument.getText();
            employeeController.update(selected);
            refreshTable();
            showAlert("Empleado actualizado correctamente");
        } catch (SQLException ex) {
            showError("Error al actualizar empleado: " + ex.getMessage());
        }
    }

    private void onEnroll() {
        Employee selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selecciona un empleado para capturar su huella");
            return;
        }

        try {
            String tmpl = fingerprintService.enrollFingerprint();
            employeeController.updateFingerprint(selected.id, tmpl);
            refreshTable();
            showAlert("Huella enrolada correctamente");
        } catch (Exception ex) {
            showError("Error al enrolar huella: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void refreshTable() {
        try {
            List<Employee> list = employeeController.getAll();
            table.getItems().setAll(list);
        } catch (SQLException ex) {
            showError("Error al cargar empleados: " + ex.getMessage());
        }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

}
