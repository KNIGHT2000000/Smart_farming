-- =====================================================================
-- IoT Smart Farming — Complete Test & Verification Script
-- Run these queries AFTER loading schema, procedures, triggers, views
-- and AFTER the Python simulator has sent some readings.
-- =====================================================================

USE smart_farming;

-- ─────────────────────────────────────────────────────────────────
-- SECTION 1: VERIFY DATABASE STRUCTURE
-- ─────────────────────────────────────────────────────────────────

-- 1a. Confirm all 7 tables exist
SELECT TABLE_NAME, TABLE_ROWS, ENGINE
FROM   INFORMATION_SCHEMA.TABLES
WHERE  TABLE_SCHEMA = 'smart_farming'
  AND  TABLE_TYPE   = 'BASE TABLE'
ORDER  BY TABLE_NAME;
-- Expected: alert, crop, crop_stage, irrigation_event, plot, sensor, sensor_reading

-- 1b. Confirm all 4 views exist
SELECT TABLE_NAME
FROM   INFORMATION_SCHEMA.VIEWS
WHERE  TABLE_SCHEMA = 'smart_farming';
-- Expected: alert_view, irrigation_history_view, plot_status_view, sensor_health_view

-- 1c. Confirm stored procedures
SELECT ROUTINE_NAME, ROUTINE_TYPE
FROM   INFORMATION_SCHEMA.ROUTINES
WHERE  ROUTINE_SCHEMA = 'smart_farming';
-- Expected: record_sensor_reading, get_plot_summary, resolve_alert, complete_irrigation

-- 1d. Confirm triggers
SELECT TRIGGER_NAME, EVENT_MANIPULATION, EVENT_OBJECT_TABLE, ACTION_TIMING
FROM   INFORMATION_SCHEMA.TRIGGERS
WHERE  TRIGGER_SCHEMA = 'smart_farming';
-- Expected: trg_update_trust_score, trg_auto_resolve_moisture_ok, trg_irrigation_water_calc

-- ─────────────────────────────────────────────────────────────────
-- SECTION 2: VERIFY SEED DATA
-- ─────────────────────────────────────────────────────────────────

-- 2a. Crops seeded
SELECT crop_id, name, base_duration_days FROM crop;
-- Expected: 3 crops (Wheat=120d, Rice=150d, Cotton=180d)

-- 2b. Growth stages seeded
SELECT c.name AS crop, cs.stage_order, cs.stage_name,
       cs.start_day, cs.end_day,
       cs.min_moisture, cs.max_moisture,
       cs.irrigation_threshold_moisture
FROM   crop_stage cs
JOIN   crop c ON c.crop_id = cs.crop_id
ORDER  BY c.name, cs.stage_order;
-- Expected: 5+4+5 = 14 rows

-- 2c. Plots seeded with current stage awareness
SELECT p.plot_name,
       c.name                             AS crop,
       p.planting_date,
       DATEDIFF(CURDATE(), p.planting_date) AS days_in,
       IFNULL(cs.stage_name, 'Not set yet') AS current_stage
FROM   plot p
JOIN   crop c       ON c.crop_id  = p.crop_id
LEFT   JOIN crop_stage cs ON cs.stage_id = p.current_stage_id;
-- Expected: 3 plots (Plot-A1/Wheat, Plot-B2/Rice, Plot-C3/Cotton)

-- 2d. Sensors seeded
SELECT s.sensor_id, p.plot_name, s.sensor_type,
       s.trust_score, s.is_active, s.location_desc
FROM   sensor s
JOIN   plot   p ON p.plot_id = s.plot_id
ORDER  BY s.sensor_id;
-- Expected: 6 sensors (2 per plot)

-- ─────────────────────────────────────────────────────────────────
-- SECTION 3: MANUAL PROCEDURE TESTS
-- Run these manually to test the stored procedure
-- ─────────────────────────────────────────────────────────────────

-- 3a. TEST: Normal moisture reading (should produce MOISTURE_OK INFO alert)
CALL record_sensor_reading(1, 65.0, @result);
SELECT @result AS procedure_result;
-- Expected: "OK: reading_id=X | stage=Tillering | valid=1 | type=moisture | value=65.0"

