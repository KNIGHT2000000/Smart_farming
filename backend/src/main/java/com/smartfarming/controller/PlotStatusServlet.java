package com.smartfarming.controller;

import com.smartfarming.model.PlotStatus;
import com.smartfarming.service.PlotService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.List;

/**
 * PlotStatusServlet
 *
 * GET /api/plot-status        → all plots
 * GET /api/plot-status?id=1   → single plot by ID
 *
 * Reads from plot_status_view.
 */
@WebServlet("/api/plot-status")
public class PlotStatusServlet extends HttpServlet {

    private PlotService plotService;

    @Override
    public void init() {
        this.plotService = new PlotService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        setCorsHeaders(resp);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String idParam = req.getParameter("id");

            if (idParam != null) {
                // Single plot
                int plotId = Integer.parseInt(idParam);
                PlotStatus plot = plotService.getPlotById(plotId);
                if (plot == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(JsonUtil.error("Plot not found: id=" + plotId));
                } else {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    out.print(JsonUtil.successWithData(
                        "Plot found", JsonUtil.plotStatusToJson(plot)));
                }
            } else {
                // All plots
                List<PlotStatus> plots = plotService.getAllPlots();
                String json = "{"
                    + "\"status\":\"success\","
                    + "\"count\":" + plots.size() + ","
                    + "\"plots\":" + JsonUtil.plotListToJson(plots)
                    + "}";
                resp.setStatus(HttpServletResponse.SC_OK);
                out.print(json);
            }

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(JsonUtil.error("Failed to fetch plot status: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }
}
