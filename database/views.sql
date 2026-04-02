-- =============================================================
-- FILE: views.sql
-- PURPOSE: Read-only views for Java backend to query.
--          Frontend reads these views via Java GET APIs.
-- =============================================================

USE smart_farming;

-- =============================================================
-- VIEW: alert_view
-- Shows all unresolved alerts with plot and sensor context.
-- Used by: GET /api/alerts
-- =============================================================
DROP VIEW IF EXISTS alert_view;

CREATE VIEW alert_view AS
SELECT
    a.alert_id,
    a.alert_type,
    a.severity,
    a.message,
    a.created_at,
    a.is_resolved,
    p.plot_name,
    p.location_desc        AS plot_location,
    s.sensor_type,
    s.trust_score          AS sensor_trust,
    c.crop_name,
    cs.stage_name          AS current_stage
FROM       alert a
JOIN       plot  p  ON p.plot_id   = a.plot_id
LEFT JOIN  sensor s ON s.sensor_id = a.sensor_id
JOIN       crop   c  ON c.crop_id  = p.crop_id
LEFT JOIN  crop_stage cs ON cs.stage_id = p.current_stage_id
ORDER BY
    FIELD(a.severity, 'CRITICAL', 'WARNING', 'INFO'),
    a.created_at DESC;

-- =============================================================
-- VIEW: plot_status_view
-- Real-time snapshot of each plot: stage, latest readings.
-- Used by: GET /api/plot-status
-- =============================================================
DROP VIEW IF EXISTS plot_status_view;

CREATE VIEW plot_status_view AS
SELECT
    p.plot_id,
    p.plot_name,
    p.location_desc,
    p.area_sqm,
    p.planted_date,
    DATEDIFF(CURDATE(), p.planted_date)  AS days_since_planting,
    c.crop_name,
    cs.stage_name                         AS current_stage,
    cs.stage_order,
    cs.min_moisture_pct,
    cs.max_moisture_pct,
    cs.min_temp_celsius,
    cs.max_temp_celsius,

    -- Latest moisture reading
    (SELECT sr.value
     FROM   sensor_reading sr
     JOIN   sensor         s  ON s.sensor_id = sr.sensor_id
     WHERE  s.plot_id      = p.plot_id
       AND  s.sensor_type  = 'MOISTURE'
       AND  sr.is_valid    = TRUE
     ORDER BY sr.recorded_at DESC LIMIT 1)       AS latest_moisture_pct,

    -- Latest temperature reading
    (SELECT sr.value
     FROM   sensor_reading sr
     JOIN   sensor         s  ON s.sensor_id = sr.sensor_id
     WHERE  s.plot_id      = p.plot_id
       AND  s.sensor_type  = 'TEMPERATURE'
       AND  sr.is_valid    = TRUE
     ORDER BY sr.recorded_at DESC LIMIT 1)       AS latest_temp_celsius,

    -- Moisture status relative to stage thresholds
    CASE
        WHEN (SELECT sr.value FROM sensor_reading sr
              JOIN sensor s ON s.sensor_id = sr.sensor_id
              WHERE s.plot_id = p.plot_id AND s.sensor_type = 'MOISTURE'
                AND sr.is_valid = TRUE
              ORDER BY sr.recorded_at DESC LIMIT 1) < cs.min_moisture_pct THEN 'LOW'
        WHEN (SELECT sr.value FROM sensor_reading sr
              JOIN sensor s ON s.sensor_id = sr.sensor_id
              WHERE s.plot_id = p.plot_id AND s.sensor_type = 'MOISTURE'
                AND sr.is_valid = TRUE
              ORDER BY sr.recorded_at DESC LIMIT 1) > cs.max_moisture_pct THEN 'HIGH'
        ELSE 'NORMAL'
    END AS moisture_status,

    -- Count of unresolved alerts
    (SELECT COUNT(*) FROM alert WHERE plot_id = p.plot_id AND is_resolved = FALSE) AS open_alerts,

    -- Last irrigation time
    (SELECT triggered_at FROM irrigation_event
     WHERE  plot_id = p.plot_id ORDER BY triggered_at DESC LIMIT 1)                AS last_irrigated_at,

    p.is_active
FROM       plot      p
JOIN       crop      c   ON c.crop_id   = p.crop_id
LEFT JOIN  crop_stage cs ON cs.stage_id = p.current_stage_id
WHERE p.is_active = TRUE;

-- =============================================================
-- VIEW: irrigation_history_view
-- Full audit trail of all irrigation events.
-- Used by: GET /api/irrigation-history
-- =============================================================
DROP VIEW IF EXISTS irrigation_history_view;

CREATE VIEW irrigation_history_view AS
SELECT
    ie.event_id,
    ie.triggered_at,
    ie.duration_minutes,
    ie.trigger_reason,
    ie.moisture_value,
    ie.status,
    p.plot_name,
    p.location_desc       AS plot_location,
    c.crop_name,
    cs.stage_name         AS stage_at_trigger
FROM       irrigation_event ie
JOIN       plot             p   ON p.plot_id    = ie.plot_id
JOIN       crop             c   ON c.crop_id    = p.crop_id
LEFT JOIN  crop_stage       cs  ON cs.stage_id  = p.current_stage_id
ORDER BY   ie.triggered_at DESC;

-- =============================================================
-- VIEW: sensor_health_view
-- Sensor trust scores and last readings — useful for debugging.
-- =============================================================
DROP VIEW IF EXISTS sensor_health_view;

CREATE VIEW sensor_health_view AS
SELECT
    s.sensor_id,
    s.sensor_type,
    s.unit,
    s.model_name,
    s.trust_score,
    s.is_active,
    s.last_reading_at,
    p.plot_name,
    CASE
        WHEN s.trust_score >= 70 THEN 'HEALTHY'
        WHEN s.trust_score >= 30 THEN 'DEGRADED'
        ELSE                          'CRITICAL'
    END AS health_status,
    (SELECT COUNT(*) FROM sensor_reading
     WHERE sensor_id = s.sensor_id AND is_valid = FALSE
       AND recorded_at > NOW() - INTERVAL 24 HOUR) AS invalid_readings_24h
FROM  sensor s
JOIN  plot   p ON p.plot_id = s.plot_id;
