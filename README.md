# NHP: National Highway of Prefixes

A real-time BGP monitoring application using RIPE RIS Live stream data. Built with Java + Spring Boot, containerized with Docker, and integrated with Prometheus and Grafana for observability.

## Features

- Subscribe to real-time BGP updates from RIPE RIS
- User-defined prefix monitoring (coming soon)
- Metrics collection with Prometheus
- Dashboard visualization in Grafana
- PostgreSQL/TimescaleDB persistence

## Tech Stack

- Java 17 (OpenJDK)
- Spring Boot 3 (WebFlux, Actuator, JPA)
- Docker + Docker Compose
- Prometheus + Grafana
- PostgreSQL with TimescaleDB

## Getting Started

### Prerequisites

- Docker
- Docker Compose

### Build and Run

```bash
docker-compose up --build
```

- App: [http://localhost:8080](http://localhost:8080)
- Prometheus: [http://localhost:9090](http://localhost:9090)
- Grafana: [http://localhost:3000](http://localhost:3000) (admin/admin)

### Prometheus Metrics

Spring Boot exposes metrics at `/actuator/prometheus`, auto-scraped by Prometheus.

## Development Notes

- RIPE RIS WebSocket endpoint: `wss://ris-live.ripe.net/v1/ws/`
- App connects and subscribes to filtered BGP updates based on user-defined prefixes
- RipeStreamClient filters incoming BGP events against stored prefixes
- Reloading Cache for monitoring tracked prefixes
- Metrics:
  - How many messages consumed total and per prefix type?
  - Announces/Withdrawls by prefix type

## TODO

- Kafka streams per prefix for history lookup
