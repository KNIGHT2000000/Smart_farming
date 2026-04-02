-- =============================================================
-- IoT-Driven Crop Stage Aware Smart Farming System
-- FILE: schema.sql
-- PURPOSE: Create all tables with proper normalization (3NF),
--          primary keys, foreign keys, and constraints
-- DATABASE: MySQL 8.0+
-- =============================================================

CREATE DATABASE IF NOT EXISTS smart_farming;
USE smart_farming;

-- =============================================================
-- TABLE: crop
-- Master table for crop types.
-- =============================================================
CREATE TABLE IF NOT EXISTS crop (
    crop_id              INT          AUTO_INCREMENT PRIMARY KEY,
    crop_name            VARCHAR(100) NOT NULL UNIQUE,
    description          TEXT,
    total_duration_days  INT          NOT NULL COMMENT 'Total days from planting to harvest',
    created_at           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Master table for crop types';

-- =============================================================
-- TABLE: crop_stage
-- Each crop has multiple ordered growth stages.
-- FK: crop_id -> crop
-- =============================================================
CREATE TABLE IF NOT EXISTS crop_stage (
    stage_id                INT          AUTO_INCREMENT PRIMARY KEY,
    crop_id                 INT          NOT NULL,
    stage_name              VARCHAR(100) NOT NULL,
    stage_order             INT          NOT NULL,
    start_day               INT          NOT NULL COMMENT 'Days after planting when stage begins',
    end_day                 INT          NOT NULL COMMENT 'Days after planting when stage ends',
    min_moisture_pct        DECIMAL(5,2) NOT NULL,
    max_moisture_pct        DECIMAL(5,2) NOT NULL,
    min_temp_celsius        DECIMAL(5,2) NOT NULL,
    max_temp_celsius        DECIMAL(5,2) NOT NULL,
    irrigation_duration_min INT          DEFAULT 20,
    CONSTRAINT fk_cs_crop  FOREIGN KEY (crop_id) REFERENCES crop(crop_id) ON DELETE CASCADE,
    CONSTRAINT uq_cs_order UNIQUE (crop_id, stage_order)
) ENGINE=InnoDB COMMENT='Growth stages per crop with irrigation thresholds';

-- =============================================================
-- TABLE: plot
-- Physical farming plots. Each plot grows one crop at a time.
-- =============================================================
CREATE TABLE IF NOT EXISTS plot (
    plot_id          INT          AUTO_INCREMENT PRIMARY KEY,
    plot_name        VARCHAR(100) NOT NULL UNIQUE,
    location_desc    VARCHAR(255),
    area_sqm         DECIMAL(8,2) NOT NULL,
    crop_id          INT          NOT NULL,
    current_stage_id INT          NULL,
    planted_date     DATE         NOT NULL,
    is_active        BOOLEAN      DEFAULT TRUE,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_plot_crop  FOREIGN KEY (crop_id)          REFERENCES crop(crop_id),
    CONSTRAINT fk_plot_stage FOREIGN KEY (current_stage_id) REFERENCES crop_stage(stage_id)
) ENGINE=InnoDB COMMENT='Physical farming plots';

-- =============================================================
-- TABLE: sensor
-- IoT sensors attached to plots. Trust score degrades on faults.
-- =============================================================
CREATE TABLE IF NOT EXISTS sensor (
    sensor_id       INT          AUTO_INCREMENT PRIMARY KEY,
    plot_id         INT          NOT NULL,
    sensor_type     ENUM('MOISTURE','TEMPERATURE','HUMIDITY','PH') NOT NULL,
    unit            VARCHAR(20)  NOT NULL,
    model_name      VARCHAR(100) DEFAULT 'Generic IoT Sensor',
    trust_score     INT          DEFAULT 100 COMMENT '0-100; decremented by trigger on invalid readings',
    is_active       BOOLEAN      DEFAULT TRUE,
    installed_date  DATE         NOT NULL,
    last_reading_at TIMESTAMP    NULL,
    CONSTRAINT fk_sensor_plot FOREIGN KEY (plot_id) REFERENCES plot(plot_id) ON DELETE CASCADE,
    CONSTRAINT chk_trust      CHECK (trust_score BETWEEN 0 AND 100)
) ENGINE=InnoDB COMMENT='IoT sensors on plots';

-- =============================================================
-- TABLE: sensor_reading
-- Time-series table. High insert volume; indexed for range queries.
-- =============================================================
CREATE TABLE IF NOT EXISTS sensor_reading (
    reading_id  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    sensor_id   INT          NOT NULL,
    value       DECIMAL(8,3) NOT NULL,
    recorded_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    is_valid    BOOLEAN      DEFAULT TRUE,
    CONSTRAINT fk_reading_sensor FOREIGN KEY (sensor_id) REFERENCES sensor(sensor_id) ON DELETE CASCADE,
    INDEX idx_sensor_time (sensor_id, recorded_at),
    INDEX idx_recorded_at (recorded_at)
) ENGINE=InnoDB COMMENT='Time-series IoT sensor readings';

-- =============================================================
-- TABLE: irrigation_event
-- Every irrigation decision is recorded here by stored procedure.
-- =============================================================
CREATE TABLE IF NOT EXISTS irrigation_event (
    event_id         INT          AUTO_INCREMENT PRIMARY KEY,
    plot_id          INT          NOT NULL,
    triggered_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    duration_minutes INT          NOT NULL,
    trigger_reason   VARCHAR(255) NOT NULL,
    moisture_value   DECIMAL(5,2) NULL,
    status           ENUM('TRIGGERED','COMPLETED','CANCELLED') DEFAULT 'TRIGGERED',
    CONSTRAINT fk_irr_plot FOREIGN KEY (plot_id) REFERENCES plot(plot_id) ON DELETE CASCADE,
    INDEX idx_irr_plot_time (plot_id, triggered_at)
) ENGINE=InnoDB COMMENT='Irrigation events triggered by DBMS logic';

-- =============================================================
-- TABLE: alert
-- System-generated alerts. Severity: INFO, WARNING, CRITICAL.
-- =============================================================
CREATE TABLE IF NOT EXISTS alert (
    alert_id    INT          AUTO_INCREMENT PRIMARY KEY,
    plot_id     INT          NOT NULL,
    sensor_id   INT          NULL,
    alert_type  VARCHAR(50)  NOT NULL,
    message     TEXT         NOT NULL,
    severity    ENUM('INFO','WARNING','CRITICAL') DEFAULT 'WARNING',
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    is_resolved BOOLEAN      DEFAULT FALSE,
    CONSTRAINT fk_alert_plot   FOREIGN KEY (plot_id)   REFERENCES plot(plot_id),
    CONSTRAINT fk_alert_sensor FOREIGN KEY (sensor_id) REFERENCES sensor(sensor_id),
    INDEX idx_alert_plot      (plot_id, created_at),
    INDEX idx_alert_unresolved (is_resolved, severity)
) ENGINE=InnoDB COMMENT='Alerts generated by DBMS stored procedures';

-- =============================================================
-- SEED DATA
-- =============================================================

INSERT INTO crop (crop_name, description, total_duration_days) VALUES
('Wheat', 'Winter wheat crop',       120),
('Rice',  'Paddy rice cultivation',  150),
('Maize', 'Corn / maize crop',       100);

-- Wheat Stages
INSERT INTO crop_stage (crop_id, stage_name, stage_order, start_day, end_day,
    min_moisture_pct, max_moisture_pct, min_temp_celsius, max_temp_celsius, irrigation_duration_min) VALUES
(1, 'Germination', 1,  0,  10, 60.0, 80.0,  8.0, 22.0, 15),
(1, 'Seedling',    2, 11,  25, 55.0, 75.0, 10.0, 25.0, 20),
(1, 'Tillering',   3, 26,  55, 50.0, 70.0, 12.0, 28.0, 25),
(1, 'Heading',     4, 56,  90, 45.0, 65.0, 15.0, 30.0, 30),
(1, 'Maturity',    5, 91, 120, 30.0, 50.0, 15.0, 35.0, 10);

-- Rice Stages
INSERT INTO crop_stage (crop_id, stage_name, stage_order, start_day, end_day,
    min_moisture_pct, max_moisture_pct, min_temp_celsius, max_temp_celsius, irrigation_duration_min) VALUES
(2, 'Germination', 1,  0,  14, 70.0, 90.0, 20.0, 30.0, 20),
(2, 'Seedling',    2, 15,  30, 65.0, 85.0, 22.0, 32.0, 25),
(2, 'Tillering',   3, 31,  70, 60.0, 80.0, 24.0, 34.0, 30),
(2, 'Flowering',   4, 71, 110, 55.0, 75.0, 24.0, 32.0, 35),
(2, 'Maturity',    5,111, 150, 40.0, 60.0, 20.0, 30.0, 10);

-- Maize Stages
INSERT INTO crop_stage (crop_id, stage_name, stage_order, start_day, end_day,
    min_moisture_pct, max_moisture_pct, min_temp_celsius, max_temp_celsius, irrigation_duration_min) VALUES
(3, 'Germination', 1,  0,   8, 55.0, 75.0, 15.0, 28.0, 15),
(3, 'Seedling',    2,  9,  20, 50.0, 70.0, 18.0, 30.0, 20),
(3, 'Vegetative',  3, 21,  55, 45.0, 65.0, 20.0, 32.0, 25),
(3, 'Tasseling',   4, 56,  80, 40.0, 60.0, 22.0, 34.0, 30),
(3, 'Maturity',    5, 81, 100, 30.0, 50.0, 18.0, 32.0, 10);

-- Sample Plots
INSERT INTO plot (plot_name, location_desc, area_sqm, crop_id, current_stage_id, planted_date) VALUES
('Plot-A1', 'North field, near main gate',   2500.00, 1, 1, DATE_SUB(CURDATE(), INTERVAL 5  DAY)),
('Plot-B2', 'South field, beside canal',     1800.00, 2, 6, DATE_SUB(CURDATE(), INTERVAL 20 DAY)),
('Plot-C3', 'East field, greenhouse zone',   3200.00, 3, 9, DATE_SUB(CURDATE(), INTERVAL 30 DAY));

-- Sample Sensors (moisture + temperature per plot)
INSERT INTO sensor (plot_id, sensor_type, unit, model_name, trust_score, is_active, installed_date) VALUES
(1, 'MOISTURE',    '%',  'ESP32-Capacitive-v2', 100, TRUE, CURDATE()),
(1, 'TEMPERATURE', 'C',  'ESP32-DS18B20',       100, TRUE, CURDATE()),
(2, 'MOISTURE',    '%',  'ESP32-Capacitive-v2', 100, TRUE, CURDATE()),
(2, 'TEMPERATURE', 'C',  'ESP32-DS18B20',       100, TRUE, CURDATE()),
(3, 'MOISTURE',    '%',  'ESP32-Capacitive-v2', 100, TRUE, CURDATE()),
(3, 'TEMPERATURE', 'C',  'ESP32-DS18B20',       100, TRUE, CURDATE());
