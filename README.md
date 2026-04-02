# 🌾 IoT-Driven Crop Stage Aware Smart Farming System

A **DBMS-centric** full-stack project demonstrating:
- Relational database design (3NF normalization)
- Stored procedures, triggers, and views as the **decision engine**
- Java Servlet + JDBC backend (no Spring, no ORM)
- IoT sensor simulation via Python (and real ESP32)
- Vanilla HTML/JS frontend with live auto-refresh

---

## 📐 System Architecture

```
ESP32 / Python Simulator
         │
         │  POST /api/sensor-data
         │  {"sensorId": 1, "value": 42.5}
         ▼
┌────────────────────────┐
│  Java Servlet (JDBC)   │  ← Transport only, zero business logic
│  SensorDataServlet     │
└────────────┬───────────┘
             │  CALL record_sensor_reading(sensor_id, value, OUT result)
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                  MySQL 8.0 — THE DECISION ENGINE                │
│                                                                 │
│  Stored Procedure: record_sensor_reading()                      │
│    1. Validate reading (physical range check)                   │
│    2. Detect crop stage via planting_date + crop_stage table    │
│    3. INSERT into sensor_reading                                │
│    4. UPDATE plot.current_stage_id                              │
│    5. If moisture < threshold → INSERT irrigation_event         │
│    6. Generate appropriate alert (LOW_MOISTURE / HIGH_TEMP …)  │
│                                                                 │
│  Triggers:                                                      │
│    trg_update_trust_score      → degrades sensor on bad reads   │
│    trg_auto_resolve_moisture_ok → cleans stale alerts          │
│    trg_irrigation_water_calc   → auto-calc water volume        │
│                                                                 │
│  Views (read-only for frontend):                                │
│    alert_view             irrigation_history_view               │
│    plot_status_view        sensor_health_view                   │
└───────────────────────────┬─────────────────────────────────────┘
                            │  SELECT from views
                            ▼
               Java DAO → Service → Servlet
                            │
                            │  GET /api/alerts
                            │  GET /api/plot-status
                            │  GET /api/irrigation-history
                            ▼
              Vanilla HTML/JS Dashboard (read-only)
                  Auto-refresh every 8 seconds
```

---

## 🗄️ Database Design

### Entity-Relationship Summary

| Table              | Purpose                                      | Key Relations                  |
|--------------------|----------------------------------------------|--------------------------------|
| `crop`             | Crop types (Wheat, Rice, Cotton)             | PK: crop_id                    |
| `crop_stage`       | Growth stages per crop with thresholds       | FK → crop                      |
| `plot`             | Physical farm plots with assigned crop       | FK → crop, crop_stage          |
| `sensor`           | IoT sensors attached to plots                | FK → plot                      |
| `sensor_reading`   | Time-series readings (indexed by time)       | FK → sensor, crop_stage        |
| `irrigation_event` | Auto-triggered irrigation log                | FK → plot, sensor_reading      |
| `alert`            | System-generated alerts                      | FK → plot, sensor              |

### Normalization
- **1NF**: All columns atomic, no repeating groups
- **2NF**: No partial dependencies (all tables have single-column PKs)
- **3NF**: No transitive dependencies (crop stage thresholds are in `crop_stage`, not `plot`)

---

## 📁 Project Structure

```
smart-farming/
├── database/
│   ├── schema.sql        ← Tables + seed data
│   ├── procedures.sql    ← Stored procedures (ALL business logic)
│   ├── triggers.sql      ← trust_score, auto-resolve, water calc
│   └── views.sql         ← alert_view, plot_status_view, etc.
│
├── backend/
│   ├── pom.xml           ← Maven build (Servlet API + MySQL JDBC)
│   └── src/main/
│       ├── java/com/smartfarming/
│       │   ├── db/
│       │   │   └── DBConnection.java        ← JDBC singleton
│       │   ├── model/
│       │   │   ├── SensorReadable.java      ← Interface (OOP)
│       │   │   ├── Sensor.java              ← Abstract base class
│       │   │   ├── MoistureSensor.java      ← Subclass (polymorphism)
│       │   │   ├── TemperatureSensor.java   ← Subclass
│       │   │   ├── Alert.java               ← POJO
│       │   │   ├── PlotStatus.java          ← POJO
│       │   │   └── IrrigationHistory.java   ← POJO
│       │   ├── dao/
│       │   │   ├── SensorDAO.java           ← Calls stored procedure
│       │   │   ├── AlertDAO.java            ← Queries alert_view
│       │   │   ├── PlotDAO.java             ← Queries plot_status_view
│       │   │   └── IrrigationDAO.java       ← Queries irrigation_history_view
│       │   ├── service/
│       │   │   ├── SensorService.java       ← Thin wrapper
│       │   │   ├── AlertService.java
│       │   │   ├── PlotService.java
│       │   │   └── IrrigationService.java
│       │   └── controller/
│       │       ├── JsonUtil.java            ← Manual JSON builder
│       │       ├── SensorDataServlet.java   ← POST /api/sensor-data
│       │       ├── AlertServlet.java        ← GET  /api/alerts
│       │       ├── PlotStatusServlet.java   ← GET  /api/plot-status
│       │       └── IrrigationHistoryServlet.java ← GET /api/irrigation-history
│       └── webapp/WEB-INF/
│           └── web.xml                      ← Servlet mappings
│
├── simulator/
│   └── sensor_simulator.py  ← Python IoT simulator
│
└── frontend/
    └── index.html           ← Vanilla HTML/JS dashboard
```

