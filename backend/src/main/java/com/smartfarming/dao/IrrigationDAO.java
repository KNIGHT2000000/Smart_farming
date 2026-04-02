package com.smartfarming.dao;

import com.smartfarming.db.DBConnection;
import com.smartfarming.model.IrrigationHistory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * IrrigationDAO
 *
 * Reads from irrigation_history_view.
 * Can also call complete_irrigation stored procedure.
 */
public class IrrigationDAO {

    /**
     * getIrrigationHistory — returns irrigation events, newest first.
     *
     * @param plotId filter by plot (0 = all plots)
     * @param limit  max records (0 = no limit)
     */
    public List<IrrigationHistory> getIrrigationHistory(int plotId, int limit)
            throws SQLException, ClassNotFoundException {

        Connection conn = DBConnection.getInstance().getConnection();
        List<IrrigationHistory> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT event_id, triggered_at, duration_minutes, " +
            "       water_amount_liters, irrigation_status, trigger_reason, " +
            "       plot_id, plot_name, crop_name, stage_at_trigger, " +
            "       moisture_at_trigger, triggering_sensor_id, " +
            "       triggering_sensor_type, minutes_ago " +
            "FROM irrigation_history_view "
        );

        if (plotId > 0) {
            sql.append("WHERE plot_id = ").append(plotId).append(" ");
        }

        sql.append("ORDER BY triggered_at DESC ");

        if (limit > 0) {
            sql.append("LIMIT ").append(limit);
        }

        try (PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                IrrigationHistory h = new IrrigationHistory();
                h.setEventId(rs.getLong("event_id"));
                h.setTriggeredAt(rs.getTimestamp("triggered_at"));
                h.setDurationMinutes(rs.getInt("duration_minutes"));
                h.setWaterAmountLiters(rs.getDouble("water_amount_liters"));
                h.setIrrigationStatus(rs.getString("irrigation_status"));
                h.setNotes(rs.getString("trigger_reason"));
         
                h.setPlotId(rs.getInt("plot_id"));
                h.setPlotName(rs.getString("plot_name"));
                h.setCropName(rs.getString("crop_name"));
                h.setStageAtTrigger(rs.getString("stage_at_trigger"));
                h.setMoistureAtTrigger(rs.getDouble("moisture_at_trigger"));
                h.setTriggeringSensorId(rs.getInt("triggering_sensor_id"));
                h.setTriggeringSensorType(rs.getString("triggering_sensor_type"));
                h.setMinutesAgo(rs.getLong("minutes_ago"));
                list.add(h);
            }
        }

        return list;
    }

    /**
     * completeIrrigation — marks an event as completed via stored procedure.
     */
    public void completeIrrigation(long eventId)
            throws SQLException, ClassNotFoundException {

        Connection conn = DBConnection.getInstance().getConnection();

        try (CallableStatement cs = conn.prepareCall("{CALL complete_irrigation(?)}")) {
            cs.setLong(1, eventId);
            cs.execute();
        }
    }
}
