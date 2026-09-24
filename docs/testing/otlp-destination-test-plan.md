# OTLP Destination — Tester's Test Plan

**Document Purpose**: Step-by-step test plan for validating the `otlp` destination in Manzan.  
**Audience**: Testers who are new to Manzan. No prior knowledge of the OTLP protocol or OpenTelemetry is assumed.  
**Last Updated**: 2026-08-18  
**Build Under Test**: Branch `multInstance`

---

## 1. Background for New Testers

### What is Manzan?

Manzan is an IBM i application that watches IBM i system events (message queues, audit journals, file activity, etc.) and forwards them to external destinations. It runs as a Java process on IBM i using Apache Camel routing. Configuration lives in three INI files per instance:

| File | Purpose |
|---|---|
| `app.ini` | IBM i connection credentials and general settings |
| `data.ini` | **Event sources** — what to watch (message queue, file, HTTP endpoint, etc.) |
| `dests.ini` | **Destinations** — where to send events (Kafka, Slack, Elasticsearch, OTLP, etc.) |

Both files live in `/QOpenSys/etc/manzan-default/` for the default instance.

### What is the OTLP destination?

OTLP stands for **OpenTelemetry Protocol**. When the `otlp` destination type is configured, Manzan forwards every IBM i event it receives as a structured log record to an **OTLP-compatible collector** — for example Grafana Agent, OpenTelemetry Collector, Datadog Agent, or Dynatrace OneAgent.

The destination posts to the collector's HTTP endpoint (`/v1/logs`) using the standard protobuf-over-HTTP format. Each event field (message text, severity, job name, timestamp, etc.) is sent as a named attribute on the log record.

### What changed in this release?

The `otlp` destination was rewritten from scratch. Key differences:

| Aspect | Before | After |
|---|---|---|
| Config key | `url` | `endpoint` |
| Transport | Single HTTP call per message via Camel HTTP component | OTel SDK exporter with built-in batching, retry, and connection pool |
| Auth | None | `headers` key: any HTTP header(s), including `Authorization: Bearer ...` |
| TLS | System default only | Custom CA cert (`caCertPath`) and mutual TLS (`clientCertPath`/`clientKeyPath`) |
| Batch control | None | `batchSize`, `batchTimeoutMs`, `timeoutMs` |
| Retry on failure | None | Built-in exponential backoff by OTel SDK |
| Severity mapping | Hardcoded string fragments | Proper OTLP severity number (INFO / ERROR / FATAL) per event type |

> **Note for testers**: The config key for the endpoint changed from `url` to `endpoint`. Any existing `dests.ini` with `url=` under an `[otlp]` section must be updated.

---

## 2. Concepts You Need to Know

| Term | Meaning |
|---|---|
| **OTLP Collector** | A server that accepts log/trace/metric data in OpenTelemetry Protocol format. In testing, you will run one locally using Docker. |
| **`/v1/logs` endpoint** | The HTTP path the OTLP exporter posts log records to. |
| **Batch** | Manzan accumulates events and sends them together in a single HTTP request when either `batchSize` records are ready or `batchTimeoutMs` milliseconds have elapsed, whichever comes first. |
| **mTLS** | Mutual TLS: both the client (Manzan) and server (collector) present certificates to authenticate each other. |
| **`data.ini` event source** | The watch definition that generates events. For testing we use a `file` watch, which triggers when a file changes. This avoids needing a live IBM i message queue. |
| **`dests.ini` destination** | The OTLP destination block that receives events from the watch and forwards them to the collector. |

---

## 3. Pre-Test Setup

### 3.1 Prerequisites

- [ ] Access to an IBM i system (PASE shell) with Manzan installed
- [ ] A Windows machine reachable from IBM i (your laptop or workstation is fine)
- [ ] A text editor to modify the INI files
- [ ] Basic familiarity with the PASE shell (`cd`, `cat`, `echo`, `kill`)
- [ ] For TLS tests (Section F): `openssl` CLI available on any machine

### 3.2 Find your Windows IP address

Manzan runs on IBM i and makes outbound HTTP connections to your Windows machine. You need to know the Windows IP address as seen from IBM i.

