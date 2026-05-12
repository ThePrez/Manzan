# Prometheus Exporter Testing Guide

This guide walks you through testing the Prometheus exporter implementation in Manzan, from basic functionality to production deployment.

---

## Prerequisites

Before testing, ensure you have:
- Manzan installed and configured
- Access to an IBM i system
- Java 8+ installed
- Maven installed (for building)
- curl or similar HTTP client
- (Optional) Prometheus server for integration testing

---

## Testing Levels

### Level 1: Unit Tests (Automated)
### Level 2: Local Manual Testing
### Level 3: Integration Testing with Prometheus
### Level 4: Production Validation

---

## Level 1: Unit Tests

### Step 1: Build the Project

```bash
cd /path/to/Manzan/camel

# Clean and compile
mvn clean compile

# Run all tests
mvn test

# Run only Prometheus tests
mvn test -Dtest=PrometheusDestinationTest
```

### Expected Output

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running CamelTests.PrometheusDestinationTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Troubleshooting Unit Tests

**Issue: Tests fail with connection errors**
```
Solution: Ensure you have app.ini configured with valid IBM i credentials
Location: camel/app.ini
```

**Issue: Port already in use**
```
Solution: The test uses port 9091. Check if it's available:
netstat -an | grep 9091
```

---

## Level 2: Local Manual Testing

### Step 1: Create Test Configuration

Create a test configuration directory:

```bash
mkdir -p ~/manzan-test/config
cd ~/manzan-test/config
```

Create `app.ini`:
```ini
[connection]
host=your-ibmi-hostname
user=your-username
password=your-password
```

Create `data.ini`:
```ini
[test_joblog]
type=joblog
# Monitor a specific job or use * for all
job=*
destinations=prometheus_test
poll_interval=5000
```

Create `dests.ini`:
```ini
[prometheus_test]
type=prometheus
port=9090
path=/metrics
metricPrefix=test_
# Optional: Enable authentication
# username=testuser
# password=testpass
```

### Step 2: Build Manzan

```bash
cd /path/to/Manzan/camel
mvn clean package
```

This creates: `target/manzan-1.0-jar-with-dependencies.jar`

### Step 3: Run Manzan

```bash
# Run with custom config directory
java -jar target/manzan-1.0-jar-with-dependencies.jar \
  --configdir=/path/to/manzan-test/config
```

### Expected Output

```
Apache Camel version 3.14.10
[main] INFO  - Route: test_joblog started
[main] INFO  - Route: prometheus_test started
[main] INFO  - Route: prometheus_test_http_endpoint started
```

### Step 4: Test the Metrics Endpoint

**Test 1: Basic Connectivity**

```bash
# Test without authentication
curl http://localhost:9090/metrics

# Expected: Prometheus metrics output
```

**Test 2: With Authentication (if enabled)**

```bash
curl -u testuser:testpass http://localhost:9090/metrics
```

**Test 3: Verify Metrics Format**

```bash
curl http://localhost:9090/metrics | head -20
```

Expected output:
```
# HELP test_events_total Total number of events processed
# TYPE test_events_total counter
test_events_total{event_type="WATCH_VLOG",system="YOURSYSTEM"} 5.0

# HELP test_messages_total Total number of messages processed
# TYPE test_messages_total counter
test_messages_total{message_type="INFO",severity="00",system="YOURSYSTEM"} 3.0

# HELP test_jobs_total Total number of jobs processed
# TYPE test_jobs_total counter
test_jobs_total{job_name="QZDASOINIT",status="success",system="YOURSYSTEM"} 2.0
```

**Test 4: Verify Metrics are Updating**

```bash
# Get initial count
curl -s http://localhost:9090/metrics | grep "test_events_total"

# Wait 10 seconds
sleep 10

# Get updated count (should be higher)
curl -s http://localhost:9090/metrics | grep "test_events_total"
```

**Test 5: Check Specific Metric Types**

```bash
# Check for counters
curl -s http://localhost:9090/metrics | grep "TYPE.*counter"

# Check for gauges
curl -s http://localhost:9090/metrics | grep "TYPE.*gauge"

# Check for histograms
curl -s http://localhost:9090/metrics | grep "TYPE.*histogram"
```

### Step 5: Test Different Event Types

Modify `data.ini` to test different event sources:

**Test Messages:**
```ini
[test_messages]
type=msg
queue=QSYSOPR
destinations=prometheus_test
poll_interval=5000
```

**Test SQL Events:**
```ini
[test_sql]
type=sql
sql=SELECT * FROM QSYS2.SYSTABLES FETCH FIRST 10 ROWS ONLY
destinations=prometheus_test
poll_interval=10000
```

