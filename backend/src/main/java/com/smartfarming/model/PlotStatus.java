package com.smartfarming.model;

import java.sql.Timestamp;
import java.sql.Date;

/**
 * PlotStatus - POJO
 * Maps to plot_status_view in the database.
 */
public class PlotStatus {

    private int       plotId;
    private String    plotName;
    private double    areaSqm;
    private String    plotStatus;
    private Date      plantingDate;
    private int       daysSincePlanting;
    private String    cropName;
    private int       baseDurationDays;
    private String    currentStage;
    private int       stageOrder;
    private int       startDay;
    private int       endDay;
    private double    minMoisture;
    private double    maxMoisture;
    private double    irrigationThreshold;
    private double    minTemperature;
    private double    maxTemperature;
    private String    latestMoisture;
    private String    latestTemperature;
    private Timestamp lastReadingAt;
    private int       activeSensorCount;
    private Double    minSensorTrustScore;
    private int       unresolvedAlertCount;
    private boolean   irrigationActive;

    // All getters and setters
    public int       getPlotId()               { return plotId; }
    public String    getPlotName()             { return plotName; }
    public double    getAreaSqm()              { return areaSqm; }
    public String    getPlotStatus()           { return plotStatus; }
    public Date      getPlantingDate()         { return plantingDate; }
    public int       getDaysSincePlanting()    { return daysSincePlanting; }
    public String    getCropName()             { return cropName; }
    public int       getBaseDurationDays()     { return baseDurationDays; }
    public String    getCurrentStage()         { return currentStage; }
    public int       getStageOrder()           { return stageOrder; }
    public int       getStartDay()             { return startDay; }
    public int       getEndDay()               { return endDay; }
    public double    getMinMoisture()          { return minMoisture; }
    public double    getMaxMoisture()          { return maxMoisture; }
    public double    getIrrigationThreshold()  { return irrigationThreshold; }
    public double    getMinTemperature()       { return minTemperature; }
    public double    getMaxTemperature()       { return maxTemperature; }
    public String    getLatestMoisture()       { return latestMoisture; }
    public String    getLatestTemperature()    { return latestTemperature; }
    public Timestamp getLastReadingAt()        { return lastReadingAt; }
    public int       getActiveSensorCount()    { return activeSensorCount; }
    public Double    getMinSensorTrustScore()  { return minSensorTrustScore; }
    public int       getUnresolvedAlertCount() { return unresolvedAlertCount; }
    public boolean   isIrrigationActive()      { return irrigationActive; }

    public void setPlotId(int v)               { plotId = v; }
    public void setPlotName(String v)          { plotName = v; }
    public void setAreaSqm(double v)           { areaSqm = v; }
    public void setPlotStatus(String v)        { plotStatus = v; }
    public void setPlantingDate(Date v)        { plantingDate = v; }
    public void setDaysSincePlanting(int v)    { daysSincePlanting = v; }
    public void setCropName(String v)          { cropName = v; }
    public void setBaseDurationDays(int v)     { baseDurationDays = v; }
    public void setCurrentStage(String v)      { currentStage = v; }
    public void setStageOrder(int v)           { stageOrder = v; }
    public void setStartDay(int v)             { startDay = v; }
    public void setEndDay(int v)               { endDay = v; }
    public void setMinMoisture(double v)       { minMoisture = v; }
    public void setMaxMoisture(double v)       { maxMoisture = v; }
    public void setIrrigationThreshold(double v){ irrigationThreshold = v; }
    public void setMinTemperature(double v)    { minTemperature = v; }
    public void setMaxTemperature(double v)    { maxTemperature = v; }
    public void setLatestMoisture(String v)    { latestMoisture = v; }
    public void setLatestTemperature(String v) { latestTemperature = v; }
    public void setLastReadingAt(Timestamp v)  { lastReadingAt = v; }
    public void setActiveSensorCount(int v)    { activeSensorCount = v; }
    public void setMinSensorTrustScore(Double v){ minSensorTrustScore = v; }
    public void setUnresolvedAlertCount(int v) { unresolvedAlertCount = v; }
    public void setIrrigationActive(boolean v) { irrigationActive = v; }
}
