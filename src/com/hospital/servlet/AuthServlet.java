package com.hospital.servlet;

import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import java.io.*;
import java.sql.*;

/**
 * AuthServlet.java
 * Handles Doctor Login and Signup via JDBC.
 *
 * POST /api/auth?action=login   → { email, password }
 * POST /api/auth?action=signup  → { name, specialization, department_id, email, password }
 *
 * Project ID: 25FE5A4305
 */
public class AuthServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String action = req.getParameter("action");
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();

        if ("login".equals(action)) {
            handleLogin(req, res, out);
        } else if ("signup".equals(action)) {
            handleSignup(req, res, out);
        } else {
            res.setStatus(400);
            out.print("{\"success\":false,\"message\":\"Unknown action\"}");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse res, PrintWriter out)
            throws IOException {
        String email    = req.getParameter("email");
        String password = req.getParameter("password");

        if (email == null || password == null) {
            res.setStatus(400);
            out.print("{\"success\":false,\"message\":\"Email and password required\"}");
            return;
        }

        String sql = "SELECT d.*, dep.department_name FROM Doctors d " +
                     "LEFT JOIN Departments dep ON d.department_id = dep.department_id " +
                     "WHERE d.email = ? AND d.password = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                out.print("{" +
                    "\"success\":true," +
                    "\"message\":\"Login successful\"," +
                    "\"doctor\":{" +
                        "\"doctor_id\":"      + rs.getInt("doctor_id")          + "," +
                        "\"name\":\""         + rs.getString("name")            + "\"," +
                        "\"specialization\":\"" + rs.getString("specialization") + "\"," +
                        "\"department_id\":"  + rs.getInt("department_id")      + "," +
                        "\"department_name\":\"" + rs.getString("department_name") + "\"," +
                        "\"email\":\""        + rs.getString("email")           + "\"" +
                    "}" +
                "}");
            } else {
                res.setStatus(401);
                out.print("{\"success\":false,\"message\":\"Invalid email or password\"}");
            }

        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"DB Error: " + e.getMessage().replace("\"","'") + "\"}");
        }
    }

    private void handleSignup(HttpServletRequest req, HttpServletResponse res, PrintWriter out)
            throws IOException {
        String name         = req.getParameter("name");
        String spec         = req.getParameter("specialization");
        String deptIdStr    = req.getParameter("department_id");
        String email        = req.getParameter("email");
        String password     = req.getParameter("password");

        if (name == null || email == null || password == null || spec == null || deptIdStr == null) {
            res.setStatus(400);
            out.print("{\"success\":false,\"message\":\"All fields are required\"}");
            return;
        }

        // Check duplicate email
        String checkSql = "SELECT doctor_id FROM Doctors WHERE email = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement check = con.prepareStatement(checkSql)) {
            check.setString(1, email);
            if (check.executeQuery().next()) {
                res.setStatus(409);
                out.print("{\"success\":false,\"message\":\"Email already registered\"}");
                return;
            }
        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"DB Error: " + e.getMessage().replace("\"","'") + "\"}");
            return;
        }

        // Insert new doctor
        String sql = "INSERT INTO Doctors (name, specialization, department_id, email, password) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, spec);
            ps.setInt(3, Integer.parseInt(deptIdStr));
            ps.setString(4, email);
            ps.setString(5, password);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            int newId = keys.next() ? keys.getInt(1) : -1;

            out.print("{\"success\":true,\"message\":\"Doctor registered successfully\"," +
                      "\"doctor_id\":" + newId + "}");

        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"DB Error: " + e.getMessage().replace("\"","'") + "\"}");
        }
    }
}