**Run this in PowerShell on Windows:**

```powershell
Get-NetIPAddress -AddressFamily IPv4 |
    Select-Object InterfaceAlias, IPAddress |
    Format-Table -AutoSize
```

Example output:

```
InterfaceAlias                IPAddress
--------------                ---------
Ethernet                      192.168.1.45
Wi-Fi                         10.0.0.23
VPN - Corporate               172.16.4.88
Loopback Pseudo-Interface 1   127.0.0.1
```

Pick the address on the same network as IBM i. If you are unsure which one, run:

```powershell
# Replace with your IBM i's actual IP address
Find-NetRoute -RemoteIPAddress <ibmi-ip> | Select-Object InterfaceAlias, LocalAddress
```

This tells you exactly which adapter IBM i will connect through.

> **Do not use `127.0.0.1`** — that only works locally on Windows itself and IBM i cannot reach it.

Throughout the rest of this document, replace `<collector-host>` with the Windows IP address you identified here.

### 3.3 Open the Windows firewall

Windows blocks inbound connections by default. Run the following in **PowerShell as Administrator** to allow IBM i to reach the collector:

```powershell
New-NetFirewallRule -DisplayName "OTLP Collector (Manzan)" `
    -Direction Inbound `
    -Protocol TCP `
    -LocalPort 4318 `
    -Action Allow
```

Remove the rule when you are finished testing:

```powershell
Remove-NetFirewallRule -DisplayName "OTLP Collector (Manzan)"
```

### 3.4 Install and start the OTLP collector on Windows

All integration tests in this plan use the **OpenTelemetry Collector** running directly on Windows as a standalone binary — no Docker required. It accepts OTLP/HTTP requests and prints every received log record to the terminal so you can see exactly what Manzan sends.

**Step 1 — Download the collector binary** (run in PowerShell):

```powershell
# Create a working directory
New-Item -ItemType Directory -Force -Path C:\otel

# Download the Windows AMD64 binary
Invoke-WebRequest `
  -Uri "https://github.com/open-telemetry/opentelemetry-collector-releases/releases/download/v0.102.1/otelcol-contrib_0.102.1_windows_amd64.tar.gz" `
  -OutFile "C:\otel\otelcol.tar.gz"

# Extract (tar is built into Windows 10 and later)
tar -xzf "C:\otel\otelcol.tar.gz" -C "C:\otel"
```

**Step 2 — Create the collector config file** — save as `C:\otel\config.yaml`:

```yaml
receivers:
  otlp:
    protocols:
      http:
        # 0.0.0.0 means "accept connections from any address"
        # This is required — localhost would only accept connections from Windows itself
        endpoint: "0.0.0.0:4318"

exporters:
  debug:
    # detailed prints every field of every log record Manzan sends
    verbosity: detailed

service:
  pipelines:
    logs:
      receivers: [otlp]
      exporters: [debug]
```

**Step 3 — Start the collector** (leave this terminal open — output appears here):

```powershell
& "C:\otel\otelcol-contrib.exe" --config "C:\otel\config.yaml"
```

You should see:

```
Everything is ready. Begin running and processing data.
```

**Step 4 — Verify the collector is reachable from Windows** (open a second PowerShell window — do not close the collector terminal):

```powershell
Invoke-WebRequest -Uri "http://localhost:4318/v1/logs" `
    -Method POST `
    -Headers @{ "Content-Type" = "application/json" } `
    -Body "{}" `
    -UseBasicParsing
```

Expected: you get a response back (any HTTP status code — even `400 Bad Request` is fine). What you must **not** see is a connection error. A response of any kind confirms the collector is listening.

You should also see a line appear in the collector terminal — something like:

```
{"kind": "exporter", "data_type": "logs", "name": "debug"}
```

**Step 5 — Verify IBM i can reach the collector** (run from IBM i PASE):

```bash
curl -s http://<collector-host>:4318/v1/logs \
     -H "Content-Type: application/json" \
     -d '{}'
# Expected: any response (even an error body) — a connection refused means
# the firewall rule is not in place yet
```

### 3.5 Configure Manzan

Edit `/QOpenSys/etc/manzan-default/dests.ini` and add:

