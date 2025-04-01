# NHP: National Highway of Prefixes

**NHP** is a real-time BGP (Border Gateway Protocol) monitoring application that utilizes the RIPE RIS Live data stream to track changes in network prefixes globally. The system helps network engineers, researchers, and security professionals observe route announcements and withdrawals in real-time. It also aims to support historical analysis of prefix behaviors using Kafka and TimescaleDB in future iterations.

## Project Overview

NHP connects to the RIPE RIS Live WebSocket and subscribes to real-time BGP updates. Users can define a set of network prefixes to monitor, and the app filters updates accordingly. The system provides insights such as path changes, prefix announcements and withdrawals, and can potentially be extended to visualize routing history.

## Tech Stack

- **Java 17 (OpenJDK)** — Core programming language
- **Spring Boot 3** — Reactive backend with WebFlux and metrics with Actuator
- **PostgreSQL + TimescaleDB** — For future historical prefix storage and queries
- **Docker + Docker Compose** — Simplified containerized deployment
- **Prometheus** — Metrics collection
- **Grafana** — Dashboarding and visualization
- **WebSocket Client** — For RIPE RIS live stream integration

## Features

- Real-time BGP message consumption from RIPE RIS
- Dynamic prefix monitoring system (add/remove prefixes live)
- Filtering and tracking announcements and withdrawals by prefix
- Metrics collection on message volume and prefix activity
- Dashboard visualization using Grafana
- Dockerized setup with Prometheus and Grafana
- Future: Kafka integration for prefix-specific historical stream storage

## Getting Started

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/)

### Build and Run

Clone the repository, then run:

```bash
docker-compose up --build
```

This launches all required containers.

- App: [http://localhost:8080](http://localhost:8080)
- Prometheus: [http://localhost:9090](http://localhost:9090)
- Grafana: [http://localhost:3000](http://localhost:3000) (Login: `admin` / `admin`)

## API Endpoints

### Prefix Management

- `GET /api/prefixes`  
  Returns the list of currently monitored prefixes.

- `POST /api/prefixes`  
  Add a new prefix to monitor.  
  **Request Body:**

  ```json
  {
    "prefix": "8.8.8.0/24"
  }
  ```

- `DELETE /api/prefixes/{prefix}`  
  Remove a monitored prefix.

### Metrics

- `GET /actuator/prometheus`  
  Prometheus endpoint for scraping metrics.

## Development Notes

- RIPE RIS WebSocket endpoint: `wss://ris-live.ripe.net/v1/ws/`
- `RipeStreamClient` subscribes to updates dynamically based on tracked prefixes.
- In-memory cache is reloaded when prefixes are modified.
- A debounce mechanism controls WebSocket reconnection to avoid flooding.

## Metrics Overview

- Total messages consumed
- Number of announcements and withdrawals per prefix
- Type-based prefix statistics
- Dropped or ignored message counts

## Future Ideas

- Kafka streams per prefix for historical tracking and replay
- UI to allow users to explore routing changes visually
- Alerting system for suspicious BGP behavior (e.g., prefix hijacks)
- Integration with external threat intelligence feeds
- REST API for prefix history queries (backed by TimescaleDB)
- Real-time geo-mapping of AS paths

## License

MIT License
