package com.smartfarming.model;

import java.sql.Timestamp;

/**
 * IrrigationHistory - POJO
 * Maps to irrigation_history_view in the database.
 */
public class IrrigationHistory {

    private long      eventId;
    private Timestamp triggeredAt;
    private int       durationMinutes;
    private double    waterAmountLiters;
    private String    irrigationStatus;  // triggered | completed | failed
    private String    notes;
    private int       plotId;
    private String    plotName;
    private String    cropName;
    private String    stageAtTrigger;
    private double    moistureAtTrigger;
    private int       triggeringSensorId;
    private String    triggeringSensorType;
    private long      minutesAgo;

    // Getters
    public long      getEventId()              { return eventId; }
    public Timestamp getTriggeredAt()          { return triggeredAt; }
    public int       getDurationMinutes()      { return durationMinutes; }
    public double    getWaterAmountLiters()    { return waterAmountLiters; }
    public String    getIrrigationStatus()     { return irrigationStatus; }
    public String    getNotes()                { return notes; }
    public int       getPlotId()               { return plotId; }
    public String    getPlotName()             { return plotName; }
    public String    getCropName()             { return cropName; }
    public String    getStageAtTrigger()       { return stageAtTrigger; }
    public double    getMoistureAtTrigger()    { return moistureAtTrigger; }
    public int       getTriggeringSensorId()   { return triggeringSensorId; }
    public String    getTriggeringSensorType() { return triggeringSensorType; }
    public long      getMinutesAgo()           { return minutesAgo; }

    // Setters
    public void setEventId(long v)              { eventId = v; }
    public void setTriggeredAt(Timestamp v)     { triggeredAt = v; }
    public void setDurationMinutes(int v)       { durationMinutes = v; }
    public void setWaterAmountLiters(double v)  { waterAmountLiters = v; }
    public void setIrrigationStatus(String v)   { irrigationStatus = v; }
    public void setNotes(String v)              { notes = v; }
    public void setPlotId(int v)                { plotId = v; }
    public void setPlotName(String v)           { plotName = v; }
    public void setCropName(String v)           { cropName = v; }
    public void setStageAtTrigger(String v)     { stageAtTrigger = v; }
    public void setMoistureAtTrigger(double v)  { moistureAtTrigger = v; }
    public void setTriggeringSensorId(int v)    { triggeringSensorId = v; }
    public void setTriggeringSensorType(String v){ triggeringSensorType = v; }
    public void setMinutesAgo(long v)           { minutesAgo = v; }
}
