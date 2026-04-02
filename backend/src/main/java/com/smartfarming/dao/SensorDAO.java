package com.smartfarming.dao;

import com.smartfarming.db.DBConnection;
import com.smartfarming.model.Sensor;
import com.smartfarming.model.MoistureSensor;
import com.smartfarming.model.TemperatureSensor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SensorDAO
 *
 * Data Access Object for sensor and sensor_reading tables.
 * IMPORTANT: Business logic stays in the DB (stored procedure).
 * This DAO only calls the procedure and maps results.
 */
public class SensorDAO {

    /**
     * recordSensorReading
     *
     * Calls the stored procedure record_sensor_reading(sensor_id, value).
     * The procedure handles all business logic:
     *   - Validates reading
     *   - Determines crop stage
     *   - Triggers irrigation
     *   - Creates alerts
     *
     * @param sensorId the sensor sending the reading
     * @param value    the sensor measurement
     * @return result message from the stored procedure
     */
    public String recordSensorReading(int sensorId, double value)
            throws SQLException, ClassNotFoundException {

        Connection conn = DBConnection.getInstance().getConnection();
        String result = null;

        // CallableStatement for stored procedure with OUT parameter
        String call = "{CALL record_sensor_reading(?, ?, ?)}";

        try (CallableStatement cs = conn.prepareCall(call)) {
            cs.setInt(1, sensorId);
            cs.setDouble(2, value);
            cs.registerOutParameter(3, Types.VARCHAR); // OUT p_result

            cs.execute();

            result = cs.getString(3);  // fetch OUT parameter
        }

        return result;
    }

    /**
     * getAllActiveSensors
     * Returns all active sensors from the DB.
     * Demonstrates polymorphism: creates MoistureSensor or TemperatureSensor
     * based on the sensor_type column.
     */
    public List<Sensor> getAllActiveSensors()
            throws SQLException, ClassNotFoundException {

        Connection conn = DBConnection.getInstance().getConnection();
        List<Sensor> sensors = new ArrayList<>();

        String sql = "SELECT sensor_id, plot_id, sensor_type, " +
                     "location_desc, trust_score, is_active " +
                     "FROM sensor WHERE is_active = 1 ORDER BY sensor_id";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int     sid   = rs.getInt("sensor_id");
                int     pid   = rs.getInt("plot_id");
                String  type  = rs.getString("sensor_type");
                String  loc   = rs.getString("location_desc");
                double  trust = rs.getDouble("trust_score");
                boolean act   = rs.getBoolean("is_active");

                // Polymorphism: instantiate correct subclass
                Sensor sensor;
                if ("moisture".equals(type)) {
                    sensor = new MoistureSensor(sid, pid, loc, trust, act);
                } else if ("temperature".equals(type)) {
                    sensor = new TemperatureSensor(sid, pid, loc, trust, act);
                } else {
                    // Generic sensor for other types
                    sensor = new MoistureSensor(sid, pid, loc, trust, act);
                    sensor.setSensorType(type);
                }

                sensors.add(sensor);
            }
        }

        return sensors;
    }

    /**
     * getSensorById - fetches a single sensor
     */
    public Sensor getSensorById(int sensorId)
            throws SQLException, ClassNotFoundException {

        Connection conn = DBConnection.getInstance().getConnection();

        String sql = "SELECT sensor_id, plot_id, sensor_type, " +
                     "location_desc, trust_score, is_active " +
                     "FROM sensor WHERE sensor_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sensorId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int     sid   = rs.getInt("sensor_id");
                    int     pid   = rs.getInt("plot_id");
                    String  type  = rs.getString("sensor_type");
                    String  loc   = rs.getString("location_desc");
                    double  trust = rs.getDouble("trust_score");
                    boolean act   = rs.getBoolean("is_active");

                    if ("moisture".equals(type)) {
                        return new MoistureSensor(sid, pid, loc, trust, act);
                    } else {
                        return new TemperatureSensor(sid, pid, loc, trust, act);
                    }
                }
            }
        }

        return null;
    }
}
