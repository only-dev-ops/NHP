# Threat Feed Integration and Suspicious Behavior Scoring

This document outlines how the NHP project plans to enhance its BGP monitoring capabilities through external threat intelligence feeds and a custom scoring mechanism for suspicious behaviors.

## 🛡️ Threat Feed Integration

**Threat feeds** are external data sources listing known malicious IP ranges, ASNs, and sometimes behaviors. These feeds are commonly maintained by cybersecurity organizations or open-source projects.

### Purpose

Integrating threat feeds into NHP allows us to:

- Flag BGP announcements from known malicious ASNs or IP ranges.
- Enrich live data with external threat context.
- Alert on prefix activities associated with abuse infrastructure.

### Common Threat Feed Sources

- Spamhaus DROP/EDROP
- Shadowserver ASN Reports
- Team Cymru BGP Intelligence
- FireHOL IP Lists

### Integration Plan

- Regularly ingest feeds and cache them in Redis or Kafka topic `bgp.threat.intel`.
- Enrich BGP events during processing by checking:
  - If origin ASN or prefix matches a known threat.
  - If any AS path includes blacklisted entities.
- Raise alerts or produce to a `bgp.anomalies` topic when matches are found.

## 🧮 Suspicious Behavior Scoring

To identify unusual and potentially harmful BGP activities, a scoring system will be implemented based on observed behavior.

### What Gets Scored

| Event Type                    | Score |
| ----------------------------- | ----- |
| Origin ASN mismatch           | +50   |
| Prefix in known threat feed   | +100  |
| Flapping (>3 events/hour)     | +20   |
| Announcement of /25 or longer | +10   |
| Path includes blacklisted ASN | +30   |

A high cumulative score will signal that the prefix may be involved in abnormal routing behavior or an attack.

### How It Works

- Each BGP update will be evaluated against scoring rules.
- Scores may be stored in Redis or published to a scoring topic.
- If a threshold is exceeded, the system can:
  - Produce a structured alert.
  - Annotate the prefix for dashboards.
  - Send notifications via alerting systems.

## Goals

- Provide actionable security insights in near-real-time.
- Add intelligence-driven context to routing events.
- Enable automated monitoring and alerting pipelines.