**Test Audit Events:**
```ini
[test_audit]
type=audit
audit_type=PW
destinations=prometheus_test
poll_interval=5000
```

After each change:
1. Restart Manzan
2. Wait for events to be processed
3. Check metrics endpoint for new metrics

---

## Level 3: Integration Testing with Prometheus

**Important:** Prometheus should run on a separate system (Windows PC, Linux server, or Mac) and scrape metrics from Manzan on IBM i over the network. Prometheus is not designed to run on IBM i itself.

### Step 1: Install Prometheus

Choose the installation method for your platform:

#### Windows Installation (Recommended for Testing)

1. **Download Prometheus for Windows:**
   ```
   https://github.com/prometheus/prometheus/releases/download/v2.48.0/prometheus-2.48.0.windows-amd64.zip
   ```

2. **Extract to a directory:**
   ```powershell
   # Extract to C:\prometheus
   Expand-Archive prometheus-2.48.0.windows-amd64.zip -DestinationPath C:\
   Rename-Item C:\prometheus-2.48.0.windows-amd64 C:\prometheus
   ```

3. **Verify installation:**
   ```powershell
   cd C:\prometheus
   .\prometheus.exe --version
   ```

#### Linux Installation

```bash
# Download Prometheus
cd /tmp
wget https://github.com/prometheus/prometheus/releases/download/v2.48.0/prometheus-2.48.0.linux-amd64.tar.gz
tar xvfz prometheus-2.48.0.linux-amd64.tar.gz

# Move to installation directory
sudo mv prometheus-2.48.0.linux-amd64 /opt/prometheus

# Verify installation
/opt/prometheus/prometheus --version
```

#### macOS Installation

```bash
# Using Homebrew
brew install prometheus

# Verify installation
prometheus --version
```

### Step 2: Configure Prometheus

Create `prometheus.yml` in your Prometheus directory:

**For Windows** (`C:\prometheus\prometheus.yml`):
```yaml
global:
  scrape_interval: 30s
  evaluation_interval: 30s

scrape_configs:
  - job_name: 'manzan'
    static_configs:
      # Replace with your IBM i hostname or IP
      - targets: ['your-ibmi-hostname:9090']
        labels:
          environment: 'test'
          system: 'ibmi'
    
    # Authentication (must match Manzan dests.ini)
    basic_auth:
      username: 'prometheus'
      password: 'changeme123'
    
    scrape_interval: 30s
    scrape_timeout: 10s
```

**For Linux/Mac** (`/opt/prometheus/prometheus.yml` or `/usr/local/etc/prometheus.yml`):
```yaml
global:
  scrape_interval: 30s
  evaluation_interval: 30s

scrape_configs:
  - job_name: 'manzan'
    static_configs:
      # Replace with your IBM i hostname or IP
      - targets: ['your-ibmi-hostname:9090']
        labels:
          environment: 'test'
          system: 'ibmi'
    
    # Authentication (must match Manzan dests.ini)
    basic_auth:
      username: 'prometheus'
      password: 'changeme123'
    
    scrape_interval: 30s
    scrape_timeout: 10s
```

**Important Configuration Notes:**
- Replace `your-ibmi-hostname` with your IBM i system's hostname or IP address
- Ensure the `username` and `password` match what you configured in Manzan's `/QOpenSys/etc/manzan/dests.ini`
- If Manzan is on the same machine as Prometheus (not typical), use `localhost:9090`

### Step 3: Verify Network Connectivity

Before starting Prometheus, verify you can reach Manzan from your Prometheus server:

**From Windows:**
```powershell
# Test connectivity
Test-NetConnection -ComputerName your-ibmi-hostname -Port 9090

# Test metrics endpoint
curl -u prometheus:changeme123 http://your-ibmi-hostname:9090/metrics
```

**From Linux/Mac:**
```bash
# Test connectivity
telnet your-ibmi-hostname 9090

# Test metrics endpoint
curl -u prometheus:changeme123 http://your-ibmi-hostname:9090/metrics
```

If you can't connect:
- Check IBM i firewall settings
- Verify Manzan is running: `sc status manzan` (on IBM i)
- Verify port 9090 is listening: `netstat -an | grep 9090` (on IBM i)

### Step 4: Start Prometheus

**On Windows:**
```powershell
cd C:\prometheus
.\prometheus.exe --config.file=prometheus.yml
```

**On Linux:**
```bash
/opt/prometheus/prometheus --config.file=/opt/prometheus/prometheus.yml
```

**On macOS:**
```bash
prometheus --config.file=/usr/local/etc/prometheus.yml
```

### Step 5: Access Prometheus UI

Open your browser to: `http://localhost:9090`

