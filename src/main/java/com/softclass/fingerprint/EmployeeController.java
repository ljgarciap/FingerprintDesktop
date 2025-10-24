package com.softclass.fingerprint;

import java.sql.*;
import java.util.*;

public class EmployeeController {

    public List<Employee> getAll() throws SQLException {
        List<Employee> list = new ArrayList<>();
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM employee")) {
            while (rs.next()) {
                Employee e = new Employee();
                e.id = rs.getInt("id");
                e.name = rs.getString("name");
                e.document = rs.getString("document");
                e.fingerprintBase64 = rs.getString("fingerprint");
                list.add(e);
            }
        }
        return list;
    }

    public void save(Employee e) throws SQLException {
        try (PreparedStatement ps = Database.get().prepareStatement(
                "INSERT INTO employee(name, document, fingerprint) VALUES(?,?,?)")) {
            ps.setString(1, e.name);
            ps.setString(2, e.document);
            ps.setString(3, e.fingerprintBase64);
            ps.executeUpdate();
        }
    }

    public void updateFingerprint(int id, String tmpl) throws SQLException {
        try (PreparedStatement ps = Database.get().prepareStatement(
                "UPDATE employee SET fingerprint=? WHERE id=?")) {
            ps.setString(1, tmpl);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void update(Employee e) throws SQLException {
        try (PreparedStatement ps = Database.get().prepareStatement(
                "UPDATE employee SET name=?, document=? WHERE id=?")) {
            ps.setString(1, e.name);
            ps.setString(2, e.document);
            ps.setInt(3, e.id);
            ps.executeUpdate();
        }
    }

}
