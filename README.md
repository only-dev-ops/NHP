# NHP: National Highway of Prefixes

**NHP** is a real-time BGP (Border Gateway Protocol) monitoring application that utilizes the RIPE RIS Live data stream to track changes in network prefixes globally. The system helps network engineers, researchers, and security professionals observe route announcements and withdrawals in real-time. It supports live monitoring, event logging, and metric visualization with Kafka, Redis, Prometheus, and Grafana.

## Project Overview

NHP connects to the RIPE RIS Live WebSocket and subscribes to real-time BGP updates. Users can define a set of network prefixes to monitor, and the app filters updates accordingly. The system tracks prefix activity, path changes, and flapping behavior in real-time. Events are published to Kafka for historical replay or auditing. Redis is used as the source of truth for live state during runtime.

## Architecture Overview

- **Kafka** is used as an event log for all prefix tracking actions (`ADD`/`REMOVE`). The topic `bgp.prefixes.track` retains every state mutation and allows the system to rebuild tracking state deterministically.
- **Redis** acts as the hot store for currently tracked prefixes during runtime. When the service starts, it replays the Kafka topic to restore prefix state into Redis.
- **WebSocket Client** connects to RIPE RIS Live and dynamically resubscribes based on current prefixes stored in Redis. Reconnection uses a debounce timer to avoid unnecessary reconnect storms.
- **Spring Boot** serves as the backend framework, exposing APIs for prefix management and providing a metrics endpoint.
- **Prometheus + Grafana** collect and visualize application metrics, including BGP message rates, prefix activity, and system performance.

## Tech Stack

- **Java 17 (OpenJDK)** — Core programming language
- **Spring Boot 3** — Reactive backend with WebFlux and Actuator
- **Kafka** — Event sourcing for prefix tracking history and event replay
- **Redis** — Real-time state store for active prefix tracking
- **Docker + Docker Compose** — Simplified containerized deployment
- **Prometheus** — Metrics collection
- **Grafana** — Dashboarding and visualization
- **WebSocket Client** — For RIPE RIS live stream integration

## Features

- Real-time BGP message consumption from RIPE RIS Live
- Kafka-based event sourcing of prefix `ADD`/`REMOVE` actions
- Redis-based runtime prefix tracking with one-time Kafka replay on startup
- Dynamic prefix subscription with debounce-based reconnect logic
- Live prefix monitoring (announcement and withdrawal tracking)
- Planned anomaly detection (e.g., hijacks, leaks, path changes)
- Prometheus-compatible metrics exposed via `/actuator/prometheus`
- Grafana dashboards for prefix activity, volume, peer ASN visibility
- Batch and single prefix add/remove via REST API

## API Endpoints

### Prefix Management

- `GET /api/prefixes`  
  Returns the list of currently monitored prefixes from Redis.

- `POST /api/prefixes`  
  Add one or more prefixes to monitor.  
  **Request Body (single or batch):**

  ```json
  {
    "prefixes": ["8.8.8.0/24", "1.1.1.0/24"]
  }
  ```

- `DELETE /api/prefixes/{prefix}`  
  Remove a monitored prefix. This updates Redis and publishes a `REMOVE` event to Kafka.

### Metrics

- `GET /actuator/prometheus`  
  Prometheus endpoint for scraping real-time metrics.

## Development Notes

- RIPE RIS WebSocket endpoint: `wss://ris-live.ripe.net/v1/ws/`
- `RipeStreamClient` subscribes dynamically based on current Redis prefix set.
- Prefix state is restored at boot by replaying the Kafka topic `bgp.prefixes.track`.
- A debounce mechanism controls WebSocket reconnection to avoid flooding.
- Redis is the only runtime dependency for state, Kafka is the authoritative source of prefix change history.

## Metrics Overview

- Total messages consumed from RIS Live
- Announcements and withdrawals per prefix
- Tracked prefix count
- Redis sync status and reconnects
- Kafka consumer lag and processed events
- Dropped or malformed message count

## Future Ideas

- Prefix hijack and leak detection with real-time alerting
- Historical event replays and visualization (via Kafka stream reprocessing)
- User-defined expected origin ASN for prefix validation
- [Threat feed integration and suspicious behavior scoring](./THREAT_INTELLIGENCE.md)
- REST endpoints for live and historical prefix insights
- Geo-mapping of AS paths and BGP anomalies

## License

MIT License