-- 3b. TEST: Low moisture reading (should trigger irrigation + CRITICAL alert)
CALL record_sensor_reading(1, 28.0, @result);
SELECT @result AS procedure_result;
-- Expected: "OK: reading_id=X | stage=Tillering | valid=1 | type=moisture | value=28.0"

-- 3c. TEST: High temperature (should trigger WARNING alert)
CALL record_sensor_reading(2, 35.0, @result);
SELECT @result AS procedure_result;
-- Expected: "OK: reading_id=X | stage=Tillering | valid=1 | type=temperature | value=35.0"

-- 3d. TEST: Faulty reading (should trigger trust_score deduction via TRIGGER)
CALL record_sensor_reading(1, 150.0, @result);
SELECT @result AS procedure_result;
-- Expected: "INVALID_READING: value=150.0 failed physical check for type=moisture"

-- ─────────────────────────────────────────────────────────────────
-- SECTION 4: VERIFY STORED PROCEDURE EFFECTS
-- ─────────────────────────────────────────────────────────────────

-- 4a. Check all sensor readings
SELECT sr.reading_id, s.plot_id, s.sensor_type,
       sr.value, sr.unit, sr.is_valid,
       IFNULL(cs.stage_name, 'Unknown') AS stage,
       sr.recorded_at
FROM   sensor_reading sr
JOIN   sensor         s  ON s.sensor_id  = sr.sensor_id
LEFT   JOIN crop_stage cs ON cs.stage_id = sr.stage_id
ORDER  BY sr.recorded_at DESC
LIMIT  20;

-- 4b. Check alerts generated
SELECT a.alert_id, p.plot_name, a.alert_type,
       a.severity, a.message, a.is_resolved,
       a.created_at
FROM   alert a
JOIN   plot  p ON p.plot_id = a.plot_id
ORDER  BY a.created_at DESC
LIMIT  20;

-- 4c. Check irrigation events triggered
SELECT ie.event_id, p.plot_name,
       ie.triggered_at, ie.duration_minutes,
       ie.water_amount_liters, ie.status,
       ie.notes
FROM   irrigation_event ie
JOIN   plot             p  ON p.plot_id = ie.plot_id
ORDER  BY ie.triggered_at DESC;

-- ─────────────────────────────────────────────────────────────────
-- SECTION 5: VERIFY TRIGGER EFFECTS
-- ─────────────────────────────────────────────────────────────────

-- 5a. Trust scores BEFORE faulty reading
SELECT sensor_id, sensor_type, trust_score, is_active FROM sensor ORDER BY sensor_id;

-- 5b. Send 15 consecutive faulty readings to sensor 1
--     This should drive trust_score from 100 → 25 → deactivation below 20
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;
CALL record_sensor_reading(1, 999.0, @r); SELECT @r;

-- 5c. Trust scores AFTER 16 faulty readings (should be ~20 or less, is_active=0)
SELECT sensor_id, sensor_type, trust_score, is_active FROM sensor WHERE sensor_id = 1;
-- Expected: trust_score ≈ 20 (100 - 16×5 = 20), is_active = 0

-- 5d. Check SENSOR_DEACTIVATED alert was auto-generated by trigger
SELECT alert_type, severity, message, created_at
FROM   alert
WHERE  alert_type = 'SENSOR_DEACTIVATED'
ORDER  BY created_at DESC;

-- 5e. Reactivate sensor for further testing
UPDATE sensor SET trust_score = 100.00, is_active = 1 WHERE sensor_id = 1;

-- 5f. Test auto-resolve trigger:
--     A LOW_MOISTURE alert should be auto-resolved when MOISTURE_OK is generated
CALL record_sensor_reading(1, 20.0, @r);   -- Creates LOW_MOISTURE (unresolved)
SELECT alert_id, alert_type, is_resolved FROM alert ORDER BY alert_id DESC LIMIT 3;

CALL record_sensor_reading(1, 70.0, @r);   -- Creates MOISTURE_OK → trigger resolves LOW_MOISTURE
SELECT alert_id, alert_type, is_resolved FROM alert ORDER BY alert_id DESC LIMIT 5;
-- Expected: The earlier LOW_MOISTURE alert now has is_resolved = 1