```ini
[otlp_test]
type          = otlp
endpoint      = http://<collector-host>:4318/v1/logs
batchSize     = 1
batchTimeoutMs = 500
```

Setting `batchSize = 1` means every event is sent immediately without waiting. This makes testing faster.

Edit `/QOpenSys/etc/manzan-default/data.ini` and add a file watch (does not require a live IBM i message queue):

```ini
[file_trigger]
type         = file
file         = /tmp/manzan-test-input.txt
destinations = otlp_test
interval     = 1000
```

Create the trigger file:

```bash
touch /tmp/manzan-test-input.txt
```

### 3.6 Start Manzan

```bash
java -jar /opt/manzan/manzan.jar &
sleep 3
```

Manzan should print startup messages and begin watching `/tmp/manzan-test-input.txt`. Keep the collector PowerShell terminal visible — test results will appear there.

---

## 4. Test Cases

Each test case follows this format:

> **TC-nn — Title**  
> **Preconditions** | **Steps** | **Expected result** | **Pass/Fail**

---

### Section A — Basic Connectivity

---

**TC-01 — Event reaches the OTLP collector**

**Preconditions**: Manzan is running with the config from §3.3. The Docker collector is running and its log output is visible.

**Steps**:
```bash
echo "Hello from Manzan" >> /tmp/manzan-test-input.txt
sleep 2
```

Then check the collector's Docker terminal output.

**Expected result**: Within 2 seconds, the collector prints a log entry containing:
- `ScopeName: manzan.otlp_test`
- An attribute with key `FILE_DATA` and value `Hello from Manzan`
- An attribute with key `FILE_NAME` containing `manzan-test-input.txt`

**Pass / Fail**: ___

---

**TC-02 — Multiple events in sequence each reach the collector**

**Steps**:
```bash
echo "event one"   >> /tmp/manzan-test-input.txt
sleep 1
echo "event two"   >> /tmp/manzan-test-input.txt
sleep 1
echo "event three" >> /tmp/manzan-test-input.txt
sleep 2
```

**Expected result**: Three separate log records appear in the collector output, one for each `echo`. Each contains the correct `FILE_DATA` value. None are missing or duplicated.

**Pass / Fail**: ___

---

**TC-03 — Collector unavailable at startup: Manzan starts without error**

**Preconditions**: Stop the Docker collector (`Ctrl+C`). Manzan is not running.

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar &
sleep 3
```

**Expected result**: Manzan starts successfully and prints its normal startup messages. It does **not** crash or print an uncaught exception, even though the collector is unreachable. (Events will be queued internally by the OTel SDK exporter.)

**Pass / Fail**: ___

---

**TC-04 — Events delivered after collector recovers**

**Preconditions**: TC-03 — Manzan is running; collector is stopped.

**Steps**:
```bash
# Trigger an event while the collector is down
echo "sent while down" >> /tmp/manzan-test-input.txt
sleep 2

# Restart the collector
docker run --rm \
  -p 4318:4318 \
  -v /tmp/otel-collector-config.yaml:/etc/otel-collector-config.yaml \
  otel/opentelemetry-collector-contrib:latest \
  --config /etc/otel-collector-config.yaml &

sleep 10
```

**Expected result**: After the collector restarts, the queued event (`sent while down`) is delivered. The `FILE_DATA` attribute shows the correct value. No data is lost.

> **Note**: The OTel SDK exporter retries failed exports with exponential backoff up to its internal queue limit.

**Pass / Fail**: ___

---

### Section B — Authentication

---

**TC-05 — Bearer token forwarded to collector**

**Preconditions**: Stop Manzan. Update `dests.ini`:

```ini
[otlp_test]
type          = otlp
endpoint      = http://<collector-host>:4318/v1/logs
headers       = Authorization: Bearer mysecrettoken
batchSize     = 1
batchTimeoutMs = 500
```

Start a **token-validating** collector. Update `C:\otel\config.yaml` to this (or save as a separate file and pass it with `--config`):

```yaml
receivers:
  otlp:
    protocols:
      http:
        endpoint: "0.0.0.0:4318"

