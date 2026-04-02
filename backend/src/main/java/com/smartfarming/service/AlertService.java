package com.smartfarming.service;

import com.smartfarming.dao.AlertDAO;
import com.smartfarming.model.Alert;

import java.sql.SQLException;
import java.util.List;

/**
 * AlertService — thin service, delegates to AlertDAO.
 */
public class AlertService {

    private final AlertDAO alertDAO;

    public AlertService() {
        this.alertDAO = new AlertDAO();
    }

    public List<Alert> getAlerts(boolean onlyUnresolved, int limit)
            throws SQLException, ClassNotFoundException {
        return alertDAO.getAllAlerts(onlyUnresolved, limit);
    }

    public void resolveAlert(long alertId)
            throws SQLException, ClassNotFoundException {
        alertDAO.resolveAlert(alertId);
    }
}
