package com.smartfarming.service;

import com.smartfarming.dao.IrrigationDAO;
import com.smartfarming.model.IrrigationHistory;

import java.sql.SQLException;
import java.util.List;

/**
 * IrrigationService — thin service, delegates to IrrigationDAO.
 */
public class IrrigationService {

    private final IrrigationDAO irrigationDAO;

    public IrrigationService() {
        this.irrigationDAO = new IrrigationDAO();
    }

    public List<IrrigationHistory> getHistory(int plotId, int limit)
            throws SQLException, ClassNotFoundException {
        return irrigationDAO.getIrrigationHistory(plotId, limit);
    }

    public void completeIrrigation(long eventId)
            throws SQLException, ClassNotFoundException {
        irrigationDAO.completeIrrigation(eventId);
    }
}
