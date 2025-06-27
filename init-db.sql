-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Create outage_events table for per-prefix events
CREATE TABLE IF NOT EXISTS outage_events (
    id BIGSERIAL PRIMARY KEY,
    prefix CIDR NOT NULL,
    origin_asn INTEGER NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    event_type VARCHAR(20) NOT NULL CHECK (event_type IN ('outage_start', 'recovery')),
    last_path TEXT,
    withdrawn_by TEXT[],
    resolved_at TIMESTAMPTZ,
    duration INTERVAL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create asn_outages table for ASN-level outage correlation
CREATE TABLE IF NOT EXISTS asn_outages (
    id BIGSERIAL PRIMARY KEY,
    asn INTEGER NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    duration INTERVAL,
    prefixes TEXT[] NOT NULL,
    severity INTEGER CHECK (severity >= 0 AND severity <= 100),
    country TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create hypertable for time-series data on outage_events
SELECT create_hypertable (
        'outage_events', 'timestamp', if_not_exists = > TRUE
    );

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_outage_events_prefix ON outage_events (prefix);

CREATE INDEX IF NOT EXISTS idx_outage_events_origin_asn ON outage_events (origin_asn);

CREATE INDEX IF NOT EXISTS idx_outage_events_timestamp ON outage_events (timestamp);

CREATE INDEX IF NOT EXISTS idx_outage_events_event_type ON outage_events (event_type);

CREATE INDEX IF NOT EXISTS idx_asn_outages_asn ON asn_outages (asn);

CREATE INDEX IF NOT EXISTS idx_asn_outages_start_time ON asn_outages (start_time);

CREATE INDEX IF NOT EXISTS idx_asn_outages_country ON asn_outages (country);

-- Create a function to automatically calculate duration when recovery event is inserted
CREATE OR REPLACE FUNCTION calculate_outage_duration()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.event_type = 'recovery' THEN
        -- Find the corresponding outage_start event
        UPDATE outage_events 
        SET resolved_at = NEW.timestamp,
            duration = NEW.timestamp - timestamp
        WHERE prefix = NEW.prefix 
          AND origin_asn = NEW.origin_asn 
          AND event_type = 'outage_start'
          AND resolved_at IS NULL
          AND timestamp < NEW.timestamp
        ORDER BY timestamp DESC
        LIMIT 1;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically calculate duration
DROP TRIGGER IF EXISTS trigger_calculate_duration ON outage_events;

CREATE TRIGGER trigger_calculate_duration
    AFTER INSERT ON outage_events
    FOR EACH ROW
    EXECUTE FUNCTION calculate_outage_duration();