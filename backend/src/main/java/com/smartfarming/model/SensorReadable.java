package com.smartfarming.model;

/**
 * SensorReadable - Interface
 *
 * OOP Concept: Interface
 * Any sensor that can produce a reading must implement this.
 */
public interface SensorReadable {
    double readValue();
    String getUnit();
    boolean isReadingValid(double value);
}
