package com.hospital.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

public class DoctorServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        String sql = "SELECT d.*, dep.department_name FROM Doctors d " +
                     "LEFT JOIN Departments dep ON d.department_id = dep.department_id " +
                     "ORDER BY d.doctor_id";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                    .append("\"doctor_id\":").append(rs.getInt("doctor_id")).append(",")
                    .append("\"name\":\"").append(s(rs.getString("name"))).append("\",")
                    .append("\"specialization\":\"").append(s(rs.getString("specialization"))).append("\",")
                    .append("\"department_id\":").append(rs.getInt("department_id")).append(",")
                    .append("\"department_name\":\"").append(s(rs.getString("department_name"))).append("\",")
                    .append("\"email\":\"").append(s(rs.getString("email"))).append("\"")
                    .append("}");
                first = false;
            }
            json.append("]");
            out.print("{\"success\":true,\"data\":" + json + "}");
        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"" + s(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        String sql = "INSERT INTO Doctors (name, specialization, department_id, email, password) VALUES (?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, req.getParameter("name"));
            ps.setString(2, req.getParameter("specialization"));
            ps.setInt(3, Integer.parseInt(req.getParameter("department_id")));
            ps.setString(4, req.getParameter("email") != null ? req.getParameter("email") : "");
            ps.setString(5, req.getParameter("password") != null ? req.getParameter("password") : "doctor123");
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            int id = keys.next() ? keys.getInt(1) : -1;
            out.print("{\"success\":true,\"message\":\"Doctor added\",\"doctor_id\":" + id + "}");
        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"" + s(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        String idStr = req.getParameter("id");
        if (idStr == null) {
            res.setStatus(400);
            out.print("{\"success\":false,\"message\":\"ID required\"}");
            return;
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Doctors WHERE doctor_id=?")) {
            ps.setInt(1, Integer.parseInt(idStr));
            int rows = ps.executeUpdate();
            out.print("{\"success\":" + (rows > 0) + ",\"message\":\"" + (rows > 0 ? "Deleted" : "Not found") + "\"}");
        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"" + s(e.getMessage()) + "\"}");
        }
    }

    private String s(String v) {
        return v == null ? "" : v.replace("\"", "'");
    }
}
