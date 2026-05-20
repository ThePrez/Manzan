# Job Log Monitoring Configuration Examples

The `joblog` event type allows you to monitor IBM i job logs in near-real-time and send log entries to various destinations.

## How It Works

- **Polling-based**: Checks for new job log entries at regular intervals (configurable via `interval`)
- **Timestamp tracking**: Automatically tracks the last processed timestamp to avoid duplicates
- **SQL-based**: Uses IBM i SQL Services `QSYS2.JOBLOG_INFO` table function
- **Near-real-time**: With `interval=200` (200ms), achieves ~200-400ms latency

## Configuration Properties

### Required Properties

| Property | Description | Example |
|----------|-------------|---------|
| `type` | Must be `joblog` | `type=joblog` |
| `jobs` | Comma-separated list of job identifiers in format `number/user/name` | `jobs=123456/QUSER/MYJOB,789012/ADMIN/BATCH01` |
| `destinations` | Where to send the log entries (as defined in `dests.ini`) | `destinations=elasticsearch,slack` |

### Optional Properties

| Property | Description | Default | Example |
|----------|-------------|---------|---------|
| `interval` | Polling interval in milliseconds | `1000` | `interval=200` for near-real-time |
| `format` | Custom format string for log entries | JSON | `format=$MESSAGE_ID$: $MESSAGE_TEXT$` |
| `injections.*` | Custom key-value pairs to inject into each log entry | None | `injections.ENVIRONMENT=PRODUCTION` |

## Available Format Fields

When using custom `format` strings, the following fields are available:

| Field | Description | Example Value |
|-------|-------------|---------------|
| `JOB_FULL` | Full job identifier | `123456/QUSER/MYJOB` |
| `JOB_NUMBER` | Job number | `123456` |
| `JOB_USER` | Job user | `QUSER` |
| `JOB_NAME` | Job name | `MYJOB` |
| `MESSAGE_ID` | Message identifier | `CPF9898` |
| `MESSAGE_TYPE` | Message type | `DIAGNOSTIC`, `INFORMATIONAL`, `ESCAPE` |
| `SEVERITY` | Message severity (0-99) | `30` |
| `MESSAGE_TIMESTAMP` | When message was sent | `2026-03-17 12:34:56.789` |
| `MESSAGE_TEXT` | First level message text | `File not found` |
| `MESSAGE_SECOND_LEVEL_TEXT` | Second level help text | `The file specified was not found...` |
| `FROM_PROGRAM` | Sending program name | `MYPGM` |
| `FROM_LIBRARY` | Sending program library | `MYLIB` |
| `FROM_MODULE` | Sending module name | `MYMOD` |
| `FROM_PROCEDURE` | Sending procedure name | `MYPROC` |
| `MESSAGE_KEY` | Message key | `0000000123` |

## Example Configurations

### Example 1: Basic Job Log Monitoring

Monitor a single job with default 1-second polling:

```ini
[joblog_basic]
type=joblog
jobs=123456/QUSER/MYJOB
destinations=stdout
```

### Example 2: Near-Real-Time Monitoring

Monitor critical jobs with 200ms polling for near-real-time alerts:

```ini
[joblog_realtime]
type=joblog
jobs=123456/ADMIN/CRITICAL,789012/ADMIN/PAYMENT
destinations=pagerduty,slack
interval=200
format=[$JOB_NAME$] $MESSAGE_ID$ (Severity $SEVERITY$): $MESSAGE_TEXT$
```

### Example 3: Multiple Jobs with Custom Format

Monitor multiple batch jobs with custom formatting:

```ini
[joblog_batch]
type=joblog
jobs=111111/QUSER/BATCH01,222222/QUSER/BATCH02,333333/QUSER/BATCH03
destinations=elasticsearch,grafanaloki
format=$MESSAGE_TIMESTAMP$ [$JOB_NUMBER$/$JOB_USER$/$JOB_NAME$] $MESSAGE_ID$ ($SEVERITY$): $MESSAGE_TEXT$
interval=500
```

### Example 4: Production Monitoring with Injections

Monitor production jobs with environment metadata:

```ini
[joblog_production]
type=joblog
jobs=123456/PROD/ORDPROC,789012/PROD/INVUPD
destinations=elasticsearch,slack,pagerduty
format=[$ENVIRONMENT$/$SYSTEM$] $JOB_NAME$: $MESSAGE_ID$ - $MESSAGE_TEXT$
interval=200
injections.ENVIRONMENT=PRODUCTION
injections.SYSTEM=IBMI-PROD-01
injections.REGION=US-EAST
```

### Example 5: Development Monitoring

Monitor development jobs with relaxed polling:

