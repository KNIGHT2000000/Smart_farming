package com.smartfarming.dao;

import com.smartfarming.db.DBConnection;
import com.smartfarming.model.PlotStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PlotDAO
 *
 * Reads from plot_status_view.
 * Pure data access — zero business logic.
 */
public class PlotDAO {

    /**
     * getAllPlotStatuses — returns current status of all plots.
     */
    public List<PlotStatus> getAllPlotStatuses()
            throws SQLException, ClassNotFoundException {

        Connection conn = DBConnection.getInstance().getConnection();
        List<PlotStatus> list = new ArrayList<>();

        String sql =
            "SELECT plot_id, plot_name, area_sqm, plot_status, " +
            "       planting_date, days_since_planting, crop_name, " +
            "       base_duration_days, current_stage, stage_order, " +
            "       start_day, end_day, min_moisture, max_moisture, " +
            "       irrigation_threshold_moisture, min_temperature, max_temperature, " +
            "       latest_moisture, latest_temperature, last_reading_at, " +
            "       active_sensor_count, min_sensor_trust_score, " +
            "       unresolved_alert_count, irrigation_active " +
            "FROM plot_status_view " +
            "ORDER BY plot_id";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                PlotStatus p = mapRow(rs);
                list.add(p);
            }
        }

        return list;
    }

    /**
     * getPlotStatusById — single plot lookup.
     */
    public PlotStatus getPlotStatusById(int plotId)
            throws SQLException, ClassNotFoundException {

        Connection conn = DBConnection.getInstance().getConnection();

        String sql =
            "SELECT plot_id, plot_name, area_sqm, plot_status, " +
            "       planting_date, days_since_planting, crop_name, " +
            "       base_duration_days, current_stage, stage_order, " +
            "       start_day, end_day, min_moisture, max_moisture, " +
            "       irrigation_threshold_moisture, min_temperature, max_temperature, " +
            "       latest_moisture, latest_temperature, last_reading_at, " +
            "       active_sensor_count, min_sensor_trust_score, " +
            "       unresolved_alert_count, irrigation_active " +
            "FROM plot_status_view " +
            "WHERE plot_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, plotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    // ── Private helper to map ResultSet row → PlotStatus ──────
    private PlotStatus mapRow(ResultSet rs) throws SQLException {
        PlotStatus p = new PlotStatus();
        p.setPlotId(rs.getInt("plot_id"));
        p.setPlotName(rs.getString("plot_name"));
        p.setAreaSqm(rs.getDouble("area_sqm"));
        p.setPlotStatus(rs.getString("plot_status"));
        p.setPlantingDate(rs.getDate("planting_date"));
        p.setDaysSincePlanting(rs.getInt("days_since_planting"));
        p.setCropName(rs.getString("crop_name"));
        p.setBaseDurationDays(rs.getInt("base_duration_days"));
        p.setCurrentStage(rs.getString("current_stage"));
        p.setStageOrder(rs.getInt("stage_order"));
        p.setStartDay(rs.getInt("start_day"));
        p.setEndDay(rs.getInt("end_day"));
        p.setMinMoisture(rs.getDouble("min_moisture"));
        p.setMaxMoisture(rs.getDouble("max_moisture"));
        p.setIrrigationThreshold(rs.getDouble("irrigation_threshold_moisture"));
        p.setMinTemperature(rs.getDouble("min_temperature"));
        p.setMaxTemperature(rs.getDouble("max_temperature"));
        p.setLatestMoisture(rs.getString("latest_moisture"));
        p.setLatestTemperature(rs.getString("latest_temperature"));
        p.setLastReadingAt(rs.getTimestamp("last_reading_at"));
        p.setActiveSensorCount(rs.getInt("active_sensor_count"));

        double trust = rs.getDouble("min_sensor_trust_score");
        p.setMinSensorTrustScore(rs.wasNull() ? null : trust);

        p.setUnresolvedAlertCount(rs.getInt("unresolved_alert_count"));
        p.setIrrigationActive(rs.getBoolean("irrigation_active"));
        return p;
    }
}
