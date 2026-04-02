package com.smartfarming.service;

import com.smartfarming.dao.SensorDAO;

import java.sql.SQLException;

/**
 * SensorService
 *
 * Thin service layer — delegates to DAO.
 * NO business logic here (all logic is in the stored procedure).
 *
 * Layered Architecture purpose:
 *   - Decouples Servlet (controller) from DAO
 *   - Makes unit testing easier
 *   - Entry point for future transaction orchestration
 */
public class SensorService {

    private final SensorDAO sensorDAO;

    public SensorService() {
        this.sensorDAO = new SensorDAO();
    }

    /**
     * Delegates to DAO → calls stored procedure in DB.
     * All logic (validation, crop stage, irrigation, alerts) is in the DB.
     */
    public String recordReading(int sensorId, double value)
            throws SQLException, ClassNotFoundException {
        return sensorDAO.recordSensorReading(sensorId, value);
    }
}
