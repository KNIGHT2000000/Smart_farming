package com.smartfarming.controller;

import com.smartfarming.model.Alert;
import com.smartfarming.service.AlertService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.List;

/**
 * AlertServlet
 *
 * GET  /api/alerts               → all alerts (latest 100)
 * GET  /api/alerts?unresolved=true → only unresolved alerts
 * GET  /api/alerts?limit=20        → limit results
 * POST /api/alerts/resolve?id=5    → resolve a specific alert
 *
 * All data comes from alert_view (DB view).
 * No business logic in this class.
 */
@WebServlet("/api/alerts")
public class AlertServlet extends HttpServlet {

    private AlertService alertService;

    @Override
    public void init() {
        this.alertService = new AlertService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        setCorsHeaders(resp);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();

        try {
            // Parse query parameters
            String unresolvedParam = req.getParameter("unresolved");
            String limitParam      = req.getParameter("limit");

            boolean onlyUnresolved = "true".equalsIgnoreCase(unresolvedParam);
            int     limit          = (limitParam != null) ? Integer.parseInt(limitParam) : 100;

            List<Alert> alerts = alertService.getAlerts(onlyUnresolved, limit);

            // Wrap in a response envelope
            String json = "{"
                + "\"status\":\"success\","
                + "\"count\":" + alerts.size() + ","
                + "\"alerts\":" + JsonUtil.alertListToJson(alerts)
                + "}";

            resp.setStatus(HttpServletResponse.SC_OK);
            out.print(json);

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(JsonUtil.error("Failed to fetch alerts: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    /** POST /api/alerts?action=resolve&id=5  → resolve an alert */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        setCorsHeaders(resp);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();

        try {
            String action  = req.getParameter("action");
            String idParam = req.getParameter("id");

            if ("resolve".equals(action) && idParam != null) {
                long alertId = Long.parseLong(idParam);
                alertService.resolveAlert(alertId);
                resp.setStatus(HttpServletResponse.SC_OK);
                out.print(JsonUtil.success("Alert #" + alertId + " marked as resolved"));
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(JsonUtil.error("Use ?action=resolve&id=<alertId>"));
            }

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(JsonUtil.error("Failed to resolve alert: " + e.getMessage()));
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }
}
