package com.hospital.servlet;

import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

/**
 * DepartmentServlet.java
 * GET    /api/departments  → list all
 * POST   /api/departments  → add
 * DELETE /api/departments?id=1 → delete
 * Project ID: 25FE5A4305
 */
public class DepartmentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM Departments ORDER BY department_id")) {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{\"department_id\":").append(rs.getInt("department_id"))
                    .append(",\"department_name\":\"").append(s(rs.getString("department_name"))).append("\"}");
                first = false;
            }
            json.append("]");
            out.print("{\"success\":true,\"data\":" + json + "}");
        } catch (SQLException e) {
            res.setStatus(500); out.print("{\"success\":false,\"message\":\"" + s(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "INSERT INTO Departments (department_name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, req.getParameter("department_name"));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            int id = keys.next() ? keys.getInt(1) : -1;
            out.print("{\"success\":true,\"message\":\"Department added\",\"department_id\":" + id + "}");
        } catch (SQLException e) {
            res.setStatus(500); out.print("{\"success\":false,\"message\":\"" + s(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Departments WHERE department_id=?")) {
            ps.setInt(1, Integer.parseInt(req.getParameter("id")));
            int rows = ps.executeUpdate();
            out.print("{\"success\":" + (rows>0) + ",\"message\":\"" + (rows>0?"Deleted":"Not found") + "\"}");
        } catch (SQLException e) {
            res.setStatus(500); out.print("{\"success\":false,\"message\":\"" + s(e.getMessage()) + "\"}");
        }
    }
    private String s(String v) { return v == null ? "" : v.replace("\"","'"); }
}