---

## ⚙️ Setup Instructions

### Prerequisites

| Tool          | Version    | Download                          |
|---------------|------------|-----------------------------------|
| MySQL Server  | 8.0+       | https://dev.mysql.com/downloads/  |
| Apache Tomcat | 9.x / 10.x | https://tomcat.apache.org/        |
| JDK           | 11+        | https://adoptium.net/             |
| Maven         | 3.8+       | https://maven.apache.org/         |
| Python        | 3.8+       | https://python.org/               |

---

### Step 1: Database Setup

```bash
# Log into MySQL
mysql -u root -p

# Run scripts in order:
source /path/to/smart-farming/database/schema.sql
source /path/to/smart-farming/database/procedures.sql
source /path/to/smart-farming/database/triggers.sql
source /path/to/smart-farming/database/views.sql
```

Verify:
```sql
USE smart_farming;
SHOW TABLES;
-- Expected: 7 tables + 4 views

SELECT * FROM crop;
SELECT * FROM plot;
SELECT * FROM sensor;
```

---

### Step 2: Configure DB Password

Edit `DBConnection.java`:
```java
private static final String PASSWORD = "your_mysql_password";
```

---

### Step 3: Build the Java Backend

```bash
cd smart-farming/backend
mvn clean package
# Creates: target/smart-farming.war
```

---

### Step 4: Deploy to Tomcat

```bash
# Copy WAR to Tomcat's webapps directory
cp target/smart-farming.war /opt/tomcat/webapps/

# Start Tomcat
/opt/tomcat/bin/startup.sh

# Verify deployment
curl http://localhost:8080/smart-farming/api/plot-status
```

---

### Step 5: Start the Sensor Simulator

```bash
cd smart-farming/simulator
pip install requests
python3 sensor_simulator.py

# Or with custom settings:
python3 sensor_simulator.py --interval 5 --fault-rate 0.15
```

---

### Step 6: Open the Dashboard

Open `smart-farming/frontend/index.html` in a browser.  
Data auto-refreshes every 8 seconds from the Java APIs.

---

## 🔌 API Reference

### POST /api/sensor-data
Send a sensor reading (called by Python simulator or ESP32).

**Request:**
```json
{ "sensorId": 1, "value": 42.5 }
```

**Response:**
```json
{
  "status": "success",
  "message": "OK: reading_id=47 | stage=Tillering | valid=1 | type=moisture | value=42.5"
}
```

---

### GET /api/alerts
```
GET /api/alerts                    → latest 100 alerts
GET /api/alerts?unresolved=true    → only active alerts
GET /api/alerts?limit=20           → limit results
POST /api/alerts?action=resolve&id=5  → resolve alert #5
```

---

### GET /api/plot-status
```
GET /api/plot-status       → all plots with live sensor data
GET /api/plot-status?id=1  → single plot
```

---

### GET /api/irrigation-history
```
GET /api/irrigation-history            → last 50 events
GET /api/irrigation-history?plotId=1   → filter by plot
GET /api/irrigation-history?limit=10   → limit
POST /api/irrigation-history?action=complete&id=3  → mark completed
```

---

## 🧪 Manual Testing

### Test 1: Normal reading (should generate INFO alert)
```bash
curl -X POST http://localhost:8080/smart-farming/api/sensor-data \
  -H "Content-Type: application/json" \
  -d '{"sensorId": 1, "value": 65.0}'
```

### Test 2: Low moisture (should trigger irrigation + CRITICAL alert)
```bash
curl -X POST http://localhost:8080/smart-farming/api/sensor-data \
  -H "Content-Type: application/json" \
  -d '{"sensorId": 1, "value": 25.0}'
```

