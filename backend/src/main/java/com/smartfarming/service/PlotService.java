package com.smartfarming.service;

import com.smartfarming.dao.PlotDAO;
import com.smartfarming.model.PlotStatus;

import java.sql.SQLException;
import java.util.List;

/**
 * PlotService — thin service, delegates to PlotDAO.
 */
public class PlotService {

    private final PlotDAO plotDAO;

    public PlotService() {
        this.plotDAO = new PlotDAO();
    }

    public List<PlotStatus> getAllPlots()
            throws SQLException, ClassNotFoundException {
        return plotDAO.getAllPlotStatuses();
    }

    public PlotStatus getPlotById(int plotId)
            throws SQLException, ClassNotFoundException {
        return plotDAO.getPlotStatusById(plotId);
    }
}
