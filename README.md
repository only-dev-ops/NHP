# Real-Time Internet Outage Map

A real-time BGP monitoring application that detects and correlates Internet outages globally using live BGP updates from the [RIPE RIS Live stream](https://ris-live.ripe.net/). Built in Java with Spring Boot, the system captures prefix-level visibility, identifies global withdrawals, detects recoveries, and correlates outages at the ASN level with real-time geolocation visualization.

## Features

- **Real-time WebSocket ingestion** of BGP UPDATEs (RIPE RIS Live)
- **Live visibility tracking** per prefix across global collectors
- **Outage detection** when a prefix becomes globally unreachable
- **Recovery detection** when a withdrawn prefix reappears
- **ASN-level correlation** of prefix outages into network-wide events
- **Real-time geolocation** with ASN information and organization names
- **Interactive web dashboard** with Leaflet.js map visualization
- **Prometheus metrics** exposure for monitoring dashboards
- **REST API** for frontend dashboard queries and external integrations
- **Redis caching** for performance optimization
- **TimescaleDB** for time-series data storage

## System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   RIPE RIS      │    │   Spring Boot   │    │   Frontend      │
│   Live Stream   │───▶│   Application   │───▶│   (Leaflet.js)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│     Redis       │    │   TimescaleDB   │    │   Prometheus    │
│  (Prefix State) │    │  (Event Store)  │    │   (Metrics)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## System Components

### 1. **RipeStreamClient**

- Connects to `wss://ris-live.ripe.net/v1/ws/`
- Subscribes to BGP UPDATE messages
- Deserializes incoming BGP UPDATE messages
- Publishes messages to the `UpdateProcessor`
- Automatic retry logic with exponential backoff
- Graceful shutdown handling

### 2. **UpdateProcessor**

- Maintains per-prefix state in Redis with in-memory caching
- Handles announcements and withdrawals
- Detects:
  - Global withdrawals → triggers `outage_start`
  - Re-announcements → triggers `recovery`
- Updates corresponding events in PostgreSQL
- Integrates with ASN correlation service
- Comprehensive error handling and metrics

### 3. **AsnGeolocationService**

- **Multi-API redundancy** for ASN information:
  - BGPView API (primary)
  - ASNLookup API (secondary)
  - WHOIS mapping (fallback)
- **Real geolocation data** with latitude/longitude coordinates
- **Organization names** (e.g., "Google LLC", "Facebook, Inc.")
- **Country information** for geographic context
- **Multi-level caching** (memory + Redis with 24-hour TTL)
- **Graceful fallbacks** when APIs are unavailable

### 4. **AsnOutageService**

- **Real-time correlation** of prefix outages into ASN-wide events
- **In-memory tracking** of active ASN outages with timeout management
- **Automatic closure** when all prefixes recover or timeout expires
- **Severity calculation** based on percentage of ASN prefixes affected
- **Scheduled cleanup** of timed-out outages every minute
- **Country information** integration via geolocation service

### 5. **Redis (Prefix Visibility Store)**

- Stores peer visibility for each prefix
- TTL-based sliding cache to evict stale, stable prefixes (24 hours)
- Key format: `prefix:{CIDR}`
- Value: set of collectors, last path, outage state
- In-memory caching layer for performance optimization
- JSON serialization for complex objects

### 6. **outage_events Table**

Stores per-prefix events:
| Column | Type | Description |
|----------------|-------------|-----------------------------------------|
| `id` | BIGSERIAL | Primary key |
| `prefix` | CIDR | BGP prefix (`203.0.113.0/24`) |
| `origin_asn` | INTEGER | ASN that originated the prefix |
| `timestamp` | TIMESTAMPTZ | Event time |
| `event_type` | VARCHAR | `outage_start`, `recovery` |
| `last_path` | TEXT | Last known AS path before event |
| `withdrawn_by` | TEXT[] | Collectors reporting withdrawals |
| `resolved_at` | TIMESTAMPTZ | If recovery, time the prefix reappeared |
| `duration` | INTERVAL | Duration of outage (if recovered) |
| `created_at` | TIMESTAMPTZ | Record creation time |

### 7. **asn_outages Table**

Stores grouped ASN-wide outage incidents:
| Column | Type | Description |
|----------------|-------------|---------------------------------------|
| `id` | BIGSERIAL | Primary key |
| `asn` | INTEGER | ASN affected |
| `start_time` | TIMESTAMPTZ | First prefix outage in group |
| `end_time` | TIMESTAMPTZ | Recovery or timer expiry |
| `duration` | INTERVAL | Total duration of the outage |
| `prefixes` | TEXT[] | List of affected prefixes |
| `severity` | INTEGER | Percentage of total ASN prefixes lost |
| `country` | TEXT | Country for ASN (from geolocation) |
| `created_at` | TIMESTAMPTZ | Record creation time |

### 8. **REST API Endpoints**

#### Core Endpoints

- `GET /api/v1/outages/recent?limit=50` - Recent outage events
- `GET /api/v1/outages/active` - Currently active outages
- `GET /api/v1/outages/map?hours=24` - Data for map visualization

#### ASN Endpoints

- `GET /api/v1/asn/{asn}/events` - Events for specific ASN
- `GET /api/v1/asn/{asn}/outages` - ASN-level outage correlations
- `GET /api/v1/asn/{asn}/info` - ASN information and geolocation

#### Prefix Endpoints

- `GET /api/v1/prefix/{prefix}/history` - Prefix outage history

#### Statistics

- `GET /api/v1/stats/summary` - Summary statistics
- `GET /api/v1/health` - Health check

### 9. **Frontend Dashboard**

#### Interactive Map Features

- **Real-time visualization** using Leaflet.js
- **Color-coded markers**:
  - 🔴 Red: Active outages
  - 🟢 Green: Recovered outages
  - 🟡 Yellow: Mixed status (both active and recovered)
- **Interactive popups** with ASN details:
  - Organization name
  - ASN number
  - Country information
  - Outage count and status
  - Affected prefixes
- **Auto-refresh** every 30 seconds
- **Legend** for map interpretation

#### Dashboard Components

- **Live statistics** (active outages, total outages, affected ASNs, avg duration)
- **Recent outages list** with clickable items
- **Responsive design** for mobile/desktop
- **Real-time updates** with geolocation data

### 10. **Metrics (Micrometer + Prometheus)**

Exposes comprehensive metrics:

- `ripe.bgp.messages.received` - BGP messages received
- `ripe.bgp.messages.processed` - BGP messages processed
- `ripe.bgp.processing.errors` - Processing errors
- `ripe.prefix.outages` - Prefix outages detected
- `ripe.prefix.recoveries` - Prefix recoveries detected
- `ripe.stream.restarts` - Stream restart count
- `ripe.websocket.errors` - WebSocket errors

Scraped at `/actuator/prometheus`

---

## Prefix and Outage Detection Flow

1. **BGP Announcement Arrives**

   - Add collector to prefix visibility set in Redis
   - Update last seen timestamp and AS path
   - If prefix was previously withdrawn → trigger recovery event
   - Process for ASN correlation

2. **BGP Withdrawal Arrives**

   - Remove collector from prefix visibility set
   - Add collector to withdrawn_by set
   - If visibility set becomes empty → trigger outage event
   - Process for ASN correlation

3. **ASN Correlation**

   - Group multiple prefix outages by ASN
   - Track affected prefixes and duration
   - 5-minute timeout window for correlation
   - Calculate severity percentage
   - Persist to `asn_outages` table

4. **Recovery Detection**

   - Prefix is announced again after outage
   - Trigger recovery event + update outage duration
   - Remove from active ASN outage tracking

5. **Geolocation Integration**
   - Fetch ASN information from multiple APIs
   - Cache results in Redis and memory
   - Provide real coordinates for map visualization

## Technologies

### Backend

- **Java 17** / **Spring Boot 3.2.2**
- **Spring WebFlux** (reactive programming)
- **Spring Data JPA** (database access)
- **Spring Data Redis** (caching)
- **Gradle** (build management)
- **TimescaleDB** (time-series database)
- **PostgreSQL** (relational database)
- **Redis** (in-memory data store)
- **Micrometer + Prometheus** (metrics)

### Frontend

- **Leaflet.js** (interactive maps)
- **Bootstrap 5** (responsive UI)
- **Vanilla JavaScript** (ES6+)
- **Font Awesome** (icons)

### Infrastructure

- **Docker** (containerization)
- **Docker Compose** (service orchestration)
- **Prometheus** (metrics collection)
- **Grafana** (monitoring dashboards)

## Quick Start

### Prerequisites

- Docker and Docker Compose
- At least 4GB RAM available
- Internet connection for BGP stream

### 1. Clone and Setup

```bash
git clone <repository-url>
cd NHP
```

### 2. Start Services

```bash
docker-compose up -d
```

This starts:

- **Application** (port 8080)
- **Redis** (port 6379)
- **TimescaleDB** (port 5432)
- **Prometheus** (port 9090)
- **Grafana** (port 3000)

### 3. Access Services

- **Dashboard**: http://localhost:8080
- **API Documentation**: http://localhost:8080/api/v1/health
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

### 4. Monitor Logs

```bash
docker-compose logs -f app
```

## Database Schema

The database schema is automatically initialized via `init-db.sql`:

- TimescaleDB extension enabled
- Hypertables for time-series optimization
- Indexes for query performance
- Triggers for automatic duration calculation

## API Examples

### Get Recent Outages

```bash
curl http://localhost:8080/api/v1/outages/recent?limit=10
```

### Get ASN Information

```bash
curl http://localhost:8080/api/v1/asn/15169/info
```

### Get Summary Statistics

```bash
curl http://localhost:8080/api/v1/stats/summary
```

### Get Map Data

```bash
curl http://localhost:8080/api/v1/outages/map?hours=24
```

## Monitoring

### Prometheus Metrics

Key metrics to monitor:

- `ripe_bgp_messages_received_total` - Message ingestion rate
- `ripe_prefix_outages_total` - Outage detection rate
- `ripe_prefix_recoveries_total` - Recovery detection rate
- `jvm_memory_used_bytes` - Memory usage
- `process_cpu_usage` - CPU usage

### Grafana Dashboards

Import the following dashboards:

- **System Overview** - Application health and performance
- **BGP Monitoring** - Stream health and message rates
- **Outage Analytics** - Outage patterns and trends

## Development

### Building Locally

```bash
./gradlew bootJar
```

### Running Tests

```bash
./gradlew test
```

### Development Mode

```bash
./gradlew bootRun
```

## Troubleshooting

### Common Issues

1. **Database Connection Failed**

   - Ensure TimescaleDB container is running: `docker-compose ps`
   - Check logs: `docker-compose logs timescaledb`

2. **Redis Connection Failed**

   - Ensure Redis container is running: `docker-compose ps`
   - Check logs: `docker-compose logs redis`

3. **BGP Stream Not Receiving Data**

   - Check network connectivity
   - Verify RIPE stream endpoint is accessible
   - Review application logs for WebSocket errors

4. **Map Not Loading**
   - Ensure application is running on port 8080
   - Check browser console for JavaScript errors
   - Verify API endpoints are responding

### Logs

```bash
# Application logs
docker-compose logs -f app

# All services
docker-compose logs -f

# Specific service
docker-compose logs -f redis
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request
