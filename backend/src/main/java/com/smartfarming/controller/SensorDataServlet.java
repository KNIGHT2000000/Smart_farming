package com.smartfarming.controller;

import com.smartfarming.service.SensorService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;

/**
 * SensorDataServlet
 *
 * Endpoint: POST /api/sensor-data
 *
 * Accepts JSON body:
 * {
 *   "sensorId": 1,
 *   "value": 42.5
 * }
 *
 * Calls SensorService → SensorDAO → stored procedure record_sensor_reading().
 * All business logic (crop stage detection, irrigation trigger, alert generation)
 * is executed INSIDE the database. Java is only the transport layer.
 *
 * Response:
 *   200 OK     → { "status": "success", "message": "OK: reading_id=..." }
 *   400        → { "status": "error",   "message": "Missing sensorId/value" }
 *   500        → { "status": "error",   "message": "DB error: ..." }
 */
@WebServlet("/api/sensor-data")
public class SensorDataServlet extends HttpServlet {

    private SensorService sensorService;

    @Override
    public void init() {
        this.sensorService = new SensorService();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        // ── CORS headers (allow Python simulator / ESP32 to POST) ──
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");

        PrintWriter out = resp.getWriter();

        try {
            // ── Read raw JSON body ─────────────────────────────
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = req.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String body = sb.toString().trim();

            if (body.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(JsonUtil.error("Request body is empty"));
                return;
            }

            // ── Parse sensorId and value from JSON manually ────
            // Format: {"sensorId":1,"value":42.5}
            int    sensorId = extractInt(body, "sensorId");
            double value    = extractDouble(body, "value");

            if (sensorId <= 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(JsonUtil.error("Missing or invalid 'sensorId' in request body"));
                return;
            }

            // ── Delegate to service → DAO → stored procedure ──
            String result = sensorService.recordReading(sensorId, value);

            if (result != null && result.startsWith("ERROR")) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print(JsonUtil.error(result));
            } else {
                resp.setStatus(HttpServletResponse.SC_OK);
                out.print(JsonUtil.success(result));
            }

        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(JsonUtil.error("Invalid number format: " + e.getMessage()));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(JsonUtil.error("Server error: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    /** Handles CORS preflight OPTIONS request */
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    // ── Simple JSON field extractors (no library needed) ──────

    /**
     * Extracts an integer value for a given key from a flat JSON string.
     * e.g. extractInt({"sensorId":3,"value":42.1}, "sensorId") → 3
     */
    private int extractInt(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return -1;
        int colon = json.indexOf(':', idx + pattern.length());
        int start = colon + 1;
        // skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        return Integer.parseInt(json.substring(start, end).trim());
    }

    /**
     * Extracts a double value for a given key from a flat JSON string.
     */
    private double extractDouble(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return 0.0;
        int colon = json.indexOf(':', idx + pattern.length());
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length()
               && (Character.isDigit(json.charAt(end))
                   || json.charAt(end) == '.'
                   || json.charAt(end) == '-')) {
            end++;
        }
        return Double.parseDouble(json.substring(start, end).trim());
    }
}
