package com.smartfarming.model;

/**
 * MoistureSensor - Subclass of Sensor
 *
 * OOP Concepts:
 *   - Inheritance   : extends Sensor
 *   - Polymorphism  : overrides getUnit() and isReadingValid()
 *   - Encapsulation : adds moisture-specific fields
 */
public class MoistureSensor extends Sensor {

    // Moisture-specific threshold (from crop stage, stored in DB)
    private double irrigationThreshold;

    public MoistureSensor(int sensorId, int plotId, String locationDesc,
                          double trustScore, boolean active) {
        super(sensorId, plotId, "moisture", locationDesc, trustScore, active);
        this.irrigationThreshold = 50.0; // default; overridden by DB data
    }

    /**
     * Polymorphism: MoistureSensor overrides getUnit()
     * Returns % (percentage soil moisture)
     */
    @Override
    public String getUnit() {
        return "%";
    }

    /**
     * Polymorphism: Physical range validation for moisture sensors.
     * Moisture must be between 0% and 100%.
     */
    @Override
    public boolean isReadingValid(double value) {
        return value >= 0.0 && value <= 100.0;
    }

    /**
     * Polymorphism: Simulates a moisture reading.
     * In real ESP32 code, this would read the ADC pin.
     */
    @Override
    public double readValue() {
        // Simulated: returns a value between 30–80% (realistic range)
        return 30.0 + Math.random() * 50.0;
    }

    // Moisture-specific getters/setters
    public double getIrrigationThreshold() { return irrigationThreshold; }
    public void setIrrigationThreshold(double t) { this.irrigationThreshold = t; }

    @Override
    public String toString() {
        return "MoistureSensor{" + super.toString() +
               ", threshold=" + irrigationThreshold + "%}";
    }
}
