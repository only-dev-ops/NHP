# Real-Time Internet Outage Map

A real-time BGP monitoring application that detects and correlates Internet outages globally using live BGP updates from the [RIPE RIS Live stream](https://ris-live.ripe.net/). Built in Java with Spring Boot, the system captures prefix-level visibility, identifies global withdrawals, detects recoveries, and correlates outages at the ASN level.

---

## Features

- Real-time WebSocket ingestion of BGP UPDATEs (RIPE RIS Live)
- Live visibility tracking per prefix across global collectors
- Outage detection when a prefix becomes globally unreachable
- Recovery detection when a withdrawn prefix reappears
- Correlation of prefix outages into ASN-level outage events
- Prometheus metrics exposure for monitoring dashboards
- REST API for frontend dashboard queries

## System Components

### 1. **RipeStreamClient**

- Connects to `wss://ris-live.ripe.net/v1/stream`
- Subscribes to `updates`
- Deserializes incoming BGP UPDATE messages
- Publishes messages to the `UpdateProcessor`

---

### 2. **UpdateProcessor**

- Maintains per-prefix state in Redis
- Handles announcements and withdrawals
- Detects:
  - Global withdrawals → triggers `outage_start`
  - Re-announcements → triggers `recovery`
- Updates corresponding events in PostgreSQL

---

### 3. **Redis (Prefix Visibility Store)**

- Stores peer visibility for each prefix
- TTL-based sliding cache to evict stale, stable prefixes
- Key format: `prefix:{CIDR}`
- Value: set of collectors, last path, outage state

---

### 4. **outage_events Table**

Stores per-prefix events:
| Column | Type | Description |
|----------------|-------------|-----------------------------------------|
| `prefix` | CIDR | BGP prefix (`203.0.113.0/24`) |
| `origin_asn` | INTEGER | ASN that originated the prefix |
| `timestamp` | TIMESTAMP | Event time |
| `event_type` | VARCHAR | `outage_start`, `recovery` |
| `last_path` | TEXT | Last known AS path before event |
| `withdrawn_by` | TEXT[] | Collectors reporting withdrawals |
| `resolved_at` | TIMESTAMP | If recovery, time the prefix reappeared |
| `duration` | INTERVAL | Duration of outage (if recovered) |

---

### 5. **AsnOutageCorrelator**

- Tracks prefix outages grouped by ASN
- Uses in-memory map with sliding timeout to form outage windows
- When timeout ends or all prefixes recover:
  - Writes ASN-level outage to `asn_outages`

---

### 6. **asn_outages Table**

Stores grouped ASN-wide outage incidents:
| Column | Type | Description |
|----------------|-------------|---------------------------------------|
| `asn` | INTEGER | ASN affected |
| `start_time` | TIMESTAMP | First prefix outage in group |
| `end_time` | TIMESTAMP | Recovery or timer expiry |
| `duration` | INTERVAL | Total duration of the outage |
| `prefixes` | TEXT[] | List of affected prefixes |
| `severity` | INTEGER | Percentage of total ASN prefixes lost |
| `country` | TEXT | Inferred country for ASN |

---

### 7. **Metrics (Micrometer + Prometheus)**

Exposes:

- Active outages
- Prefix withdrawal rate
- Recovery lag
- ASN flap count

Scraped at `/actuator/prometheus`

---

### 8. **REST API (Spring Web)**

Example endpoints:

- `GET /outages/recent`
- `GET /asn/{asn}/events`
- `GET /prefix/{prefix}/history`
- `GET /stats/summary`

---

## Prefix and Outage detection flow

1. **Announce arrives**

   - Add peer to prefix in Redis
   - Possibly detect recovery if prefix was in withdrawn state

2. **Withdraw arrives**

   - Remove peer from prefix’s visibility set
   - If set becomes empty → trigger outage

3. **Prefix state TTL expires**

   - If no activity → prefix evicted silently

4. **Recovery**

   - Prefix is announced again after outage
   - Trigger `recovery` event + update outage duration

5. **ASN correlation**
   - Group multiple prefix outages into ASN-wide events
   - Time-based or threshold-based batching

---

## Technologies

- **Java 17** / **Spring Boot 3**
- Gradle (build management)
- Docker (service containerization)
- **Redis** (prefix visibility tracking)
- **PostgreSQL / TimescaleDB** (event persistence)
- **Micrometer + Prometheus** (metrics)
- **Spring Web / Spring WebSocket** (API + ingest)

---

## Setup

