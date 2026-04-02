package com.smartfarming.controller;

import com.smartfarming.model.Alert;
import com.smartfarming.model.IrrigationHistory;
import com.smartfarming.model.PlotStatus;

import java.util.List;

/**
 * JsonUtil
 *
 * Manual JSON builder — zero external libraries.
 * Keeps project self-contained for academic/viva purposes.
 */
public class JsonUtil {

    // ── Primitive helpers ─────────────────────────────────────

    private static String esc(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r") + "\"";
    }

    private static String kv(String key, String jsonValue) {
        return "\"" + key + "\":" + jsonValue;
    }

    private static String kvStr(String key, String value) {
        return kv(key, esc(value));
    }

    private static String kvNum(String key, Number value) {
        return kv(key, value == null ? "null" : value.toString());
    }

    private static String kvBool(String key, boolean value) {
        return kv(key, value ? "true" : "false");
    }

    // ── Response envelopes ────────────────────────────────────

    /** Success with a plain string message (used for POST confirmations) */
    public static String success(String message) {
        return "{\"status\":\"success\",\"message\":" + esc(message) + "}";
    }

    /** Success with an embedded JSON data block */
    public static String successWithData(String message, String dataJson) {
        return "{\"status\":\"success\",\"message\":" + esc(message) + ",\"data\":" + dataJson + "}";
    }

    /** Error with a string message (used by all servlets) */
    public static String error(String message) {
        return "{\"status\":\"error\",\"message\":" + esc(message) + "}";
    }

    // ── Alert serialization ───────────────────────────────────

    public static String alertToJson(Alert a) {
        StringBuilder sb = new StringBuilder("{");
        sb.append(kvNum("alertId",           a.getAlertId())).append(",");
        sb.append(kvStr("alertType",         a.getAlertType())).append(",");
        sb.append(kvStr("message",           a.getMessage())).append(",");
        sb.append(kvStr("severity",          a.getSeverity())).append(",");
        sb.append(kvBool("resolved",         a.isResolved())).append(",");
        sb.append(kvStr("createdAt",         a.getCreatedAt()  == null ? null : a.getCreatedAt().toString())).append(",");
        sb.append(kvStr("resolvedAt",        a.getResolvedAt() == null ? null : a.getResolvedAt().toString())).append(",");
        sb.append(kvNum("plotId",            a.getPlotId())).append(",");
        sb.append(kvStr("plotName",          a.getPlotName())).append(",");
        sb.append(kvNum("sensorId",          a.getSensorId())).append(",");
        sb.append(kvStr("sensorType",        a.getSensorType())).append(",");
        sb.append(kvStr("sensorLocation",    a.getSensorLocation())).append(",");
        sb.append(kvNum("sensorTrustScore",  a.getSensorTrustScore())).append(",");
        sb.append(kvStr("cropName",          a.getCropName())).append(",");
        sb.append(kvStr("cropStageAtAlert",  a.getCropStageAtAlert()));
        sb.append("}");
        return sb.toString();
    }

    public static String alertListToJson(List<Alert> alerts) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < alerts.size(); i++) {
            sb.append(alertToJson(alerts.get(i)));
            if (i < alerts.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    // ── PlotStatus serialization ──────────────────────────────

    public static String plotStatusToJson(PlotStatus p) {
        StringBuilder sb = new StringBuilder("{");
        sb.append(kvNum("plotId",               p.getPlotId())).append(",");
        sb.append(kvStr("plotName",             p.getPlotName())).append(",");
        sb.append(kvNum("areaSqm",              p.getAreaSqm())).append(",");
        sb.append(kvStr("plotStatus",           p.getPlotStatus())).append(",");
        sb.append(kvStr("plantingDate",         p.getPlantingDate()    == null ? null : p.getPlantingDate().toString())).append(",");
        sb.append(kvNum("daysSincePlanting",    p.getDaysSincePlanting())).append(",");
        sb.append(kvStr("cropName",             p.getCropName())).append(",");
        sb.append(kvNum("baseDurationDays",     p.getBaseDurationDays())).append(",");
        sb.append(kvStr("currentStage",         p.getCurrentStage())).append(",");
        sb.append(kvNum("stageOrder",           p.getStageOrder())).append(",");
        sb.append(kvNum("startDay",             p.getStartDay())).append(",");
        sb.append(kvNum("endDay",               p.getEndDay())).append(",");
        sb.append(kvNum("minMoisture",          p.getMinMoisture())).append(",");
        sb.append(kvNum("maxMoisture",          p.getMaxMoisture())).append(",");
        sb.append(kvNum("irrigationThreshold",  p.getIrrigationThreshold())).append(",");
        sb.append(kvNum("minTemperature",       p.getMinTemperature())).append(",");
        sb.append(kvNum("maxTemperature",       p.getMaxTemperature())).append(",");
        sb.append(kvStr("latestMoisture",       p.getLatestMoisture())).append(",");
        sb.append(kvStr("latestTemperature",    p.getLatestTemperature())).append(",");
        sb.append(kvStr("lastReadingAt",        p.getLastReadingAt()   == null ? null : p.getLastReadingAt().toString())).append(",");
        sb.append(kvNum("activeSensorCount",    p.getActiveSensorCount())).append(",");
        sb.append(kvNum("minSensorTrustScore",  p.getMinSensorTrustScore())).append(",");
        sb.append(kvNum("unresolvedAlertCount", p.getUnresolvedAlertCount())).append(",");
        sb.append(kvBool("irrigationActive",    p.isIrrigationActive()));
        sb.append("}");
        return sb.toString();
    }

    /** Alias expected by PlotStatusServlet */
    public static String plotListToJson(List<PlotStatus> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(plotStatusToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    // ── IrrigationHistory serialization ──────────────────────

    public static String irrigationToJson(IrrigationHistory h) {
        StringBuilder sb = new StringBuilder("{");
        sb.append(kvNum("eventId",              h.getEventId())).append(",");
        sb.append(kvStr("triggeredAt",          h.getTriggeredAt()  == null ? null : h.getTriggeredAt().toString())).append(",");
        sb.append(kvNum("durationMinutes",      h.getDurationMinutes())).append(",");
        sb.append(kvNum("waterAmountLiters",    h.getWaterAmountLiters())).append(",");
        sb.append(kvStr("irrigationStatus",     h.getIrrigationStatus())).append(",");
        sb.append(kvStr("notes",                h.getNotes())).append(",");
        sb.append(kvNum("plotId",               h.getPlotId())).append(",");
        sb.append(kvStr("plotName",             h.getPlotName())).append(",");
        sb.append(kvStr("cropName",             h.getCropName())).append(",");
        sb.append(kvStr("stageAtTrigger",       h.getStageAtTrigger())).append(",");
        sb.append(kvNum("moistureAtTrigger",    h.getMoistureAtTrigger())).append(",");
        sb.append(kvNum("triggeringSensorId",   h.getTriggeringSensorId())).append(",");
        sb.append(kvStr("triggeringSensorType", h.getTriggeringSensorType())).append(",");
        sb.append(kvNum("minutesAgo",           h.getMinutesAgo()));
        sb.append("}");
        return sb.toString();
    }

    public static String irrigationListToJson(List<IrrigationHistory> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(irrigationToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