exporters:
  debug:
    verbosity: detailed

service:
  pipelines:
    logs:
      receivers: [otlp]
      exporters: [debug]
```

For this test, use a network inspector (e.g. `tcpdump` or `tshark` on the collector host) to capture the HTTP request and verify the `Authorization` header is present:

```bash
# On the collector host, in a separate terminal
tcpdump -i any -A port 4318 2>/dev/null | grep -A2 "Authorization"
```

Restart Manzan:
```bash
java -jar /opt/manzan/manzan.jar &
sleep 3
echo "auth test" >> /tmp/manzan-test-input.txt
sleep 2
```

**Expected result**: The captured traffic shows `Authorization: Bearer mysecrettoken` in the HTTP request headers. The collector receives the log record.

**Pass / Fail**: ___

---

**TC-06 — Multiple custom headers forwarded**

**Preconditions**: Update `dests.ini`:

```ini
[otlp_test]
type          = otlp
endpoint      = http://<collector-host>:4318/v1/logs
headers       = Authorization: Bearer abc123, X-Tenant-ID: acme-corp
batchSize     = 1
batchTimeoutMs = 500
```

**Steps**: Restart Manzan. Trigger an event. Inspect the HTTP traffic (same `tcpdump` method as TC-05).

**Expected result**: Both `Authorization: Bearer abc123` **and** `X-Tenant-ID: acme-corp` appear in the request. The collector receives the log record.

**Pass / Fail**: ___

---

### Section C — Batching

---

**TC-07 — Events are batched when `batchSize > 1`**

**Preconditions**: Update `dests.ini`:

```ini
[otlp_test]
type          = otlp
endpoint      = http://<collector-host>:4318/v1/logs
batchSize     = 5
batchTimeoutMs = 30000
```

`batchTimeoutMs = 30000` (30 seconds) ensures the timeout does not flush the batch before you send the 5th event.

Restart Manzan.

**Steps**:
```bash
for i in 1 2 3 4; do
    echo "batch event $i" >> /tmp/manzan-test-input.txt
    sleep 0.5
done
# Pause — no export should have happened yet
sleep 3
echo "batch event 5" >> /tmp/manzan-test-input.txt
sleep 2
```

**Expected result**:
- After events 1–4: **no** log records appear in the collector output.
- After event 5: all 5 records arrive at the collector in a single HTTP request (the collector output shows them together with the same timestamp cluster).

**Pass / Fail**: ___

---

**TC-08 — Timeout flushes a partial batch**

**Preconditions**: Update `dests.ini`:

```ini
[otlp_test]
type          = otlp
endpoint      = http://<collector-host>:4318/v1/logs
batchSize     = 100
batchTimeoutMs = 3000
```

Restart Manzan.

**Steps**:
```bash
echo "timeout flush test" >> /tmp/manzan-test-input.txt
# Wait longer than batchTimeoutMs
sleep 5
```

**Expected result**: Within approximately 3 seconds of the event being written, the log record arrives at the collector — even though `batchSize = 100` has not been reached. The `FILE_DATA` attribute shows `timeout flush test`.

**Pass / Fail**: ___

---

### Section D — Severity Mapping

---

**TC-09 — Message severity below threshold maps to INFO**

**Preconditions**: Restore `batchSize = 1` in `dests.ini`. Ensure the event source is a `watch` type connected to a message queue (requires a live IBM i watch; skip to TC-11 if using only a file watch).

Send a message with severity 10 (informational) to the watched queue:

```
SNDMSG MSG('Test INFO message') TOMSGQ(QSYSOPR) MSGTYPE(*INFO)
```

**Expected result**: The collector log record shows `SeverityText: INFO` and a severity number of `9`.

**Pass / Fail**: ___

---

**TC-10 — Message severity above threshold (>29) maps to ERROR**

**Steps**: Send a message with severity 50 to the watched queue:

```
SNDMSG MSG('Test ERROR message') TOMSGQ(QSYSOPR) MSGTYPE(*ESCAPE)
```

**Expected result**: The collector log record shows `SeverityText: ERROR` and a severity number of `17`.

**Pass / Fail**: ___

---

**TC-11 — `errorRegex` promotes file event to ERROR severity**

**Preconditions**: Update `dests.ini`:

```ini
[otlp_test]
type          = otlp
endpoint      = http://<collector-host>:4318/v1/logs
batchSize     = 1
batchTimeoutMs = 500
errorRegex    = CRITICAL|SEVERE
```

Restart Manzan.

**Steps**:
```bash
# Write a line that does NOT match the regex — expect INFO
echo "normal log entry" >> /tmp/manzan-test-input.txt
sleep 2

