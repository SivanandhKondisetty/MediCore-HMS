package com.hospital.servlet;

import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

/**
 * AppointmentServlet.java
 * GET    /api/appointments               → all appointments (JOIN)
 * GET    /api/appointments?doctor_id=1   → by doctor
 * POST   /api/appointments               → schedule (calls STORED PROCEDURE)
 * PUT    /api/appointments?id=1          → update status (triggers AutoGenerateBilling)
 * DELETE /api/appointments?id=1          → delete
 * Project ID: 25FE5A4305
 */
public class AppointmentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        String docId = req.getParameter("doctor_id");

        String sql =
            "SELECT a.appointment_id, a.patient_id, a.doctor_id, a.appointment_date, a.status, " +
            "       p.name AS patient_name, d.name AS doctor_name, d.specialization, dep.department_name " +
            "FROM Appointments a " +
            "JOIN Patients    p   ON a.patient_id    = p.patient_id " +
            "JOIN Doctors     d   ON a.doctor_id     = d.doctor_id " +
            "JOIN Departments dep ON d.department_id = dep.department_id " +
            (docId != null ? "WHERE a.doctor_id = " + docId + " " : "") +
            "ORDER BY a.appointment_date DESC";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                    .append("\"appointment_id\":").append(rs.getInt("appointment_id")).append(",")
                    .append("\"patient_id\":").append(rs.getInt("patient_id")).append(",")
                    .append("\"doctor_id\":").append(rs.getInt("doctor_id")).append(",")
                    .append("\"appointment_date\":\"").append(s(rs.getString("appointment_date"))).append("\",")
                    .append("\"status\":\"").append(s(rs.getString("status"))).append("\",")
                    .append("\"patient_name\":\"").append(s(rs.getString("patient_name"))).append("\",")
                    .append("\"doctor_name\":\"").append(s(rs.getString("doctor_name"))).append("\",")
                    .append("\"specialization\":\"").append(s(rs.getString("specialization"))).append("\",")
                    .append("\"department_name\":\"").append(s(rs.getString("department_name"))).append("\"")
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

    // POST — Schedule via STORED PROCEDURE ScheduleAppointment
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();

        String patId  = req.getParameter("patient_id");
        String docId  = req.getParameter("doctor_id");
        String date   = req.getParameter("appointment_date");

        if (patId == null || docId == null || date == null) {
            res.setStatus(400);
            out.print("{\"success\":false,\"message\":\"patient_id, doctor_id, appointment_date required\"}");
            return;
        }

        String sql = "{CALL ScheduleAppointment(?, ?, ?)}";
        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setInt(1, Integer.parseInt(patId));
            cs.setInt(2, Integer.parseInt(docId));
            cs.setString(3, date);

            String notif = "";
            boolean hasResult = cs.execute();
            if (hasResult) {
                ResultSet rs = cs.getResultSet();
                if (rs.next()) notif = s(rs.getString("notification_message"));
            }

            out.print("{\"success\":true,\"message\":\"Appointment scheduled via stored procedure\"," +
                      "\"notification\":\"" + notif + "\"}");

        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"" + s(e.getMessage()) + "\"}");
        }
    }

    // PUT — Update status. If Completed → AutoGenerateBilling TRIGGER fires automatically
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        String idStr  = req.getParameter("id");
        String status = req.getParameter("status");

        if (idStr == null || status == null) {
            res.setStatus(400);
            out.print("{\"success\":false,\"message\":\"id and status required\"}");
            return;
        }

        String sql = "UPDATE Appointments SET status=? WHERE appointment_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, Integer.parseInt(idStr));
            int rows = ps.executeUpdate();

            String triggerMsg = status.equals("Completed")
                ? " AutoGenerateBilling TRIGGER fired — bill inserted." : "";

            out.print("{\"success\":" + (rows>0) + "," +
                      "\"message\":\"Status updated to " + status + "." + triggerMsg + "\"," +
                      "\"trigger_fired\":" + status.equals("Completed") + "}");

        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"" + s(e.getMessage()) + "\"}");
        }
    }

    // DELETE
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        String idStr = req.getParameter("id");
        if (idStr == null) { res.setStatus(400); out.print("{\"success\":false,\"message\":\"ID required\"}"); return; }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Appointments WHERE appointment_id=?")) {
            ps.setInt(1, Integer.parseInt(idStr));
            int rows = ps.executeUpdate();
            out.print("{\"success\":" + (rows>0) + ",\"message\":\"" + (rows>0?"Deleted":"Not found") + "\"}");
        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"" + s(e.getMessage()) + "\"}");
        }
    }

    private String s(String v) { return v == null ? "" : v.replace("\"","'"); }
}
