package com.hospital.servlet;

import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

/**
 * ReportServlet.java
 * GET /api/reports?type=doctor_summary      → VIEW: DoctorAppointmentSummary
 * GET /api/reports?type=dept_patients       → VIEW: DepartmentPatientList
 * GET /api/reports?type=daily_billing       → VIEW: DailyBillingReport
 * GET /api/reports?type=frequent_patients   → NESTED: patients > 2 visits/month
 * GET /api/reports?type=dept_most_appts     → NESTED: dept with most appointments
 * GET /api/reports?type=highest_bill        → NESTED: highest bill per patient
 * GET /api/reports?type=billing_by_dept     → JOIN: billing by department
 * GET /api/reports?type=daily_revenue       → JOIN: daily appointments + revenue
 * Project ID: 25FE5A4305
 */
public class ReportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        String type = req.getParameter("type");
        if (type == null) { res.setStatus(400); out.print("{\"success\":false,\"message\":\"type param required\"}"); return; }

        String sql = getSql(type);
        if (sql == null) { res.setStatus(400); out.print("{\"success\":false,\"message\":\"Unknown report type\"}"); return; }

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            StringBuilder json = new StringBuilder("[");
            boolean firstRow = true;

            while (rs.next()) {
                if (!firstRow) json.append(",");
                json.append("{");
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) json.append(",");
                    String colName = meta.getColumnLabel(i);
                    String val     = rs.getString(i);
                    json.append("\"").append(colName).append("\":");
                    // Numeric columns — no quotes
                    if (isNumeric(meta.getColumnType(i))) {
                        json.append(val == null ? "null" : val);
                    } else {
                        json.append("\"").append(val == null ? "" : val.replace("\"","'")).append("\"");
                    }
                }
                json.append("}");
                firstRow = false;
            }
            json.append("]");
            out.print("{\"success\":true,\"type\":\"" + type + "\",\"data\":" + json + "}");

        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"success\":false,\"message\":\"" + e.getMessage().replace("\"","'") + "\"}");
        }
    }

    private String getSql(String type) {
        if (type.equals("doctor_summary")) {
            return "SELECT * FROM DoctorAppointmentSummary";

        } else if (type.equals("dept_patients")) {
            return "SELECT * FROM DepartmentPatientList LIMIT 50";

        } else if (type.equals("daily_billing")) {
            return "SELECT * FROM DailyBillingReport";

        } else if (type.equals("frequent_patients")) {
            return "SELECT p.patient_id, p.name, COUNT(a.appointment_id) AS visit_count, " +
                   "MONTH(a.appointment_date) AS visit_month, YEAR(a.appointment_date) AS visit_year " +
                   "FROM Patients p JOIN Appointments a ON p.patient_id = a.patient_id " +
                   "WHERE a.patient_id IN (" +
                   "  SELECT patient_id FROM Appointments " +
                   "  WHERE MONTH(appointment_date) = MONTH(CURDATE()) " +
                   "  AND YEAR(appointment_date) = YEAR(CURDATE()) " +
                   "  GROUP BY patient_id HAVING COUNT(*) > 2) " +
                   "GROUP BY p.patient_id, p.name, MONTH(a.appointment_date), YEAR(a.appointment_date)";

        } else if (type.equals("dept_most_appts")) {
            return "SELECT dep.department_name, COUNT(a.appointment_id) AS appointment_count " +
                   "FROM Departments dep JOIN Doctors d ON dep.department_id = d.department_id " +
                   "JOIN Appointments a ON d.doctor_id = a.doctor_id " +
                   "GROUP BY dep.department_id, dep.department_name " +
                   "HAVING COUNT(a.appointment_id) = (" +
                   "  SELECT MAX(dc) FROM (" +
                   "    SELECT COUNT(a2.appointment_id) AS dc FROM Doctors d2 " +
                   "    JOIN Appointments a2 ON d2.doctor_id = a2.doctor_id " +
                   "    GROUP BY d2.department_id) sub)";

        } else if (type.equals("highest_bill")) {
            return "SELECT p.patient_id, p.name, b.max_bill FROM Patients p " +
                   "JOIN (SELECT patient_id, MAX(amount) AS max_bill FROM Billing " +
                   "GROUP BY patient_id) b ON p.patient_id = b.patient_id " +
                   "ORDER BY b.max_bill DESC";

        } else if (type.equals("billing_by_dept")) {
            return "SELECT p.name AS patient_name, dep.department_name, " +
                   "SUM(b.amount) AS total_billed, COUNT(b.bill_id) AS bill_count " +
                   "FROM Patients p JOIN Billing b ON p.patient_id = b.patient_id " +
                   "JOIN Appointments a ON p.patient_id = a.patient_id " +
                   "AND b.billing_date = a.appointment_date " +
                   "JOIN Doctors d ON a.doctor_id = d.doctor_id " +
                   "JOIN Departments dep ON d.department_id = dep.department_id " +
                   "GROUP BY p.patient_id, p.name, dep.department_id, dep.department_name " +
                   "ORDER BY total_billed DESC";

        } else if (type.equals("daily_revenue")) {
            return "SELECT a.appointment_date, " +
                   "COUNT(DISTINCT a.appointment_id) AS total_appointments, " +
                   "SUM(CASE WHEN a.status='Completed' THEN 1 ELSE 0 END) AS completed, " +
                   "SUM(CASE WHEN a.status='Scheduled' THEN 1 ELSE 0 END) AS scheduled, " +
                   "SUM(CASE WHEN a.status='Cancelled' THEN 1 ELSE 0 END) AS cancelled, " +
                   "COALESCE(SUM(b.amount), 0) AS daily_revenue " +
                   "FROM Appointments a " +
                   "LEFT JOIN Billing b ON a.patient_id = b.patient_id " +
                   "AND b.billing_date = a.appointment_date " +
                   "GROUP BY a.appointment_date ORDER BY a.appointment_date DESC";

        } else {
            return null;
        }
    }

    private boolean isNumeric(int sqlType) {
        return sqlType == Types.INTEGER || sqlType == Types.BIGINT ||
               sqlType == Types.DOUBLE  || sqlType == Types.FLOAT  ||
               sqlType == Types.DECIMAL || sqlType == Types.NUMERIC||
               sqlType == Types.SMALLINT|| sqlType == Types.TINYINT;
    }
}