# Write a line that DOES match the regex — expect ERROR
echo "CRITICAL: disk usage at 99%" >> /tmp/manzan-test-input.txt
sleep 2
```

**Expected result**:
- First event: `SeverityText: INFO`
- Second event: `SeverityText: ERROR`

**Pass / Fail**: ___

---

**TC-12 — Invalid `errorRegex` does not crash Manzan**

**Preconditions**: Update `dests.ini`:

```ini
[otlp_test]
type          = otlp
endpoint      = http://<collector-host>:4318/v1/logs
batchSize     = 1
batchTimeoutMs = 500
errorRegex    = [invalid regex
```

**Steps**: Restart Manzan:
```bash
java -jar /opt/manzan/manzan.jar &
sleep 3
echo "test after bad regex" >> /tmp/manzan-test-input.txt
sleep 2
```

**Expected result**: Manzan starts without crashing. A warning is printed to stdout containing `Invalid errorRegex`. Events continue to flow to the collector (with `INFO` severity, since the pattern is disabled). No exception stack trace in the output.

**Pass / Fail**: ___

---

### Section E — Event Fields as Attributes

---

**TC-13 — All data map fields arrive as named attributes**

**Preconditions**: `batchSize = 1`. Using a file watch.

**Steps**:
```bash
echo "attribute field test" >> /tmp/manzan-test-input.txt
sleep 2
```

**Expected result**: The collector output shows a log record with at minimum these attributes:

| Attribute key | Expected value |
|---|---|
| `FILE_NAME` | `manzan-test-input.txt` |
| `FILE_PATH` | `/tmp/manzan-test-input.txt` |
| `FILE_DATA` | `attribute field test` |

Verify by scanning the collector's detailed output for `Attributes:`.

**Pass / Fail**: ___

---

**TC-14 — Numeric attributes arrive as numbers, not strings**

**Preconditions**: Using a `watch` event source on a message queue (requires IBM i watch). If unavailable, this test can be deferred.

**Expected result**: The `SEVERITY` field appears in the collector output as an integer value (e.g. `IntValue: 10`), not as a quoted string (`StringValue: "10"`).

**Pass / Fail**: ___

---

### Section F — TLS / mTLS

> **Setup for this section**: TLS tests require generating certificates. Run the following on any machine with `openssl`. Substitute `<collector-host>` with your collector's IP or hostname.

**Generate test certificates**:

```bash
# Create a self-signed CA
openssl genrsa -out ca.key 4096
openssl req -new -x509 -days 3650 -key ca.key -out ca.crt \
  -subj "/CN=Manzan Test CA"

# Create a server certificate for the collector, signed by our CA
openssl genrsa -out server.key 4096
openssl req -new -key server.key -out server.csr \
  -subj "/CN=<collector-host>"
openssl x509 -req -days 365 -in server.csr \
  -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt

# Create a client certificate for Manzan (mTLS)
openssl genrsa -out client.key 4096
openssl req -new -key client.key -out client.csr \
  -subj "/CN=manzan-client"
openssl x509 -req -days 365 -in client.csr \
  -CA ca.crt -CAkey ca.key -CAcreateserial -out client.crt
```

Copy `ca.crt`, `client.crt`, and `client.key` to IBM i (e.g. to `/QOpenSys/etc/manzan-default/certs/`). Set permissions:

```bash
chmod 600 /QOpenSys/etc/manzan-default/certs/client.key
chmod 644 /QOpenSys/etc/manzan-default/certs/ca.crt
chmod 644 /QOpenSys/etc/manzan-default/certs/client.crt
```

---

**TC-15 — Custom CA cert enables TLS to a self-signed collector**

**Preconditions**: Start the collector with TLS using `server.crt` and `server.key` (consult the OpenTelemetry Collector docs for TLS config). Update `dests.ini`:

```ini
[otlp_test]
type          = otlp
endpoint      = https://<collector-host>:4318/v1/logs
caCertPath    = /QOpenSys/etc/manzan-default/certs/ca.crt
batchSize     = 1
batchTimeoutMs = 500
```

Restart Manzan. Trigger an event:
```bash
echo "tls test" >> /tmp/manzan-test-input.txt
sleep 2
```

**Expected result**: The event arrives at the collector. No TLS certificate error in Manzan's output.

**Pass / Fail**: ___

---

**TC-16 — mTLS: collector rejects client without certificate**

**Preconditions**: Collector configured to require a client certificate (`requireClientCert: true`). Manzan configured with only `caCertPath` — **no** `clientCertPath`.

**Steps**: Trigger an event. Observe Manzan's stdout.

**Expected result**: Manzan's output contains an error indicating the server closed the connection or rejected the handshake. The event does **not** appear in the collector output.

**Pass / Fail**: ___

---

**TC-17 — mTLS: event delivered when both client cert and key are configured**

**Preconditions**: Collector still configured to require a client cert. Update `dests.ini`:

```ini
[otlp_test]
type           = otlp
endpoint       = https://<collector-host>:4318/v1/logs
caCertPath     = /QOpenSys/etc/manzan-default/certs/ca.crt
clientCertPath = /QOpenSys/etc/manzan-default/certs/client.crt
clientKeyPath  = /QOpenSys/etc/manzan-default/certs/client.key
batchSize      = 1
batchTimeoutMs = 500
```

Restart Manzan. Trigger an event:
```bash
echo "mtls test" >> /tmp/manzan-test-input.txt
sleep 2
```

**Expected result**: Event arrives at the collector. `FILE_DATA` is `mtls test`. No TLS errors.

**Pass / Fail**: ___

---

**TC-18 — Providing `clientCertPath` without `clientKeyPath` is rejected at startup**

**Preconditions**: Update `dests.ini` with `clientCertPath` but **no** `clientKeyPath`:

```ini
[otlp_test]
type           = otlp
endpoint       = http://<collector-host>:4318/v1/logs
clientCertPath = /QOpenSys/etc/manzan-default/certs/client.crt
batchSize      = 1
batchTimeoutMs = 500
```

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar
# Do not background — it should exit with an error
```

**Expected result**: Manzan prints an error containing `clientCertPath requires clientKeyPath` and exits with a non-zero code. It does **not** start routing events.

**Pass / Fail**: ___

---

**TC-19 — A non-existent cert file path is rejected at startup**

**Preconditions**: Update `dests.ini`:

```ini
[otlp_test]
type       = otlp
endpoint   = http://<collector-host>:4318/v1/logs
caCertPath = /this/path/does/not/exist.crt
batchSize  = 1
batchTimeoutMs = 500
```

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar
```

**Expected result**: Error message containing `Cannot read caCertPath` and the missing path. Exit with non-zero code. Manzan does not start.

**Pass / Fail**: ___

---

### Section G — Configuration Validation

---

**TC-20 — Missing `endpoint` key prevents startup**

**Preconditions**: Update `dests.ini` — omit `endpoint`:

```ini
[otlp_test]
type          = otlp
batchSize     = 1
batchTimeoutMs = 500
```

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar
```

**Expected result**: Startup fails with an error message containing `endpoint` and `[otlp_test]`. Exit non-zero.

**Pass / Fail**: ___

---

**TC-21 — `enabled = false` disables the destination**

**Preconditions**: Update `dests.ini`:

```ini
[otlp_test]
type          = otlp
endpoint      = http://<collector-host>:4318/v1/logs
enabled       = false
batchSize     = 1
batchTimeoutMs = 500
```

**Steps**: Restart Manzan. Trigger an event:
```bash
echo "disabled test" >> /tmp/manzan-test-input.txt
sleep 3
```

**Expected result**: No log record appears in the collector output. Manzan starts without error.

**Pass / Fail**: ___

---

## 5. Cleanup

After all tests are complete:

**On IBM i** (PASE shell):

```bash
# Stop Manzan
kill $(cat /var/run/manzan/manzan-default.lock) 2>/dev/null

# Remove the test trigger file
rm -f /tmp/manzan-test-input.txt
```

**On Windows** (PowerShell):

```powershell
# Stop the collector — press Ctrl+C in its terminal, or close the window

# Remove the firewall rule
Remove-NetFirewallRule -DisplayName "OTLP Collector (Manzan)"
```

Restore `/QOpenSys/etc/manzan-default/dests.ini` to its pre-test content.

---

## 6. Test Execution Checklist

| TC | Title | Result | Tester | Date |
|---|---|---|---|---|
| TC-01 | Event reaches collector | | | |
| TC-02 | Multiple events in sequence | | | |
| TC-03 | Collector unavailable at startup | | | |
| TC-04 | Events delivered after recovery | | | |
| TC-05 | Bearer token forwarded | | | |
| TC-06 | Multiple headers forwarded | | | |
| TC-07 | Events batched when batchSize > 1 | | | |
| TC-08 | Timeout flushes partial batch | | | |
| TC-09 | Severity below threshold → INFO | | | |
| TC-10 | Severity above threshold → ERROR | | | |
| TC-11 | errorRegex promotes to ERROR | | | |
| TC-12 | Invalid errorRegex does not crash | | | |
| TC-13 | All fields arrive as attributes | | | |
| TC-14 | Numeric attributes as numbers | | | |
| TC-15 | Custom CA cert for TLS | | | |
| TC-16 | mTLS: server rejects missing cert | | | |
| TC-17 | mTLS: event delivered with cert | | | |
| TC-18 | clientCertPath without key rejected | | | |
| TC-19 | Non-existent cert file rejected | | | |
| TC-20 | Missing endpoint prevents startup | | | |
| TC-21 | enabled=false disables destination | | | |

---

## 7. Known Limitations & Notes

- **Batch delivery timing**: Tests TC-07 and TC-08 depend on timing. If the IBM i system is under load, allow up to 2× the expected delay before marking a failure.
- **IBM i network routing**: IBM i makes outbound connections to your Windows machine on port 4318. If `curl` from IBM i to the collector fails, check: (1) the Windows firewall rule in §3.3 is present, (2) you are using the correct Windows IP — not `127.0.0.1` — and (3) both machines are on the same network or VPN segment.
- **VPN**: If your Windows machine is on a corporate VPN, run `Get-NetIPAddress` again while connected to the VPN and use the VPN-assigned IP. The IBM i may be routed through the VPN adapter.
- **TLS tests require openssl**: Tests TC-15 through TC-19 require generating certificates. If `openssl` is not available on Windows, install it via `winget install ShiningLight.OpenSSL` or use Git Bash which bundles it.
- **Collector log verbosity**: The `verbosity: detailed` setting in the collector config is required to see individual attributes. With `verbosity: normal` you will only see that a record arrived, not its contents.
- **Collector binary version**: The download URL in §3.4 pins version `0.102.1`. Check [the releases page](https://github.com/open-telemetry/opentelemetry-collector-releases/releases) for a newer version if the link is broken, and update the filename accordingly.
- **Old `url` key**: If an existing config uses `url=` instead of `endpoint=`, Manzan will fail to start with a missing-required-key error. Update the key as part of the upgrade.

---

## 8. Defect Reporting

When logging a defect, include:

1. TC number and title
2. Full `dests.ini` content (redact any real credentials)
3. Full stdout/stderr from the Manzan process (`java -jar /opt/manzan/manzan.jar` without `&` to capture output)
4. Collector terminal output at the time of the failure (copy/paste from the PowerShell window)
5. Output of `curl -v http://<collector-host>:4318/v1/logs -d '{}'` run from IBM i PASE
6. IBM i OS version (`DSPSYSVAL SYSVAL(QOSLEVEL)`) and Java version (`java -version`)
7. Output of `Get-NetFirewallRule -DisplayName "OTLP Collector (Manzan)"` from Windows PowerShell
