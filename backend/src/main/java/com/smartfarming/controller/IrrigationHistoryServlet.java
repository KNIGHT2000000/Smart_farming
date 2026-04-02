package com.smartfarming.controller;

import com.smartfarming.model.IrrigationHistory;
import com.smartfarming.service.IrrigationService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.List;

/**
 * IrrigationHistoryServlet
 *
 * GET  /api/irrigation-history             → last 50 events (all plots)
 * GET  /api/irrigation-history?plotId=1    → events for a specific plot
 * GET  /api/irrigation-history?limit=10    → limit results
 * POST /api/irrigation-history?action=complete&id=3 → mark completed
 *
 * Reads from irrigation_history_view.
 */
@WebServlet("/api/irrigation-history")
public class IrrigationHistoryServlet extends HttpServlet {

    private IrrigationService irrigationService;

    @Override
    public void init() {
        this.irrigationService = new IrrigationService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        setCorsHeaders(resp);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String plotIdParam = req.getParameter("plotId");
            String limitParam  = req.getParameter("limit");

            int plotId = (plotIdParam != null) ? Integer.parseInt(plotIdParam) : 0;
            int limit  = (limitParam  != null) ? Integer.parseInt(limitParam)  : 50;

            List<IrrigationHistory> history =
                irrigationService.getHistory(plotId, limit);

            String json = "{"
                + "\"status\":\"success\","
                + "\"count\":"  + history.size() + ","
                + "\"history\":" + JsonUtil.irrigationListToJson(history)
                + "}";

            resp.setStatus(HttpServletResponse.SC_OK);
            out.print(json);

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(JsonUtil.error("Failed to fetch irrigation history: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    /** POST /api/irrigation-history?action=complete&id=3 */
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

            if ("complete".equals(action) && idParam != null) {
                long eventId = Long.parseLong(idParam);
                irrigationService.completeIrrigation(eventId);
                resp.setStatus(HttpServletResponse.SC_OK);
                out.print(JsonUtil.success(
                    "Irrigation event #" + eventId + " marked as completed"));
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(JsonUtil.error("Use ?action=complete&id=<eventId>"));
            }

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(JsonUtil.error("Error: " + e.getMessage()));
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
