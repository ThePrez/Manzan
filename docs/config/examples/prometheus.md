# Prometheus Exporter

The Prometheus destination exposes Manzan events as Prometheus metrics via an HTTP endpoint that can be scraped by Prometheus servers.

## Overview

Prometheus is an open-source monitoring and alerting toolkit that collects metrics from configured targets at given intervals. By configuring Manzan with a Prometheus destination, you can:

- Expose IBM i system events as time-series metrics
- Integrate with Grafana for visualization
- Set up alerts using Prometheus AlertManager
- Track trends and patterns in your IBM i environment

## Configuration

### Basic Configuration

```ini
[prometheus_exporter]
type=prometheus
port=9090
```

### Full Configuration

```ini
[prometheus_exporter]
type=prometheus
# Port for Prometheus to scrape metrics (default: 9090)
port=9090
# Path for metrics endpoint (default: /metrics)
path=/metrics
# Prefix for all metric names (default: manzan_)
metricPrefix=ibmi_
# Optional: Basic authentication
username=prometheus
password=secret_password
```

## Configuration Parameters

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `type` | Yes | - | Must be `prometheus` |
| `port` | No | 9090 | Port number for the HTTP metrics endpoint |
| `path` | No | /metrics | URL path for the metrics endpoint |
| `metricPrefix` | No | manzan_ | Prefix added to all metric names |
| `username` | No | - | Username for basic authentication (requires password) |
| `password` | No | - | Password for basic authentication (requires username) |

## Metrics Exposed

### Default Metrics

The Prometheus destination automatically creates and updates the following metrics:

#### Event Metrics

- **`manzan_events_total`** (Counter)
  - Total number of events processed
  - Labels: `event_type`, `system`

- **`manzan_messages_total`** (Counter)
  - Total number of messages processed
  - Labels: `message_type`, `severity`, `system`

- **`manzan_jobs_total`** (Counter)
  - Total number of jobs processed
  - Labels: `job_name`, `status`, `system`

- **`manzan_audit_events_total`** (Counter)
  - Total number of audit events
  - Labels: `audit_type`, `user`, `system`

- **`manzan_file_operations_total`** (Counter)
  - Total number of file operations
  - Labels: `operation`, `system`

- **`manzan_sql_operations_total`** (Counter)
  - Total number of SQL operations
  - Labels: `operation`, `table`, `system`

#### Gauge Metrics

- **`manzan_queue_depth`** (Gauge)
  - Current message queue depth
  - Labels: `queue_name`, `system`

#### Histogram Metrics

- **`manzan_job_duration_seconds`** (Histogram)
  - Job execution duration in seconds
  - Labels: `job_name`, `status`, `system`
  - Buckets: Default Prometheus histogram buckets

## Example Metrics Output

When you access the metrics endpoint (e.g., `http://localhost:9090/metrics`), you'll see output like:

```
# HELP manzan_events_total Total number of events processed
# TYPE manzan_events_total counter
manzan_events_total{event_type="WATCH_MSG",system="IBMI01"} 1523.0

# HELP manzan_messages_total Total number of messages processed
# TYPE manzan_messages_total counter
manzan_messages_total{message_type="INFO",severity="00",system="IBMI01"} 845.0
manzan_messages_total{message_type="ERROR",severity="40",system="IBMI01"} 12.0

# HELP manzan_jobs_total Total number of jobs processed
# TYPE manzan_jobs_total counter
manzan_jobs_total{job_name="QZDASOINIT",status="success",system="IBMI01"} 234.0

# HELP manzan_job_duration_seconds Job execution duration in seconds
# TYPE manzan_job_duration_seconds histogram
manzan_job_duration_seconds_bucket{job_name="BACKUP",status="success",system="IBMI01",le="0.005"} 0.0
manzan_job_duration_seconds_bucket{job_name="BACKUP",status="success",system="IBMI01",le="0.01"} 0.0
manzan_job_duration_seconds_bucket{job_name="BACKUP",status="success",system="IBMI01",le="0.025"} 0.0
manzan_job_duration_seconds_bucket{job_name="BACKUP",status="success",system="IBMI01",le="0.05"} 0.0
manzan_job_duration_seconds_bucket{job_name="BACKUP",status="success",system="IBMI01",le="0.075"} 0.0
manzan_job_duration_seconds_bucket{job_name="BACKUP",status="success",system="IBMI01",le="0.1"} 0.0
manzan_job_duration_seconds_bucket{job_name="BACKUP",status="success",system="IBMI01",le="0.25"} 0.0
manzan_job_duration_seconds_bucket{job_name="BACKUP",status="success",system="IBMI01",le="0.5"} 0.0
manzan_job_duration_seconds_bucket{job_name="BACKUP",status="success",system="IBMI01",le="0.75"} 0.0
manzan_job_duration_seconds_bucket{job_name="BACKUP",status="success",system="IBMI01",le="1.0"} 0.0
manzan_job_duration_seconds_bucket{job_name="BACKUP",status="success",system="IBMI01",le="2.5"} 0.0
manzan_job_duration_seconds_bucket{job_name="BACKUP",status="success",system="IBMI01",le="5.0"} 0.0
manzan_job_duration_seconds_bucket{job_name="BACKUP",status="success",system="IBMI01",le="7.5"} 0.0
manzan_job_duration_seconds_bucket{job_name="BACKUP",status="success",system="IBMI01",le="10.0"} 0.0
manzan_job_duration_seconds_bucket{job_name="BACKUP",status="success",system="IBMI01",le="+Inf"} 45.0
manzan_job_duration_seconds_sum{job_name="BACKUP",status="success",system="IBMI01"} 2345.67
manzan_job_duration_seconds_count{job_name="BACKUP",status="success",system="IBMI01"} 45.0

# HELP manzan_queue_depth Current message queue depth
# TYPE manzan_queue_depth gauge
manzan_queue_depth{queue_name="QSYSOPR",system="IBMI01"} 3.0
```