### Test 3: Faulty reading (should trigger trust score deduction via TRIGGER)
```bash
curl -X POST http://localhost:8080/smart-farming/api/sensor-data \
  -H "Content-Type: application/json" \
  -d '{"sensorId": 1, "value": 150.0}'
```

### Test 4: High temperature (should trigger WARNING alert)
```bash
curl -X POST http://localhost:8080/smart-farming/api/sensor-data \
  -H "Content-Type: application/json" \
  -d '{"sensorId": 2, "value": 38.0}'
```

### Test 5: Verify trust score was reduced in DB
```sql
SELECT sensor_id, trust_score, is_active FROM sensor;
```

### Test 6: Check alerts were generated
```sql
SELECT * FROM alert_view LIMIT 10;
```

### Test 7: Check irrigation was triggered
```sql
SELECT * FROM irrigation_history_view;
```

---

## 📡 ESP32 Integration Guide

The ESP32 must send the same JSON payload the Python simulator sends.

```cpp
// Arduino/ESP32 sketch (pseudocode)
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

const char* ssid     = "YOUR_WIFI";
const char* password = "YOUR_PASSWORD";
const char* serverUrl = "http://192.168.1.100:8080/smart-farming/api/sensor-data";

// Sensor IDs must match sensor.sensor_id in MySQL
const int MOISTURE_SENSOR_ID    = 1;
const int TEMPERATURE_SENSOR_ID = 2;

void sendReading(int sensorId, float value) {
  HTTPClient http;
  http.begin(serverUrl);
  http.addHeader("Content-Type", "application/json");

  StaticJsonDocument<100> doc;
  doc["sensorId"] = sensorId;
  doc["value"]    = value;

  String payload;
  serializeJson(doc, payload);

  int httpCode = http.POST(payload);
  String response = http.getString();
  Serial.println("Response: " + response);
  http.end();
}

void loop() {
  float moisture    = readMoistureSensor();     // your sensor read logic
  float temperature = readTemperatureSensor();  // DHT22 or DS18B20

  sendReading(MOISTURE_SENSOR_ID,    moisture);
  sendReading(TEMPERATURE_SENSOR_ID, temperature);

  delay(10000);  // every 10 seconds
}
```

---

## 🎓 OOP Concepts Summary

| Concept         | Where                                                              |
|-----------------|--------------------------------------------------------------------|
| **Interface**   | `SensorReadable` — contract for `readValue()`, `getUnit()`, etc.  |
| **Inheritance** | `MoistureSensor`, `TemperatureSensor` extend abstract `Sensor`    |
| **Encapsulation**| All fields `private`, exposed only via getters/setters            |
| **Polymorphism**| `getUnit()`, `isReadingValid()`, `readValue()` overridden per type|
| **Abstraction** | `Sensor` is abstract — cannot be instantiated directly            |

---

## 🗃️ DBMS Concepts Checklist

| Concept               | Implemented Where                                              |
|-----------------------|----------------------------------------------------------------|
| Normalization (1–3NF) | `crop`, `crop_stage`, `plot`, `sensor` — no redundancy        |
| Primary Keys          | Every table has `AUTO_INCREMENT` PK                           |
| Foreign Keys          | Cascading constraints across all tables                        |
| Stored Procedure      | `record_sensor_reading()` — entire business logic             |
| Triggers              | `trg_update_trust_score`, `trg_auto_resolve_moisture_ok`      |
| Views                 | `alert_view`, `plot_status_view`, `irrigation_history_view`   |
| Time-series Indexing  | `INDEX idx_sensor_time(sensor_id, recorded_at)` in readings   |
| Transaction Safety    | `START TRANSACTION / COMMIT / ROLLBACK` in procedure          |
| OUT parameter         | Stored procedure returns result message to Java via OUT param  |
| Enum columns          | `sensor.sensor_type`, `alert.severity`, `plot.status`         |

---

## ✅ Viva Tips

**Q: Why is all logic in the database?**
A: It guarantees consistency regardless of which application layer calls the procedure. If logic were in Java, two different clients could make conflicting decisions about irrigation.

**Q: Why use a stored procedure instead of SQL in Java?**
A: Security (parameterized), performance (compiled + cached execution plan), atomicity (wrapped in transaction), and separation of concerns.

**Q: What happens when a sensor sends a bad reading?**
A: The stored procedure sets `is_valid=0`, the AFTER INSERT trigger fires on `sensor_reading`, reduces `trust_score` by 5%, and if trust drops below 20% the sensor is auto-deactivated with a CRITICAL alert — all without any Java code.

**Q: How does crop stage detection work?**
A: The procedure calculates `DATEDIFF(CURDATE(), planting_date)` to get days since planting, then joins `crop_stage` where `days BETWEEN start_day AND end_day` — pure SQL, no Java.