-- ─────────────────────────────────────────────────────────────────
-- SECTION 6: VERIFY VIEWS
-- ─────────────────────────────────────────────────────────────────

-- 6a. alert_view — enriched alert data
SELECT alert_id, alert_type, severity, plot_name,
       sensor_type, sensor_trust_score, crop_name,
       crop_stage_at_alert, created_at
FROM   alert_view
ORDER  BY created_at DESC
LIMIT  10;

-- 6b. plot_status_view — live dashboard data
SELECT plot_name, crop_name, current_stage, days_since_planting,
       latest_moisture, latest_temperature,
       unresolved_alert_count, irrigation_active,
       min_sensor_trust_score
FROM   plot_status_view;

-- 6c. irrigation_history_view — full audit trail
SELECT plot_name, crop_name, stage_at_trigger,
       moisture_at_trigger, duration_minutes,
       water_amount_liters, irrigation_status,
       triggered_at, minutes_ago
FROM   irrigation_history_view
ORDER  BY triggered_at DESC;

-- 6d. sensor_health_view — sensor reliability summary
SELECT plot_name, sensor_type, trust_score, is_active,
       total_readings, invalid_readings,
       invalid_pct, avg_value, last_reading_at
FROM   sensor_health_view
ORDER  BY invalid_pct DESC;

-- ─────────────────────────────────────────────────────────────────
-- SECTION 7: TIME-SERIES ANALYSIS QUERIES
-- Demonstrates indexed time-series capabilities
-- ─────────────────────────────────────────────────────────────────

-- 7a. Hourly average moisture per plot (last 24 hours)
SELECT p.plot_name,
       DATE_FORMAT(sr.recorded_at, '%Y-%m-%d %H:00') AS hour_bucket,
       ROUND(AVG(sr.value), 2)                        AS avg_moisture,
       COUNT(*)                                        AS reading_count
FROM   sensor_reading sr
JOIN   sensor         s  ON s.sensor_id = sr.sensor_id
JOIN   plot           p  ON p.plot_id   = s.plot_id
WHERE  s.sensor_type  = 'moisture'
  AND  sr.is_valid    = 1
  AND  sr.recorded_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
GROUP  BY p.plot_name, hour_bucket
ORDER  BY p.plot_name, hour_bucket;

-- 7b. Daily irrigation summary
SELECT DATE(triggered_at)         AS irrigation_date,
       p.plot_name,
       COUNT(*)                    AS times_irrigated,
       SUM(water_amount_liters)    AS total_liters,
       SUM(duration_minutes)       AS total_minutes
FROM   irrigation_event ie
JOIN   plot             p ON p.plot_id = ie.plot_id
GROUP  BY DATE(triggered_at), p.plot_name
ORDER  BY irrigation_date DESC;

-- 7c. Trend: is moisture improving or degrading? (last 10 readings per plot)
SELECT p.plot_name,
       sr.value                   AS moisture_pct,
       sr.recorded_at,
       sr.value - LAG(sr.value) OVER (
           PARTITION BY s.plot_id
           ORDER BY sr.recorded_at
       )                          AS change_from_prev
FROM   sensor_reading sr
JOIN   sensor         s  ON s.sensor_id = sr.sensor_id
JOIN   plot           p  ON p.plot_id   = s.plot_id
WHERE  s.sensor_type = 'moisture'
  AND  sr.is_valid   = 1
ORDER  BY s.plot_id, sr.recorded_at DESC
LIMIT  30;

-- 7d. Find periods where crop was under moisture stress
SELECT p.plot_name, cs.stage_name,
       COUNT(*) AS stress_readings,
       MIN(sr.value) AS min_moisture,
       AVG(sr.value) AS avg_moisture,
       MIN(sr.recorded_at) AS stress_start,
       MAX(sr.recorded_at) AS stress_end
FROM   sensor_reading sr
JOIN   sensor         s  ON s.sensor_id  = sr.sensor_id
JOIN   plot           p  ON p.plot_id    = s.plot_id
JOIN   crop_stage     cs ON cs.stage_id  = sr.stage_id
WHERE  s.sensor_type = 'moisture'
  AND  sr.is_valid   = 1
  AND  sr.value < cs.irrigation_threshold_moisture
