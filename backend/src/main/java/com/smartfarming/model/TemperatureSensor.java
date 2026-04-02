package com.smartfarming.model;

/**
 * TemperatureSensor - Subclass of Sensor
 *
 * OOP Concepts:
 *   - Inheritance   : extends Sensor
 *   - Polymorphism  : overrides getUnit() and isReadingValid()
 */
public class TemperatureSensor extends Sensor {

    public TemperatureSensor(int sensorId, int plotId, String locationDesc,
                             double trustScore, boolean active) {
        super(sensorId, plotId, "temperature", locationDesc, trustScore, active);
    }

    /**
     * Polymorphism: TemperatureSensor returns Celsius unit
     */
    @Override
    public String getUnit() {
        return "°C";
    }

    /**
     * Polymorphism: Temperature must be in physical range -10 to 60°C
     */
    @Override
    public boolean isReadingValid(double value) {
        return value >= -10.0 && value <= 60.0;
    }

    /**
     * Polymorphism: Simulates a temperature reading
     * In ESP32, this reads a DS18B20 or DHT22 sensor
     */
    @Override
    public double readValue() {
        // Simulated: realistic field temperature 15–40°C
        return 15.0 + Math.random() * 25.0;
    }

    @Override
    public String toString() {
        return "TemperatureSensor{" + super.toString() + "}";
    }
}
