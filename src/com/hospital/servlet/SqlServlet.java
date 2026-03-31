package com.hospital.servlet;

import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * SqlServlet.java
 * POST /api/sql  { query: "SELECT * FROM Patients" }
 * Executes SELECT queries against the live MySQL database.
 * Returns results as JSON for the SQL Console in the frontend.
 * Project ID: 25FE5A4305
 */
public class SqlServlet extends HttpServlet {

    // Only allow SELECT and CALL — block destructive queries
    private static final List<String> BLOCKED = Arrays.asList("DROP","TRUNCATE","ALTER","CREATE","GRANT","REVOKE");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();

        String query = req.getParameter("query");
        if (query == null || query.trim().isEmpty()) {
            res.setStatus(400);
            out.print("{\"success\":false,\"message\":\"No query provided\"}");
            return;
        }

        String upper = query.trim().toUpperCase();

        // Safety check
        for (String blocked : BLOCKED) {
            if (upper.startsWith(blocked)) {
                res.setStatus(403);
                out.print("{\"success\":false,\"message\":\"" + blocked + " statements are not allowed in the console.\"}");
                return;
            }
        }

        // Only allow SELECT and CALL
        if (!upper.startsWith("SELECT") && !upper.startsWith("CALL")) {
            res.setStatus(403);
            out.print("{\"success\":false,\"message\":\"Only SELECT and CALL statements are allowed.\"}");
            return;
        }

        long start = System.currentTimeMillis();

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {

            boolean hasResultSet = st.execute(query);

            if (hasResultSet) {
                ResultSet rs = st.getResultSet();
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();

                // Build columns array
                StringBuilder colJson = new StringBuilder("[");
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) colJson.append(",");
                    colJson.append("\"").append(meta.getColumnLabel(i)).append("\"");
                }
                colJson.append("]");

                // Build rows array
                StringBuilder rowJson = new StringBuilder("[");
                boolean firstRow = true;
                int rowCount = 0;

                while (rs.next()) {
                    if (!firstRow) rowJson.append(",");
                    rowJson.append("{");
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1) rowJson.append(",");
                        String colName = meta.getColumnLabel(i);
                        String val = rs.getString(i);
                        rowJson.append("\"").append(colName).append("\":");
                        if (val == null) {
                            rowJson.append("null");
                        } else {
                            rowJson.append("\"").append(val.replace("\\","\\\\").replace("\"","'")).append("\"");
                        }
                    }
                    rowJson.append("}");
                    firstRow = false;
                    rowCount++;
                }
                rowJson.append("]");

                long ms = System.currentTimeMillis() - start;
                out.print("{\"success\":true," +
                          "\"rows\":" + rowCount + "," +
                          "\"ms\":" + ms + "," +
                          "\"columns\":" + colJson + "," +
                          "\"data\":" + rowJson + "}");

            } else {
                // UPDATE/INSERT result (if somehow passed)
                int updateCount = st.getUpdateCount();
                long ms = System.currentTimeMillis() - start;
                out.print("{\"success\":true,\"rows\":" + updateCount + ",\"ms\":" + ms +
                          ",\"columns\":[],\"data\":[]}");
            }

        } catch (SQLException e) {
            long ms = System.currentTimeMillis() - start;
            res.setStatus(400);
            out.print("{\"success\":false," +
                      "\"ms\":" + ms + "," +
                      "\"message\":\"" + e.getMessage().replace("\"","'").replace("\n"," ") + "\"," +
                      "\"error_code\":" + e.getErrorCode() + "}");
        }
    }
}
