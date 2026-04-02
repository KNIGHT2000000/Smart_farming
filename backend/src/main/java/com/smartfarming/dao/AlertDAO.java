package com.smartfarming.dao;

import com.smartfarming.db.DBConnection;
import com.smartfarming.model.Alert;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AlertDAO
 *
 * Reads from alert_view (created in views.sql).
 * No business logic here — all logic is in the DB.
 */
public class AlertDAO {

    /**
     * getAllAlerts — returns all alerts (newest first) from alert_view.
     * Optionally filtered by resolved status.
     *
     * @param onlyUnresolved if true, returns only is_resolved = 0
     * @param limit          max number of records (0 = no limit)
     */
    public List<Alert> getAllAlerts(boolean onlyUnresolved, int limit)
            throws SQLException, ClassNotFoundException {

        Connection conn = DBConnection.getInstance().getConnection();
        List<Alert> alerts = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT alert_id, alert_type, message, severity, " +
            "       is_resolved, created_at, resolved_at, " +
            "       plot_id, plot_name, sensor_id, sensor_type, " +
            "       sensor_location, sensor_trust_score, " +
            "       crop_name, crop_stage_at_alert " +
            "FROM alert_view "
        );

        if (onlyUnresolved) {
            sql.append("WHERE is_resolved = 0 ");
        }

        sql.append("ORDER BY created_at DESC ");

        if (limit > 0) {
            sql.append("LIMIT ").append(limit);
        }

        try (PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Alert a = new Alert();
                a.setAlertId(rs.getLong("alert_id"));
                a.setAlertType(rs.getString("alert_type"));
                a.setMessage(rs.getString("message"));
                a.setSeverity(rs.getString("severity"));
                a.setResolved(rs.getBoolean("is_resolved"));
                a.setCreatedAt(rs.getTimestamp("created_at"));
                a.setResolvedAt(rs.getTimestamp("resolved_at"));
                a.setPlotId(rs.getInt("plot_id"));
                a.setPlotName(rs.getString("plot_name"));

                // sensor_id can be NULL in DB
                int sid = rs.getInt("sensor_id");
                a.setSensorId(rs.wasNull() ? null : sid);

                a.setSensorType(rs.getString("sensor_type"));
                a.setSensorLocation(rs.getString("sensor_location"));

                double trust = rs.getDouble("sensor_trust_score");
                a.setSensorTrustScore(rs.wasNull() ? null : trust);

                a.setCropName(rs.getString("crop_name"));
                a.setCropStageAtAlert(rs.getString("crop_stage_at_alert"));

                alerts.add(a);
            }
        }

        return alerts;
    }

    /**
     * resolveAlert — calls stored procedure to mark alert resolved.
     */
    public void resolveAlert(long alertId)
            throws SQLException, ClassNotFoundException {

        Connection conn = DBConnection.getInstance().getConnection();

        try (CallableStatement cs = conn.prepareCall("{CALL resolve_alert(?)}")) {
            cs.setLong(1, alertId);
            cs.execute();
        }
    }
}