**Note:** Prometheus UI runs on port 9090 by default. If this conflicts with Manzan (which also uses 9090), you can change Prometheus's port:

```bash
# Windows
.\prometheus.exe --config.file=prometheus.yml --web.listen-address=:9091

# Linux/Mac
prometheus --config.file=prometheus.yml --web.listen-address=:9091
```

Then access UI at: `http://localhost:9091`

### Step 4: Verify Scraping

1. **Check Targets:**
   - Navigate to: http://localhost:9090/targets
   - Verify `manzan_test` target shows as "UP"
   - Check "Last Scrape" time is recent

2. **Query Metrics:**
   - Navigate to: http://localhost:9090/graph
   - Try these queries:

```promql
# Total events
test_events_total

# Event rate per minute
rate(test_events_total[1m])

# Jobs by status
test_jobs_total

# Message queue depth
test_queue_depth
```

3. **Verify Labels:**
```promql
# Group by event type
sum by (event_type) (test_events_total)

# Group by system
sum by (system) (test_events_total)
```

### Step 5: Test Alerting

Create `alerts.yml`:

```yaml
groups:
  - name: manzan_alerts
    rules:
      - alert: HighEventRate
        expr: rate(test_events_total[5m]) > 10
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High event rate detected"
          description: "Event rate is {{ $value }} events/sec"
      
      - alert: NoEventsReceived
        expr: rate(test_events_total[5m]) == 0
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "No events received"
          description: "Manzan has not received events in 5 minutes"
```

Update `prometheus.yml`:
```yaml
rule_files:
  - "alerts.yml"
```

Restart Prometheus and check alerts at: http://localhost:9090/alerts

---

## Level 4: Production Validation

### Pre-Production Checklist

- [ ] Unit tests pass
- [ ] Manual testing successful
- [ ] Prometheus integration working
- [ ] Authentication enabled
- [ ] HTTPS configured (see PROMETHEUS_SECURITY_REPORT.md)
- [ ] IP whitelisting configured
- [ ] Rate limiting enabled
- [ ] Monitoring and alerting set up
- [ ] Documentation reviewed
- [ ] Backup and recovery tested

### Production Deployment Steps

**1. Deploy with Minimal Configuration**

```ini
[prometheus_prod]
type=prometheus
port=9090
path=/metrics
metricPrefix=ibmi_
username=prometheus_prod_user
password=<strong-password-here>
```

**2. Configure Reverse Proxy (Nginx)**

See detailed instructions in `PROMETHEUS_SECURITY_REPORT.md`

**3. Test Production Endpoint**

```bash
# From Prometheus server
curl -u prometheus_prod_user:password \
  https://metrics.yourdomain.com/metrics

# Verify HTTPS redirect
curl -I http://metrics.yourdomain.com/metrics
# Should return: 301 Moved Permanently
```

**4. Configure Production Prometheus**

```yaml
scrape_configs:
  - job_name: 'ibmi_production'
    scheme: https
    static_configs:
      - targets: ['metrics.yourdomain.com']
        labels:
          environment: 'production'
          datacenter: 'dc1'
    tls_config:
      insecure_skip_verify: false
    basic_auth:
      username: 'prometheus_prod_user'
      password: '<strong-password-here>'
    scrape_interval: 30s
```

**5. Monitor Initial Deployment**

```bash
# Check Manzan logs
tail -f /var/log/manzan/manzan.log

# Check Nginx access logs
tail -f /var/log/nginx/manzan-prometheus-access.log

# Check Prometheus targets
# Navigate to: https://prometheus.yourdomain.com/targets
```

**6. Validate Metrics**

Run these queries in Prometheus:

```promql
# Verify metrics are being scraped
up{job="ibmi_production"}

# Check event rate
rate(ibmi_events_total[5m])

# Verify all expected metrics exist
count by (__name__) ({job="ibmi_production"})
```

---

## Common Testing Scenarios

### Scenario 1: Test Job Monitoring

**Setup:**
```ini
[job_monitor]
type=joblog
job=QZDASOINIT
destinations=prometheus_test
poll_interval=5000
```

**Validation:**
```bash
# Check for job metrics
curl -s http://localhost:9090/metrics | grep "jobs_total"

# Expected output:
# test_jobs_total{job_name="QZDASOINIT",status="success",system="MYSYSTEM"} 15.0
```

### Scenario 2: Test Message Queue Monitoring

**Setup:**
```ini
[msg_monitor]
type=msg
queue=QSYSOPR
destinations=prometheus_test
poll_interval=5000
```

**Validation:**
```bash
# Check for message metrics
curl -s http://localhost:9090/metrics | grep "messages_total"

# Check queue depth
curl -s http://localhost:9090/metrics | grep "queue_depth"
```

