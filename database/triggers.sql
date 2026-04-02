-- =============================================================
-- FILE: triggers.sql
-- PURPOSE: Automatic trust_score degradation on invalid readings
--          All logic is inside the DB, not Java.
-- =============================================================

USE smart_farming;

DELIMITER $$

-- =============================================================
-- TRIGGER: trg_sensor_reading_after_insert
-- FIRES: AFTER INSERT on sensor_reading
-- PURPOSE:
--   - If reading is NOT valid → decrement sensor trust_score by 10
--   - If trust_score hits 0 → mark sensor as inactive
--   - If trust_score drops below 30 → (alert is created in procedure)
-- WHY TRIGGER (not procedure): The procedure inserts the reading,
-- so the trigger fires automatically — no extra Java code needed.
-- =============================================================
DROP TRIGGER IF EXISTS trg_sensor_reading_after_insert$$

CREATE TRIGGER trg_sensor_reading_after_insert
AFTER INSERT ON sensor_reading
FOR EACH ROW
BEGIN
    IF NEW.is_valid = FALSE THEN
        -- Decrement trust score by 10 (min 0)
        UPDATE sensor
        SET trust_score = GREATEST(trust_score - 10, 0)
        WHERE sensor_id = NEW.sensor_id;

        -- If trust score reaches 0, deactivate sensor
        UPDATE sensor
        SET is_active = FALSE
        WHERE sensor_id = NEW.sensor_id
          AND trust_score = 0;

    ELSE
        -- Valid reading: slowly recover trust (max 100) by 1 point
        UPDATE sensor
        SET trust_score = LEAST(trust_score + 1, 100)
        WHERE sensor_id = NEW.sensor_id;
    END IF;
END$$

-- =============================================================
-- TRIGGER: trg_irrigation_event_after_insert
-- FIRES: AFTER INSERT on irrigation_event
-- PURPOSE: Auto-insert an INFO alert acknowledging irrigation
--          was triggered. Keeps alert table as audit trail.
-- =============================================================
DROP TRIGGER IF EXISTS trg_irrigation_event_after_insert$$

CREATE TRIGGER trg_irrigation_event_after_insert
AFTER INSERT ON irrigation_event
FOR EACH ROW
BEGIN
    INSERT INTO alert (plot_id, sensor_id, alert_type, message, severity)
    VALUES (
        NEW.plot_id,
        NULL,
        'IRRIGATION_TRIGGERED',
        CONCAT('Irrigation started on Plot ', NEW.plot_id,
               ' for ', NEW.duration_minutes, ' minutes. Reason: ', NEW.trigger_reason),
        'INFO'
    );
END$$

-- =============================================================
-- TRIGGER: trg_plot_stage_change
-- FIRES: AFTER UPDATE on plot
-- PURPOSE: When crop stage changes, log an INFO alert as audit
-- =============================================================
DROP TRIGGER IF EXISTS trg_plot_stage_change$$


CREATE TRIGGER trg_plot_stage_change
AFTER UPDATE ON plot
FOR EACH ROW
BEGIN
    IF NOT (OLD.current_stage_id <=> NEW.current_stage_id)
       AND NEW.current_stage_id IS NOT NULL THEN

        INSERT INTO alert (plot_id, sensor_id, alert_type, message, severity)
        VALUES (
            NEW.plot_id,
            NULL,
            'STAGE_TRANSITION',
            CONCAT('Plot ', NEW.plot_name,
                   ' has entered a new crop growth stage (Stage ID: ',
                   NEW.current_stage_id,
                   '). Thresholds updated automatically.'),
            'INFO'
        );
    END IF;
END$$

DELIMITER ;