```ini
[joblog_dev]
type=joblog
jobs=555555/DEVUSER/TESTJOB
destinations=stdout
format=DEV: $MESSAGE_TIMESTAMP$ - $MESSAGE_TEXT$
interval=5000
injections.ENVIRONMENT=DEVELOPMENT
```

### Example 6: High-Severity Alerts Only

Monitor for high-severity messages (Note: filtering by severity requires custom processing in destinations):

```ini
[joblog_critical]
type=joblog
jobs=123456/QUSER/CRITICAL
destinations=pagerduty
format=CRITICAL: [$JOB_NAME$] $MESSAGE_ID$ (Severity $SEVERITY$): $MESSAGE_TEXT$ | From: $FROM_PROGRAM$/$FROM_LIBRARY$
interval=100
```

### Example 7: Detailed Logging to Elasticsearch

Send comprehensive job log data to Elasticsearch for analysis:

```ini
[joblog_detailed]
type=joblog
jobs=123456/QUSER/WEBAPP,789012/QUSER/API
destinations=elasticsearch
interval=200
injections.APPLICATION=WEBAPP
injections.TIER=BACKEND
injections.VERSION=2.1.0
```

## Integration Examples

### With Elasticsearch

```ini
[joblog_es]
type=joblog
jobs=123456/QUSER/MYJOB
destinations=elasticsearch_dest
interval=200

# In dests.ini:
[elasticsearch_dest]
type=elasticsearch
host=elasticsearch.example.com
port=9200
index=ibmi-joblogs
```

### With Slack

```ini
[joblog_slack]
type=joblog
jobs=789012/ADMIN/CRITICAL
destinations=slack_alerts
format=WARNING: Job $JOB_NAME$: $MESSAGE_TEXT$
interval=200

# In dests.ini:
[slack_alerts]
type=slack
webhook=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

### With PagerDuty

```ini
[joblog_pagerduty]
type=joblog
jobs=123456/PROD/PAYMENT
destinations=pagerduty_oncall
format=Payment Job Alert: $MESSAGE_ID$ - $MESSAGE_TEXT$
interval=100

# In dests.ini:
[pagerduty_oncall]
type=pagerduty
routingKey=YOUR_ROUTING_KEY
```

### With Grafana Loki

```ini
[joblog_loki]
type=joblog
jobs=123456/QUSER/WEBAPP,789012/QUSER/API
destinations=loki_logs
interval=200
injections.job=$JOB_NAME$
injections.severity=$SEVERITY$

# In dests.ini:
[loki_logs]
type=grafanaloki
url=http://loki.example.com:3100
```

## Performance Considerations

### Polling Interval Guidelines

| Use Case | Recommended Interval | Latency | System Load |
|----------|---------------------|---------|-------------|
| **Critical Production** | 100-200ms | ~100-400ms | High |
| **Normal Monitoring** | 1000ms (default) | ~1-2s | Low |
| **Development/Testing** | 5000ms | ~5-6s | Very Low |
| **Batch Job Monitoring** | 2000-5000ms | ~2-6s | Low |

### Multiple Jobs

- Each job requires a separate SQL query
- Queries are combined with `UNION ALL`
- More jobs = longer query execution time
- Recommended: Monitor up to 10 jobs per configuration
- For more jobs, create multiple joblog configurations

### System Requirements

- **IBM i Version**: 7.2 or higher (for QSYS2.JOBLOG_INFO)
- **SQL Services**: Must be available
- **Database Load**: Proportional to polling frequency and number of jobs

## Troubleshooting

### No Log Entries Appearing

1. Verify job identifier format: `number/user/name`
2. Check that jobs are active: `WRKACTJOB`
3. Verify QSYS2.JOBLOG_INFO is available: `SELECT * FROM TABLE(QSYS2.JOBLOG_INFO('job/user/name'))`
4. Check Manzan logs for errors

### High CPU Usage

- Increase `interval` to reduce polling frequency
- Reduce number of monitored jobs
- Consider monitoring fewer jobs per configuration

### Duplicate Messages

- Should not occur due to timestamp tracking
- If duplicates appear, check system clock synchronization
- Verify MESSAGE_TIMESTAMP field is being returned correctly

## Best Practices

1. **Start with default interval (1000ms)** and adjust based on needs
2. **Use 200ms interval** for critical jobs requiring near-real-time monitoring
3. **Group related jobs** in the same configuration
4. **Use injections** to add context (environment, system, application)
5. **Format messages** appropriately for each destination
6. **Monitor system load** when using fast polling intervals
7. **Test configurations** in development before production deployment

## See Also

- [Data Configuration](../data.md)
- [Destination Configuration](../dests.md)
- [Message Formatting](../format.md)
- [Elasticsearch Example](./elasticsearch.md)
- [Slack Example](./slack.md)
- [PagerDuty Example](./pagerDuty.md)