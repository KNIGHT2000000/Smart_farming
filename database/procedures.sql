-- =============================================================
-- FILE: procedures.sql
-- PURPOSE: ALL business logic lives here in the database.
--          Java backend only calls these procedures.
-- =============================================================

USE smart_farming;

DELIMITER $$

-- =============================================================
-- PROCEDURE: record_sensor_reading
-- Called by: Java SensorDataServlet via JDBC
--
-- STEPS INSIDE THIS PROCEDURE (all business logic):
--   1. Determine valid range based on sensor type
--   2. Insert reading with is_valid flag
--   3. Update sensor last_reading_at
--   4. If invalid → trigger will decrement trust_score (see triggers.sql)
--   5. If valid moisture sensor → determine current crop stage
--   6. Update plot.current_stage_id based on days since planting
--   7. Check moisture vs stage thresholds → trigger irrigation
--   8. Check temperature vs stage thresholds → raise alert
--   9. Raise CRITICAL alert if sensor trust_score < 30
-- =============================================================
DROP PROCEDURE IF EXISTS record_sensor_reading$$

CREATE PROCEDURE record_sensor_reading(
    IN  p_sensor_id INT,
    IN  p_value     DECIMAL(8,3),
    OUT p_status    VARCHAR(100)
)
BEGIN
    -- Declare variables
    DECLARE v_sensor_type      ENUM('MOISTURE','TEMPERATURE','HUMIDITY','PH');
    DECLARE v_plot_id          INT;
    DECLARE v_trust_score      INT;
    DECLARE v_is_valid         BOOLEAN DEFAULT TRUE;

    DECLARE v_crop_id          INT;
    DECLARE v_planted_date     DATE;
    DECLARE v_days_since_plant INT;

    DECLARE v_stage_id         INT     DEFAULT NULL;
    DECLARE v_stage_name       VARCHAR(100);
    DECLARE v_min_moisture     DECIMAL(5,2);
    DECLARE v_max_moisture     DECIMAL(5,2);
    DECLARE v_min_temp         DECIMAL(5,2);
    DECLARE v_max_temp         DECIMAL(5,2);
    DECLARE v_irr_duration     INT;

    DECLARE v_reading_id       BIGINT;
    DECLARE v_alert_msg        TEXT;

    -- Error handler
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        SET p_status = 'ERROR: Database exception in record_sensor_reading';
        ROLLBACK;
    END;

    START TRANSACTION;

    -- -------------------------------------------------------
    -- STEP 1: Fetch sensor metadata
    -- -------------------------------------------------------
    SELECT sensor_type, plot_id, trust_score
    INTO   v_sensor_type, v_plot_id, v_trust_score
    FROM   sensor
    WHERE  sensor_id = p_sensor_id AND is_active = TRUE;

    IF v_plot_id IS NULL THEN
        SET p_status = 'ERROR: Sensor not found or inactive';
        ROLLBACK;
        LEAVE record_sensor_reading;  -- exit procedure
    END IF;

    -- -------------------------------------------------------
    -- STEP 2: Validate reading range by sensor type
    --         Physical impossibility check
    -- -------------------------------------------------------
    SET v_is_valid = CASE v_sensor_type
        WHEN 'MOISTURE'    THEN (p_value BETWEEN 0 AND 100)
        WHEN 'TEMPERATURE' THEN (p_value BETWEEN -10 AND 60)
        WHEN 'HUMIDITY'    THEN (p_value BETWEEN 0 AND 100)
        WHEN 'PH'          THEN (p_value BETWEEN 0 AND 14)
        ELSE FALSE
    END;

    -- -------------------------------------------------------
    -- STEP 3: Insert sensor reading
    -- -------------------------------------------------------
    INSERT INTO sensor_reading (sensor_id, value, recorded_at, is_valid)
    VALUES (p_sensor_id, p_value, NOW(), v_is_valid);

    SET v_reading_id = LAST_INSERT_ID();

    -- Update sensor's last_reading_at
    UPDATE sensor SET last_reading_at = NOW() WHERE sensor_id = p_sensor_id;

    -- -------------------------------------------------------
    -- STEP 4: If reading is INVALID → raise faulty sensor alert
    --         (trigger will separately decrement trust_score)
    -- -------------------------------------------------------
    IF v_is_valid = FALSE THEN
        INSERT INTO alert (plot_id, sensor_id, alert_type, message, severity)
        VALUES (
            v_plot_id,
            p_sensor_id,
            'FAULTY_SENSOR',
            CONCAT('Sensor ', p_sensor_id, ' sent invalid value: ', p_value,
                   '. Type: ', v_sensor_type),
            'WARNING'
        );
        SET p_status = CONCAT('WARNING: Invalid reading recorded. Sensor ID=', p_sensor_id);
        COMMIT;
        LEAVE record_sensor_reading;
    END IF;

    -- -------------------------------------------------------
    -- STEP 5: Alert if sensor trust_score is critically low
    -- -------------------------------------------------------
    IF v_trust_score < 30 THEN
        -- Check if we haven't already created this alert in last hour
        IF NOT EXISTS (
            SELECT 1 FROM alert
            WHERE sensor_id = p_sensor_id
              AND alert_type = 'LOW_TRUST_SENSOR'
              AND created_at > NOW() - INTERVAL 1 HOUR
        ) THEN
            INSERT INTO alert (plot_id, sensor_id, alert_type, message, severity)
            VALUES (
                v_plot_id, p_sensor_id, 'LOW_TRUST_SENSOR',
                CONCAT('Sensor ', p_sensor_id, ' trust score is critically low: ', v_trust_score,
                       '%. Consider replacing sensor.'),
                'CRITICAL'
            );
        END IF;
    END IF;

    -- -------------------------------------------------------
    -- STEP 6: Determine current crop stage for this plot
    --         based on DATEDIFF(NOW(), planted_date)
    -- -------------------------------------------------------
    SELECT p.crop_id, p.planted_date
    INTO   v_crop_id, v_planted_date
    FROM   plot p
    WHERE  p.plot_id = v_plot_id;

    SET v_days_since_plant = DATEDIFF(CURDATE(), v_planted_date);

    -- Find the correct stage for today
    SELECT stage_id, stage_name,
           min_moisture_pct, max_moisture_pct,
           min_temp_celsius, max_temp_celsius,
           irrigation_duration_min
    INTO   v_stage_id, v_stage_name,
           v_min_moisture, v_max_moisture,
           v_min_temp, v_max_temp,
           v_irr_duration
    FROM   crop_stage
    WHERE  crop_id   = v_crop_id
      AND  v_days_since_plant BETWEEN start_day AND end_day
    LIMIT  1;

    -- -------------------------------------------------------
    -- STEP 7: Update plot.current_stage_id if stage changed
    -- -------------------------------------------------------
    IF v_stage_id IS NOT NULL THEN
        UPDATE plot
        SET    current_stage_id = v_stage_id
        WHERE  plot_id = v_plot_id
          AND  (current_stage_id IS NULL OR current_stage_id != v_stage_id);

        -- -------------------------------------------------------
        -- STEP 8: Moisture-based decisions (only for moisture sensor)
        -- -------------------------------------------------------
        IF v_sensor_type = 'MOISTURE' THEN

            -- 8a. Moisture too LOW → trigger irrigation + warning alert
            IF p_value < v_min_moisture THEN

                -- Only trigger if no irrigation in last 2 hours for this plot
                IF NOT EXISTS (
                    SELECT 1 FROM irrigation_event
                    WHERE plot_id = v_plot_id
                      AND triggered_at > NOW() - INTERVAL 2 HOUR
                ) THEN
                    INSERT INTO irrigation_event
                        (plot_id, duration_minutes, trigger_reason, moisture_value)
                    VALUES (
                        v_plot_id,
                        v_irr_duration,
                        CONCAT('LOW_MOISTURE_', UPPER(REPLACE(v_stage_name,' ','_')),
                               '_STAGE: ', p_value, '% < min ', v_min_moisture, '%'),
                        p_value
                    );
                END IF;

                SET v_alert_msg = CONCAT(
                    'Plot ', v_plot_id, ' [', v_stage_name, ' stage]: ',
                    'Moisture ', p_value, '% is BELOW minimum ', v_min_moisture, '%. ',
                    'Irrigation triggered for ', v_irr_duration, ' minutes.'
                );

                INSERT INTO alert (plot_id, sensor_id, alert_type, message, severity)
                VALUES (v_plot_id, p_sensor_id, 'LOW_MOISTURE', v_alert_msg, 'WARNING');

            -- 8b. Moisture too HIGH → alert only, no irrigation
            ELSEIF p_value > v_max_moisture THEN

                SET v_alert_msg = CONCAT(
                    'Plot ', v_plot_id, ' [', v_stage_name, ' stage]: ',
                    'Moisture ', p_value, '% EXCEEDS maximum ', v_max_moisture, '%. ',
                    'Risk of root rot. Stop irrigation.'
                );

                INSERT INTO alert (plot_id, sensor_id, alert_type, message, severity)
                VALUES (v_plot_id, p_sensor_id, 'HIGH_MOISTURE', v_alert_msg, 'WARNING');

            END IF;
        END IF;

        -- -------------------------------------------------------
        -- STEP 9: Temperature-based decisions (temp sensor)
        -- -------------------------------------------------------
        IF v_sensor_type = 'TEMPERATURE' THEN

            IF p_value < v_min_temp THEN
                SET v_alert_msg = CONCAT(
                    'Plot ', v_plot_id, ' [', v_stage_name, ' stage]: ',
                    'Temperature ', p_value, 'C is BELOW minimum ', v_min_temp, 'C. ',
                    'Frost risk possible. Take protective measures.'
                );
                INSERT INTO alert (plot_id, sensor_id, alert_type, message, severity)
                VALUES (v_plot_id, p_sensor_id, 'LOW_TEMPERATURE', v_alert_msg, 'CRITICAL');

            ELSEIF p_value > v_max_temp THEN
                SET v_alert_msg = CONCAT(
                    'Plot ', v_plot_id, ' [', v_stage_name, ' stage]: ',
                    'Temperature ', p_value, 'C EXCEEDS maximum ', v_max_temp, 'C. ',
                    'Heat stress risk. Consider shading or increased irrigation.'
                );
                INSERT INTO alert (plot_id, sensor_id, alert_type, message, severity)
                VALUES (v_plot_id, p_sensor_id, 'HIGH_TEMPERATURE', v_alert_msg, 'WARNING');
            END IF;

        END IF;

    ELSE
        -- Stage not found means crop cycle is complete or data issue
        INSERT INTO alert (plot_id, sensor_id, alert_type, message, severity)
        VALUES (
            v_plot_id, p_sensor_id, 'CROP_CYCLE_COMPLETE',
            CONCAT('Plot ', v_plot_id, ': Day ', v_days_since_plant,
                   ' - No active stage found. Crop may be ready for harvest.'),
            'INFO'
        );
    END IF;

    COMMIT;
    SET p_status = CONCAT('OK: Reading recorded. ReadingID=', v_reading_id,
                          ', Valid=', v_is_valid, ', Stage=', IFNULL(v_stage_name,'N/A'));

END$$

-- =============================================================
-- PROCEDURE: get_plot_dashboard
-- Returns a summary for a given plot (used by Java PlotDAO)
-- =============================================================
DROP PROCEDURE IF EXISTS get_plot_dashboard$$

CREATE PROCEDURE get_plot_dashboard(IN p_plot_id INT)
BEGIN
    -- Latest readings per sensor type for this plot
    SELECT
        s.sensor_type,
        sr.value,
        sr.recorded_at,
        s.trust_score,
        sr.is_valid
    FROM sensor s
    JOIN sensor_reading sr ON sr.reading_id = (
        SELECT reading_id FROM sensor_reading
        WHERE sensor_id = s.sensor_id
        ORDER BY recorded_at DESC LIMIT 1
    )
    WHERE s.plot_id = p_plot_id AND s.is_active = TRUE;
END$$

-- =============================================================
-- PROCEDURE: resolve_alert
-- Marks an alert as resolved
-- =============================================================
DROP PROCEDURE IF EXISTS resolve_alert$$

CREATE PROCEDURE resolve_alert(IN p_alert_id INT)
BEGIN
    UPDATE alert SET is_resolved = TRUE WHERE alert_id = p_alert_id;
END$$

DELIMITER ;
