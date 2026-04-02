package com.smartfarming.model;

import java.sql.Timestamp;

/**
 * Alert - Plain Old Java Object (POJO)
 * Maps to alert_view in the database.
 * Encapsulates all fields as private with getters.
 */
public class Alert {

    private long      alertId;
    private String    alertType;
    private String    message;
    private String    severity;       // INFO | WARNING | CRITICAL
    private boolean   resolved;
    private Timestamp createdAt;
    private Timestamp resolvedAt;
    private int       plotId;
    private String    plotName;
    private Integer   sensorId;       // nullable
    private String    sensorType;
    private String    sensorLocation;
    private Double    sensorTrustScore;
    private String    cropName;
    private String    cropStageAtAlert;

    // ── Default constructor ───────────────────────────────────
    public Alert() {}

    // ── Full constructor ──────────────────────────────────────
    public Alert(long alertId, String alertType, String message, String severity,
                 boolean resolved, Timestamp createdAt, int plotId, String plotName) {
        this.alertId   = alertId;
        this.alertType = alertType;
        this.message   = message;
        this.severity  = severity;
        this.resolved  = resolved;
        this.createdAt = createdAt;
        this.plotId    = plotId;
        this.plotName  = plotName;
    }

    // ── Getters ───────────────────────────────────────────────
    public long      getAlertId()           { return alertId; }
    public String    getAlertType()         { return alertType; }
    public String    getMessage()           { return message; }
    public String    getSeverity()          { return severity; }
    public boolean   isResolved()           { return resolved; }
    public Timestamp getCreatedAt()         { return createdAt; }
    public Timestamp getResolvedAt()        { return resolvedAt; }
    public int       getPlotId()            { return plotId; }
    public String    getPlotName()          { return plotName; }
    public Integer   getSensorId()          { return sensorId; }
    public String    getSensorType()        { return sensorType; }
    public String    getSensorLocation()    { return sensorLocation; }
    public Double    getSensorTrustScore()  { return sensorTrustScore; }
    public String    getCropName()          { return cropName; }
    public String    getCropStageAtAlert()  { return cropStageAtAlert; }

    // ── Setters ───────────────────────────────────────────────
    public void setAlertId(long id)              { this.alertId = id; }
    public void setAlertType(String t)           { this.alertType = t; }
    public void setMessage(String m)             { this.message = m; }
    public void setSeverity(String s)            { this.severity = s; }
    public void setResolved(boolean r)           { this.resolved = r; }
    public void setCreatedAt(Timestamp t)        { this.createdAt = t; }
    public void setResolvedAt(Timestamp t)       { this.resolvedAt = t; }
    public void setPlotId(int id)                { this.plotId = id; }
    public void setPlotName(String n)            { this.plotName = n; }
    public void setSensorId(Integer id)          { this.sensorId = id; }
    public void setSensorType(String t)          { this.sensorType = t; }
    public void setSensorLocation(String l)      { this.sensorLocation = l; }
    public void setSensorTrustScore(Double s)    { this.sensorTrustScore = s; }
    public void setCropName(String n)            { this.cropName = n; }
    public void setCropStageAtAlert(String s)    { this.cropStageAtAlert = s; }

    @Override
    public String toString() {
        return "Alert{id=" + alertId + ", type=" + alertType +
               ", severity=" + severity + ", plot=" + plotName + "}";
    }
}
