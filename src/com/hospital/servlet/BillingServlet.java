package com.hospital.servlet;

import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

/**
 * BillingServlet.java
 * GET  /api/billing  → all bills (JOIN patients)
 * POST /api/billing  → add bill
 * DELETE /api/billing?id=1 → delete
 * Project ID: 25FE5A4305
 */
public class BillingServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        String sql = "SELECT b.*, p.name AS patient_name FROM Billing b " +
                     "JOIN Patients p ON b.patient_id=p.patient_id ORDER BY b.billing_date DESC";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                    .append("\"bill_id\":").append(rs.getInt("bill_id")).append(",")
                    .append("\"patient_id\":").append(rs.getInt("patient_id")).append(",")
                    .append("\"patient_name\":\"").append(s(rs.getString("patient_name"))).append("\",")
                    .append("\"amount\":").append(rs.getDouble("amount")).append(",")
                    .append("\"billing_date\":\"").append(s(rs.getString("billing_date"))).append("\"")
                    .append("}");
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
                 "INSERT INTO Billing (patient_id, amount, billing_date) VALUES (?,?,?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, Integer.parseInt(req.getParameter("patient_id")));
            ps.setDouble(2, Double.parseDouble(req.getParameter("amount")));
            ps.setString(3, req.getParameter("billing_date"));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            int id = keys.next() ? keys.getInt(1) : -1;
            out.print("{\"success\":true,\"message\":\"Bill added\",\"bill_id\":" + id + "}");
        } catch (SQLException e) {
            res.setStatus(500); out.print("{\"success\":false,\"message\":\"" + s(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Billing WHERE bill_id=?")) {
            ps.setInt(1, Integer.parseInt(req.getParameter("id")));
            int rows = ps.executeUpdate();
            out.print("{\"success\":" + (rows>0) + ",\"message\":\"" + (rows>0?"Deleted":"Not found") + "\"}");
        } catch (SQLException e) {
            res.setStatus(500); out.print("{\"success\":false,\"message\":\"" + s(e.getMessage()) + "\"}");
        }
    }
    private String s(String v) { return v == null ? "" : v.replace("\"","'"); }
}
