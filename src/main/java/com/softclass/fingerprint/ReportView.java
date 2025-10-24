package com.softclass.fingerprint;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportView {

    private final TableView<AttendanceRecord> table = new TableView<>();

    public void showWindow() {
        Stage stage = new Stage();

        TableColumn<AttendanceRecord, String> colName = new TableColumn<>("Empleado");
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().employeeName));

        TableColumn<AttendanceRecord, String> colType = new TableColumn<>("Tipo");
        colType.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().type));

        TableColumn<AttendanceRecord, String> colTime = new TableColumn<>("Fecha y Hora");
        colTime.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().timestamp));

        table.getColumns().addAll(colName, colType, colTime);
        refreshTable();

        VBox root = new VBox(10, table);
        root.setPadding(new Insets(10));

        stage.setScene(new Scene(root, 600, 400));
        stage.setTitle("Reporte de Asistencia");
        stage.show();
    }

    private void refreshTable() {
        try (var st = Database.get().createStatement();
             var rs = st.executeQuery("""
                    SELECT e.name AS employee_name, a.type, a.timestamp
                    FROM attendance a
                    JOIN employee e ON a.employee_id = e.id
                    ORDER BY a.timestamp DESC
                    """)) {
            List<AttendanceRecord> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new AttendanceRecord(
                        rs.getString("employee_name"),
                        rs.getString("type"),
                        rs.getString("timestamp")
                ));
            }
            table.getItems().setAll(list);
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Error cargando reporte: " + ex.getMessage()).showAndWait();
        }
    }

    public static class AttendanceRecord {
        public final String employeeName;
        public final String type;
        public final String timestamp;

        public AttendanceRecord(String employeeName, String type, String timestamp) {
            this.employeeName = employeeName;
            this.type = type;
            this.timestamp = timestamp;
        }
    }
}
