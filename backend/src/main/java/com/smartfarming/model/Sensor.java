package com.smartfarming.model;

/**
 * Sensor - Abstract Base Class
 *
 * OOP Concepts:
 *   - Encapsulation : all fields are private
 *   - Inheritance   : MoistureSensor / TemperatureSensor extend this
 *   - Abstraction   : getUnit() is abstract (subclasses must define)
 */
public abstract class Sensor implements SensorReadable {

    // ── Encapsulated fields (private) ─────────────────────────
    private int    sensorId;
    private int    plotId;
    private String sensorType;
    private String locationDesc;
    private double trustScore;
    private boolean active;

    // ── Constructor ───────────────────────────────────────────
    public Sensor(int sensorId, int plotId, String sensorType,
                  String locationDesc, double trustScore, boolean active) {
        this.sensorId     = sensorId;
        this.plotId       = plotId;
        this.sensorType   = sensorType;
        this.locationDesc = locationDesc;
        this.trustScore   = trustScore;
        this.active       = active;
    }

    // ── Abstract methods (polymorphism) ───────────────────────
    @Override
    public abstract String getUnit();

    @Override
    public abstract boolean isReadingValid(double value);

    /**
     * Default readValue — subclasses can override for simulation logic.
     * In real ESP32 integration this would invoke sensor hardware.
     */
    @Override
    public double readValue() {
        return 0.0;  // overridden by subclasses
    }

    // ── Getters ───────────────────────────────────────────────
    public int    getSensorId()    { return sensorId;    }
    public int    getPlotId()      { return plotId;      }
    public String getSensorType()  { return sensorType;  }
    public String getLocationDesc(){ return locationDesc; }
    public double getTrustScore()  { return trustScore;  }
    public boolean isActive()      { return active;      }

    // ── Setters ───────────────────────────────────────────────
    public void setSensorId(int id)         { this.sensorId    = id;    }
    public void setPlotId(int plotId)       { this.plotId      = plotId; }
    public void setSensorType(String t)     { this.sensorType  = t;     }
    public void setLocationDesc(String loc) { this.locationDesc = loc;  }
    public void setTrustScore(double score) { this.trustScore  = score; }
    public void setActive(boolean a)        { this.active      = a;     }

    @Override
    public String toString() {
        return "Sensor{id=" + sensorId +
               ", type=" + sensorType +
               ", plot=" + plotId +
               ", trust=" + trustScore + "%" +
               ", active=" + active + "}";
    }
}