## Prometheus Server Configuration

To scrape metrics from Manzan, add a job to your Prometheus configuration (`prometheus.yml`):

### Without Authentication

```yaml
scrape_configs:
  - job_name: 'manzan'
    static_configs:
      - targets: ['ibmi-server:9090']
```

### With Basic Authentication

```yaml
scrape_configs:
  - job_name: 'manzan'
    static_configs:
      - targets: ['ibmi-server:9090']
    basic_auth:
      username: 'prometheus'
      password: 'secret_password'
```

### With Custom Metrics Path

```yaml
scrape_configs:
  - job_name: 'manzan'
    metrics_path: '/custom/metrics'
    static_configs:
      - targets: ['ibmi-server:9090']
```

## Integration with Grafana

Once Prometheus is scraping Manzan metrics, you can create Grafana dashboards:

1. Add Prometheus as a data source in Grafana
2. Create a new dashboard
3. Use PromQL queries to visualize metrics:

### Example Queries

**Message rate per minute:**
```promql
rate(manzan_messages_total[1m])
```

**Job failure rate:**
```promql
rate(manzan_jobs_total{status="failure"}[5m])
```

**Average job duration:**
```promql
rate(manzan_job_duration_seconds_sum[5m]) / rate(manzan_job_duration_seconds_count[5m])
```

**Queue depth over time:**
```promql
manzan_queue_depth
```

## Alerting Examples

Create alerts in Prometheus AlertManager:

```yaml
groups:
  - name: manzan_alerts
    rules:
      - alert: HighMessageQueueDepth
        expr: manzan_queue_depth > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High message queue depth on {{ $labels.system }}"
          description: "Queue {{ $labels.queue_name }} has {{ $value }} messages"

      - alert: HighJobFailureRate
        expr: rate(manzan_jobs_total{status="failure"}[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High job failure rate on {{ $labels.system }}"
          description: "Job failure rate is {{ $value }} per second"

      - alert: NoEventsReceived
        expr: rate(manzan_events_total[5m]) == 0
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "No events received from {{ $labels.system }}"
          description: "Manzan has not received any events in the last 10 minutes"
```

## Complete Example

### Manzan Configuration (`dests.ini`)

```ini
[prometheus_metrics]
type=prometheus
port=9090
path=/metrics
metricPrefix=ibmi_
username=prometheus
password=MySecurePassword123
```

### Data Source Configuration (`data.ini`)

```ini
[joblog_monitor]
type=joblog
job=*
destinations=prometheus_metrics
```

### Prometheus Configuration (`prometheus.yml`)

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'ibmi_manzan'
    static_configs:
      - targets: ['ibmi-server.example.com:9090']
        labels:
          environment: 'production'
          datacenter: 'dc1'
    basic_auth:
      username: 'prometheus'
      password: 'MySecurePassword123'
    scrape_interval: 30s
    scrape_timeout: 10s
```

## Best Practices

1. **Use Metric Prefixes**: Set a meaningful `metricPrefix` to avoid naming conflicts
2. **Enable Authentication**: Always use basic authentication in production environments
3. **Monitor Scrape Health**: Check Prometheus targets page to ensure successful scraping
4. **Set Appropriate Scrape Intervals**: Balance between data freshness and system load
5. **Use Labels Wisely**: Labels create new time series, so avoid high-cardinality labels
6. **Create Dashboards**: Build Grafana dashboards for common monitoring scenarios
7. **Set Up Alerts**: Configure alerts for critical conditions before they become problems

## Troubleshooting

### Metrics Endpoint Not Accessible

- Verify the port is not blocked by firewall
- Check that Manzan is running and the destination is configured
- Test with: `curl http://localhost:9090/metrics`

### Authentication Failures

- Verify username and password match in both Manzan and Prometheus configs
- Check Prometheus logs for authentication errors
- Test with: `curl -u username:password http://localhost:9090/metrics`

### No Metrics Appearing

- Ensure data sources are configured and sending events to the Prometheus destination
- Check Manzan logs for errors
- Verify the destination is enabled in `dests.ini`

### High Memory Usage

- Reduce the number of unique label combinations
- Increase Prometheus scrape interval
- Consider using recording rules to pre-aggregate metrics

## See Also

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [PromQL Query Language](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Manzan Data Sources](../data.md)
- [Manzan Destinations](../dests.md)