package com.softclass.fingerprint;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class FingerprintMainApp extends Application {

    @Override
    public void start(Stage stage) {
        Button btnEmployees = new Button("Gestión de Empleados");
        Button btnAttendance = new Button("Control de Asistencia");
        Button btnReports = new Button("Reportes");

        VBox root = new VBox(15, btnEmployees, btnAttendance, btnReports);
        root.setStyle("-fx-padding: 30; -fx-alignment: center;");

        btnEmployees.setOnAction(e -> new EmployeeView().showWindow());
        // (más adelante) btnAttendance.setOnAction(e -> new AttendanceView().showWindow());
        btnReports.setOnAction(e -> new ReportView().showWindow());

        stage.setScene(new Scene(root, 400, 300));
        stage.setTitle("Sistema de Control de Huellas");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