### Scenario 3: Test High-Volume Events

**Setup:**
```ini
[high_volume]
type=sql
sql=SELECT * FROM QSYS2.ACTIVE_JOB_INFO()
destinations=prometheus_test
poll_interval=1000
```

**Validation:**
```bash
# Monitor metric growth
watch -n 1 'curl -s http://localhost:9090/metrics | grep events_total'

# Check for performance issues
top -p $(pgrep -f manzan)
```

---

## Performance Testing

### Test 1: Baseline Performance

```bash
# Measure response time
time curl -s http://localhost:9090/metrics > /dev/null

# Measure payload size
curl -s http://localhost:9090/metrics | wc -c
```

### Test 2: Load Testing

```bash
# Install Apache Bench
sudo yum install httpd-tools

# Run load test (100 requests, 10 concurrent)
ab -n 100 -c 10 http://localhost:9090/metrics

# With authentication
ab -n 100 -c 10 -A testuser:testpass http://localhost:9090/metrics
```

### Test 3: Memory Usage

```bash
# Monitor Java heap usage
jstat -gc $(pgrep -f manzan) 1000

# Check for memory leaks (run for extended period)
watch -n 60 'ps aux | grep manzan | grep -v grep'
```

---

## Troubleshooting Guide

### Issue: No Metrics Appearing

**Diagnosis:**
```bash
# Check if Manzan is running
ps aux | grep manzan

# Check if port is listening
netstat -tlnp | grep 9090

# Check Manzan logs
tail -f /var/log/manzan/manzan.log
```

**Solutions:**
1. Verify configuration files are correct
2. Check data sources are configured
3. Ensure events are being generated
4. Restart Manzan

### Issue: Authentication Failures

**Diagnosis:**
```bash
# Test without auth
curl http://localhost:9090/metrics

# Test with auth
curl -u username:password http://localhost:9090/metrics

# Check for 401 responses
curl -I http://localhost:9090/metrics
```

**Solutions:**
1. Verify credentials in dests.ini
2. Check credentials in Prometheus config
3. Ensure Authorization header is being sent

### Issue: Metrics Not Updating

**Diagnosis:**
```bash
# Check event processing
curl -s http://localhost:9090/metrics | grep events_total

# Wait and check again
sleep 30
curl -s http://localhost:9090/metrics | grep events_total
```

**Solutions:**
1. Verify data sources are active
2. Check poll intervals
3. Ensure IBM i connection is working
4. Review event filters

### Issue: High Memory Usage

**Diagnosis:**
```bash
# Check Java heap
jmap -heap $(pgrep -f manzan)

# Check metric cardinality
curl -s http://localhost:9090/metrics | grep -c "^[a-z]"
```

**Solutions:**
1. Reduce label cardinality
2. Increase JVM heap size
3. Reduce scrape frequency
4. Limit number of data sources

---

## Validation Checklist

### Functional Testing
- [ ] Metrics endpoint accessible
- [ ] Metrics in correct Prometheus format
- [ ] All metric types present (counter, gauge, histogram)
- [ ] Labels are correct
- [ ] Metrics increment over time
- [ ] Authentication works (if enabled)
- [ ] Different event types create appropriate metrics

### Performance Testing
- [ ] Response time < 1 second
- [ ] Memory usage stable
- [ ] No memory leaks
- [ ] Handles concurrent requests
- [ ] Scales with event volume

### Security Testing
- [ ] Authentication required (if configured)
- [ ] HTTPS working (production)
- [ ] IP whitelisting effective
- [ ] Rate limiting functional
- [ ] No sensitive data in metrics
- [ ] Configuration files secured

### Integration Testing
- [ ] Prometheus successfully scrapes metrics
- [ ] Metrics appear in Prometheus UI
- [ ] Queries return expected results
- [ ] Alerts trigger correctly
- [ ] Grafana dashboards work

---

## Next Steps

After successful testing:

1. **Document Your Configuration**
   - Save working configurations
   - Document any customizations
   - Create runbooks for operations

2. **Set Up Monitoring**
   - Configure Prometheus alerts
   - Create Grafana dashboards
   - Set up notification channels

3. **Plan Maintenance**
   - Schedule credential rotation
   - Plan for certificate renewal
   - Set up backup procedures

4. **Train Operations Team**
   - Share documentation
   - Conduct training sessions
   - Create troubleshooting guides

---

## Additional Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [PromQL Query Language](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Dashboards](https://grafana.com/docs/)
- [Manzan Prometheus Configuration](docs/config/examples/prometheus.md)
- [Security Best Practices](PROMETHEUS_SECURITY_REPORT.md)