package com.softclass.fingerprint;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class AttendanceController {

    @FXML private ListView<Employee> employeeList;
    @FXML private Label statusLabel;

    private final EmployeeController employeeController = new EmployeeController();
    private FingerprintService fingerprintService;

    @FXML
    public void initialize() {
        try {
            fingerprintService = new FingerprintService();
            refreshEmployees();
        } catch (Exception e) {
            statusLabel.setText("Error init: " + e.getMessage());
        }
    }

    private void refreshEmployees() throws SQLException {
        employeeList.getItems().setAll(employeeController.getAll());
        employeeList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Employee e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) {
                    setText(null);
                } else {
                    String status = (e.fingerprintBase64 != null && !e.fingerprintBase64.isBlank())
                            ? "✅ Enrolled"
                            : "❌ Not Enrolled";
                    setText(e.name + " (" + e.document + ") - " + status);
                }
            }
        });
    }

    @FXML
    public void onAddEmployee() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Enter employee name:");
        dialog.showAndWait().ifPresent(name -> {
            try {
                Employee e = new Employee();
                e.name = name;
                e.document = "N/A";
                employeeController.save(e);
                refreshEmployees();
                statusLabel.setText("Added " + name);
            } catch (SQLException ex) {
                statusLabel.setText("DB error: " + ex.getMessage());
            }
        });
    }

    @FXML
    public void onEnroll() {
        Employee sel = employeeList.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setText("Select employee first");
            return;
        }
        try {
            String tmpl = fingerprintService.enrollFingerprint();
            employeeController.updateFingerprint(sel.id, tmpl);
            refreshEmployees();
            statusLabel.setText("Fingerprint enrolled for " + sel.name);
        } catch (Exception e) {
            statusLabel.setText("Enroll error: " + e.getMessage());
        }
    }

    @FXML
    public void onCheckIn() { registerAttendance("IN"); }

    @FXML
    public void onCheckOut() { registerAttendance("OUT"); }

    private void registerAttendance(String type) {
        try {
            String live = fingerprintService.enrollFingerprint();
            List<Employee> employees = employeeController.getAll();
            for (Employee e : employees) {
                if (e.fingerprintBase64 == null) continue;
                if (fingerprintService.match(e.fingerprintBase64, live)) {
                    try (var ps = Database.get().prepareStatement(
                            "INSERT INTO attendance(employee_id, timestamp, type) VALUES(?,?,?)")) {
                        ps.setInt(1, e.id);
                        ps.setString(2, LocalDateTime.now().toString());
                        ps.setString(3, type);
                        ps.executeUpdate();
                    }
                    statusLabel.setText(type + " registered for " + e.name);
                    return;
                }
            }
            statusLabel.setText("No match found");
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    @FXML
    public void onEditEmployee() {
        Employee sel = employeeList.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setText("Select employee to edit");
            return;
        }

        Dialog<Employee> dialog = new Dialog<>();
        dialog.setTitle("Edit Employee");
        dialog.setHeaderText("Modify employee information:");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField nameField = new TextField(sel.name);
        TextField docField = new TextField(sel.document);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Document:"), 0, 1);
        grid.add(docField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                sel.name = nameField.getText();
                sel.document = docField.getText();
                return sel;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            try {
                employeeController.update(updated);
                refreshEmployees();
                statusLabel.setText("Updated " + updated.name);
            } catch (SQLException ex) {
                statusLabel.setText("DB error: " + ex.getMessage());
            }
        });
    }

    @FXML
    public void onShowReports() {
        try {
            new ReportWindow().show(); // esto abrirá una ventana simple de reportes
        } catch (Exception e) {
            statusLabel.setText("Error opening reports: " + e.getMessage());
        }
    }

}
