package com.hospital.servlet;

import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

/**
 * PatientServlet.java
 * GET    /api/patients          → list all patients
 * GET    /api/patients?id=1     → get patient by ID
 * POST   /api/patients          → add patient
 * PUT    /api/patients?id=1     → update patient
 * DELETE /api/patients?id=1     → delete patient
 * GET    /api/patients?action=history&id=1 → stored procedure
 *
 * Project ID: 25FE5A4305
 */
public class PatientServlet extends HttpServlet {

    // GET — List all or by ID or history
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        String action = req.getParameter("action");
        String idStr  = req.getParameter("id");

        // STORED PROCEDURE: GetPatientHistory
        if ("history".equals(action) && idStr != null) {
            getHistory(Integer.parseInt(idStr), res, out);
            return;
        }

        // Get by ID
        if (idStr != null) {
            getById(Integer.parseInt(idStr), res, out);
            return;
        }

        // Get all
        String sql = "SELECT * FROM Patients ORDER BY patient_id";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append(patientToJson(rs));
                first = false;
            }
            json.append("]");
            out.print("{\"success\":true,\"data\":" + json + "}");

        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"" + e.getMessage().replace("\"","'") + "\"}");
        }
    }

    // POST — Add patient
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();

        String name    = req.getParameter("name");
        String dob     = req.getParameter("dob");
        String contact = req.getParameter("contact");
        String address = req.getParameter("address");

        if (name == null || name.isEmpty()) {
            res.setStatus(400);
            out.print("{\"success\":false,\"message\":\"Name is required\"}");
            return;
        }

        String sql = "INSERT INTO Patients (name, dob, contact, address) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, dob);
            ps.setString(3, contact);
            ps.setString(4, address);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            int newId = keys.next() ? keys.getInt(1) : -1;
            out.print("{\"success\":true,\"message\":\"Patient added\",\"patient_id\":" + newId + "}");

        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"" + e.getMessage().replace("\"","'") + "\"}");
        }
    }

    // PUT — Update patient
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        String idStr   = req.getParameter("id");
        String contact = req.getParameter("contact");
        String address = req.getParameter("address");

        if (idStr == null) { res.setStatus(400); out.print("{\"success\":false,\"message\":\"ID required\"}"); return; }

        String sql = "UPDATE Patients SET contact=?, address=? WHERE patient_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, contact); ps.setString(2, address); ps.setInt(3, Integer.parseInt(idStr));
            int rows = ps.executeUpdate();
            out.print("{\"success\":" + (rows>0) + ",\"message\":\"" + (rows>0?"Updated":"Not found") + "\"}");
        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"" + e.getMessage().replace("\"","'") + "\"}");
        }
    }

    // DELETE — Delete patient
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        String idStr = req.getParameter("id");
        if (idStr == null) { res.setStatus(400); out.print("{\"success\":false,\"message\":\"ID required\"}"); return; }

        String sql = "DELETE FROM Patients WHERE patient_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idStr));
            int rows = ps.executeUpdate();
            out.print("{\"success\":" + (rows>0) + ",\"message\":\"" + (rows>0?"Deleted":"Not found") + "\"}");
        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"" + e.getMessage().replace("\"","'") + "\"}");
        }
    }

    private void getById(int id, HttpServletResponse res, PrintWriter out) throws IOException {
        String sql = "SELECT * FROM Patients WHERE patient_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) out.print("{\"success\":true,\"data\":" + patientToJson(rs) + "}");
            else { res.setStatus(404); out.print("{\"success\":false,\"message\":\"Not found\"}"); }
        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"" + e.getMessage().replace("\"","'") + "\"}");
        }
    }

    // STORED PROCEDURE: GetPatientHistory
    private void getHistory(int patientId, HttpServletResponse res, PrintWriter out) throws IOException {
        String sql = "{CALL GetPatientHistory(?)}";
        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, patientId);
            ResultSet rs = cs.executeQuery();
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                    .append("\"patient_name\":\"").append(safe(rs.getString("patient_name"))).append("\",")
                    .append("\"appointment_date\":\"").append(safe(rs.getString("appointment_date"))).append("\",")
                    .append("\"status\":\"").append(safe(rs.getString("status"))).append("\",")
                    .append("\"doctor_name\":\"").append(safe(rs.getString("doctor_name"))).append("\",")
                    .append("\"specialization\":\"").append(safe(rs.getString("specialization"))).append("\",")
                    .append("\"department_name\":\"").append(safe(rs.getString("department_name"))).append("\",")
                    .append("\"billed_amount\":\"").append(safe(rs.getString("billed_amount"))).append("\"")
                    .append("}");
                first = false;
            }
            json.append("]");
            out.print("{\"success\":true,\"data\":" + json + "}");
        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"" + e.getMessage().replace("\"","'") + "\"}");
        }
    }

    private String patientToJson(ResultSet rs) throws SQLException {
        return "{" +
            "\"patient_id\":"   + rs.getInt("patient_id")         + "," +
            "\"name\":\""       + safe(rs.getString("name"))       + "\"," +
            "\"dob\":\""        + safe(rs.getString("dob"))        + "\"," +
            "\"contact\":\""    + safe(rs.getString("contact"))    + "\"," +
            "\"address\":\""    + safe(rs.getString("address"))    + "\"" +
        "}";
    }

    private String safe(String s) { return s == null ? "" : s.replace("\"","'"); }
}