GROUP  BY p.plot_name, cs.stage_name;

-- ─────────────────────────────────────────────────────────────────
-- SECTION 8: STORED PROCEDURE DEEP TESTS
-- ─────────────────────────────────────────────────────────────────

-- 8a. Test all 6 sensors with realistic values
CALL record_sensor_reading(1, 58.0, @r); SELECT 'Sensor1 (moisture Plot-A1)' AS test, @r AS result;
CALL record_sensor_reading(2, 24.5, @r); SELECT 'Sensor2 (temp Plot-A1)'    AS test, @r AS result;
CALL record_sensor_reading(3, 72.0, @r); SELECT 'Sensor3 (moisture Plot-B2)' AS test, @r AS result;
CALL record_sensor_reading(4, 29.0, @r); SELECT 'Sensor4 (temp Plot-B2)'    AS test, @r AS result;
CALL record_sensor_reading(5, 45.0, @r); SELECT 'Sensor5 (moisture Plot-C3)' AS test, @r AS result;
CALL record_sensor_reading(6, 28.0, @r); SELECT 'Sensor6 (temp Plot-C3)'    AS test, @r AS result;

-- 8b. Test get_plot_summary procedure
CALL get_plot_summary(1);  -- Full summary for Plot-A1
CALL get_plot_summary(2);  -- Full summary for Plot-B2
CALL get_plot_summary(3);  -- Full summary for Plot-C3

-- 8c. Test duplicate irrigation prevention (2-hour lockout)
--     First call: should create irrigation_event
CALL record_sensor_reading(3, 15.0, @r); SELECT @r;
--     Second call within 2 hours: should NOT create duplicate event
CALL record_sensor_reading(3, 14.0, @r); SELECT @r;
--     Count: should be 1, not 2
SELECT COUNT(*) AS irrigation_count
FROM   irrigation_event
WHERE  plot_id     = 2   -- Plot-B2
  AND  triggered_at >= DATE_SUB(NOW(), INTERVAL 2 HOUR);

-- ─────────────────────────────────────────────────────────────────
-- SECTION 9: RESOLVE + COMPLETE WORKFLOW TEST
-- ─────────────────────────────────────────────────────────────────

-- 9a. Resolve the oldest unresolved alert
SET @oldest_alert = (
    SELECT alert_id FROM alert
    WHERE  is_resolved = 0
    ORDER  BY created_at ASC
    LIMIT  1
);
CALL resolve_alert(@oldest_alert);
SELECT alert_id, is_resolved, resolved_at
FROM   alert WHERE alert_id = @oldest_alert;

-- 9b. Complete the oldest triggered irrigation event
SET @oldest_irr = (
    SELECT event_id FROM irrigation_event
    WHERE  status = 'triggered'
    ORDER  BY triggered_at ASC
    LIMIT  1
);
CALL complete_irrigation(@oldest_irr);
SELECT event_id, status FROM irrigation_event WHERE event_id = @oldest_irr;

-- ─────────────────────────────────────────────────────────────────
-- SECTION 10: FINAL SUMMARY REPORT
-- ─────────────────────────────────────────────────────────────────
SELECT '=== SYSTEM SUMMARY REPORT ===' AS report_header;

SELECT
    (SELECT COUNT(*) FROM sensor_reading)                          AS total_readings,
    (SELECT COUNT(*) FROM sensor_reading WHERE is_valid = 0)       AS invalid_readings,
    (SELECT COUNT(*) FROM alert)                                   AS total_alerts,
    (SELECT COUNT(*) FROM alert WHERE is_resolved = 0)             AS unresolved_alerts,
    (SELECT COUNT(*) FROM alert WHERE severity = 'CRITICAL')       AS critical_alerts,
    (SELECT COUNT(*) FROM irrigation_event)                        AS irrigation_events,
    (SELECT COUNT(*) FROM irrigation_event WHERE status='completed')AS completed_irrigations,
    (SELECT ROUND(SUM(water_amount_liters),1) FROM irrigation_event) AS total_water_liters,
    (SELECT MIN(trust_score) FROM sensor WHERE is_active = 1)      AS lowest_sensor_trust;
