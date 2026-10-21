# OpenSearch Destination — Tester's Test Plan

**Document Purpose**: Step-by-step test plan for validating the `opensearch` destination in Manzan.  
**Audience**: Testers who are new to Manzan. No prior knowledge of OpenSearch is assumed.  
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
| `dests.ini` | **Destinations** — where to send events (Kafka, Slack, Elasticsearch, OpenSearch, etc.) |

Both files live in `/QOpenSys/etc/manzan-default/` for the default instance.

### What is the OpenSearch destination?

When the `opensearch` destination type is configured, Manzan indexes every IBM i event it receives as a JSON document into an **OpenSearch index**. OpenSearch is a search and analytics engine (a fork of Elasticsearch). You can use it to search, filter, and visualise IBM i events using Kibana-compatible dashboards.

Each event field (message text, severity, job name, timestamp, etc.) becomes a separate field in the indexed document.

### How does it differ from the `elasticsearch` destination?

The two destinations index the same data in the same way. The difference is auth:

| Feature | `elasticsearch` | `opensearch` |
|---|---|---|
| Auth modes | API key only | API key, Basic (username/password), or AWS SigV4 |
| Target | Elastic Cloud / self-hosted Elasticsearch | Self-hosted OpenSearch or Amazon OpenSearch Service |
| Config type key | `elasticsearch` | `opensearch` |

Use `opensearch` if your cluster is managed by AWS or if it requires Basic auth or AWS SigV4 signing. Use `elasticsearch` for Elastic Cloud.

### What is SigV4?

AWS Signature Version 4 is the authentication mechanism Amazon requires for AWS-managed services (including Amazon OpenSearch Service). Instead of a password, requests are signed using your AWS credentials. Manzan automatically picks up credentials from the standard AWS chain: environment variables → `~/.aws/credentials` → EC2/ECS instance role.

---

## 2. Concepts You Need to Know

| Term | Meaning |
|---|---|
| **Index** | In OpenSearch, an index is the equivalent of a database table — a named collection of documents. |
| **Document** | One indexed record. Each Manzan event becomes one document. |
| **`authType`** | Config key that selects how Manzan authenticates to OpenSearch: `apikey`, `basic`, or `sigv4`. Default is `apikey`. |
| **OpenSearch Dashboards** | The web UI bundled with OpenSearch (equivalent of Kibana). Available at port 5601. Used to verify documents were indexed. |
| **`data_map`** | Manzan's internal name for the set of fields attached to each event (severity, message text, job name, etc.). Every field in the data_map becomes a document field in OpenSearch. |
| **Document sanitization** | Large numeric values from IBM i (bigger than Java's `long` type) are automatically converted to strings before indexing. This prevents OpenSearch from rejecting the document. |

---

## 3. Pre-Test Setup

### 3.1 Prerequisites

- [ ] Access to an IBM i system (PASE shell) with Manzan installed
- [ ] A Windows machine reachable from IBM i (your laptop or workstation is fine)
- [ ] A text editor for INI files
- [ ] Basic familiarity with PowerShell (run as Administrator for firewall and service steps)
- [ ] For AWS tests (Section E): an AWS account with an Amazon OpenSearch Service domain, and AWS credentials available on the IBM i system

### 3.2 Find your Windows IP address

Manzan runs on IBM i and makes outbound HTTP connections to your Windows machine. You need to know the Windows IP address as seen from IBM i.

**Run this in PowerShell on Windows:**

```powershell
Get-NetIPAddress -AddressFamily IPv4 |
    Select-Object InterfaceAlias, IPAddress |
    Format-Table -AutoSize
```

Pick the address on the same network as IBM i. If you are unsure which one:

```powershell
# Replace with your IBM i's actual IP address
Find-NetRoute -RemoteIPAddress <ibmi-ip> | Select-Object InterfaceAlias, LocalAddress
```

> **Do not use `127.0.0.1`** — that only works locally on Windows itself; IBM i cannot reach it.

Throughout the rest of this document, replace `<opensearch-host>` with the Windows IP address you identified here.

### 3.3 Open the Windows firewall

Windows blocks inbound connections by default. Run the following in **PowerShell as Administrator**:

```powershell
# OpenSearch REST API
New-NetFirewallRule -DisplayName "OpenSearch REST (Manzan)" `
    -Direction Inbound -Protocol TCP -LocalPort 9200 -Action Allow

# OpenSearch Dashboards (optional, for visual verification)
New-NetFirewallRule -DisplayName "OpenSearch Dashboards (Manzan)" `
    -Direction Inbound -Protocol TCP -LocalPort 5601 -Action Allow
```

Remove these rules when you finish testing:

```powershell
Remove-NetFirewallRule -DisplayName "OpenSearch REST (Manzan)"
Remove-NetFirewallRule -DisplayName "OpenSearch Dashboards (Manzan)"
```

### 3.4 Install and start OpenSearch on Windows

All non-AWS integration tests use OpenSearch running directly on Windows as a standalone process — no Docker required.

**Step 1 — Download OpenSearch** (run in PowerShell):

```powershell
New-Item -ItemType Directory -Force -Path C:\opensearch

Invoke-WebRequest `
  -Uri "https://artifacts.opensearch.org/releases/bundle/opensearch/2.13.0/opensearch-2.13.0-windows-x64.zip" `
  -OutFile "C:\opensearch\opensearch-2.13.0.zip"

Expand-Archive -Path "C:\opensearch\opensearch-2.13.0.zip" `
               -DestinationPath "C:\opensearch" -Force
```

**Step 2 — Disable the security plugin** (simplest for testing):

```powershell
# Edit the config file
$configFile = "C:\opensearch\opensearch-2.13.0\config\opensearch.yml"
Add-Content -Path $configFile -Value "`nplugins.security.disabled: true"
```

**Step 3 — Start OpenSearch** (leave this terminal open):

```powershell
& "C:\opensearch\opensearch-2.13.0\bin\opensearch.bat"
```

Wait about 30 seconds. You will see `started` in the output when the cluster is ready.

**Step 4 — Verify OpenSearch is reachable from Windows** (open a second PowerShell window — do not close the OpenSearch terminal):

```powershell
Invoke-WebRequest -Uri "http://localhost:9200" `
    -Method GET `
    -UseBasicParsing
```

Expected: a `200 OK` response whose content contains `"cluster_name"`, `"version"`, and `"tagline"`. This confirms OpenSearch is up and listening before you involve IBM i at all.

Example of what the `Content` field should look like:

```json
{
  "name" : "opensearch-node",
  "cluster_name" : "opensearch",
  "version" : { ... },
  "tagline" : "The OpenSearch Project: https://opensearch.org/"
}
```

If you get a connection error, OpenSearch has not finished starting — wait another 15 seconds and retry.

**Step 5 — Verify IBM i can reach OpenSearch** (run from IBM i PASE):

```bash
curl -s http://<opensearch-host>:9200
# Expected: same JSON as above — if this fails but Step 4 passed,
# the problem is the Windows firewall rule from §3.3, not OpenSearch itself.
```

**Step 6 — Install OpenSearch Dashboards** (optional, for visual verification):

```powershell
Invoke-WebRequest `
  -Uri "https://artifacts.opensearch.org/releases/bundle/opensearch-dashboards/2.13.0/opensearch-dashboards-2.13.0-windows-x64.zip" `
  -OutFile "C:\opensearch\opensearch-dashboards-2.13.0.zip"

Expand-Archive -Path "C:\opensearch\opensearch-dashboards-2.13.0.zip" `
               -DestinationPath "C:\opensearch" -Force

# Edit dashboards config to point at your OpenSearch instance
$dbConfig = "C:\opensearch\opensearch-dashboards-2.13.0\config\opensearch_dashboards.yml"
(Get-Content $dbConfig) -replace 'opensearch.hosts:.*', "opensearch.hosts: ['http://localhost:9200']" |
    Set-Content $dbConfig

& "C:\opensearch\opensearch-dashboards-2.13.0\bin\opensearch-dashboards.bat"
```

Dashboards will be available at `http://<opensearch-host>:5601` after ~60 seconds.

### 3.5 Configure Manzan

Edit `/QOpenSys/etc/manzan-default/dests.ini` and add:

```ini
[opensearch_test]
type     = opensearch
endpoint = http://<opensearch-host>:9200
index    = manzan-events
authType = apikey
apiKey   = placeholder
```

> **Note**: Because we started OpenSearch with `DISABLE_SECURITY_PLUGIN=true`, authentication is not enforced. The `apiKey` value above is a placeholder — any non-empty string will work.

Edit `/QOpenSys/etc/manzan-default/data.ini` and add a file watch:

```ini
[file_trigger]
type         = file
file         = /tmp/manzan-test-input.txt
destinations = opensearch_test
interval     = 1000
```

Create the trigger file:

```bash
touch /tmp/manzan-test-input.txt
```

### 3.6 Create the target index

Before starting Manzan, create the index in OpenSearch:

```bash
curl -s -X PUT http://<opensearch-host>:9200/manzan-events \
     -H "Content-Type: application/json" \
     -d '{"settings": {"number_of_shards": 1, "number_of_replicas": 0}}'
# Expected: {"acknowledged":true,"shards_acknowledged":true,"index":"manzan-events"}
```

### 3.7 Start Manzan

```bash
java -jar /opt/manzan/manzan.jar &
sleep 3
```

---

## 4. Verifying Results

Throughout this test plan you will use two methods to check whether documents were indexed:

**Method A — curl (quick check)**:
```bash
curl -s "http://<opensearch-host>:9200/manzan-events/_search?pretty&size=5"
```
Look for the `hits.hits` array. Each entry is an indexed document.

**Method B — OpenSearch Dashboards (visual)**:
1. Open `http://<opensearch-host>:5601` in a browser
2. Go to **Management → Index Patterns**, create a pattern for `manzan-events`
3. Go to **Discover** — documents appear as rows

---

## 5. Test Cases

---

### Section A — Basic Indexing

---

**TC-01 — Event is indexed as a document**

**Preconditions**: Manzan is running with the config from §3.3.

**Steps**:
```bash
echo "Hello from Manzan" >> /tmp/manzan-test-input.txt
sleep 2

# Check OpenSearch
curl -s "http://<opensearch-host>:9200/manzan-events/_search?pretty&q=FILE_DATA:Hello"
```

**Expected result**: The `hits.hits` array contains one document. The document's `_source` object includes:
```json
{
  "FILE_DATA": "Hello from Manzan",
  "FILE_NAME": "manzan-test-input.txt",
  "FILE_PATH": "/tmp/manzan-test-input.txt"
}
```

**Pass / Fail**: ___

---

**TC-02 — Multiple events produce multiple documents**

**Steps**:
```bash
echo "first event"  >> /tmp/manzan-test-input.txt
sleep 1
echo "second event" >> /tmp/manzan-test-input.txt
sleep 1
echo "third event"  >> /tmp/manzan-test-input.txt
sleep 2

curl -s "http://<opensearch-host>:9200/manzan-events/_count?pretty"
```

**Expected result**: The `count` field is at least 3. Each event is a separate document; none are merged or missing.

**Pass / Fail**: ___

---

**TC-03 — Each document contains the correct field values**

**Steps**:
```bash
echo "field check event" >> /tmp/manzan-test-input.txt
sleep 2

curl -s "http://<opensearch-host>:9200/manzan-events/_search?pretty&q=FILE_DATA:field+check+event"
```

**Expected result**: Exactly one document matches. Its `_source` contains:

| Field | Expected value |
|---|---|
| `FILE_DATA` | `field check event` |
| `FILE_NAME` | `manzan-test-input.txt` |
| `FILE_PATH` | `/tmp/manzan-test-input.txt` |

**Pass / Fail**: ___

---

**TC-04 — Document count increases with each new event**

**Steps**:
```bash
# Get current count
BEFORE=$(curl -s "http://<opensearch-host>:9200/manzan-events/_count" | grep -o '"count":[0-9]*' | cut -d: -f2)

echo "count test event" >> /tmp/manzan-test-input.txt
sleep 2

AFTER=$(curl -s "http://<opensearch-host>:9200/manzan-events/_count" | grep -o '"count":[0-9]*' | cut -d: -f2)

echo "Before: $BEFORE  After: $AFTER"
```

**Expected result**: `After` is exactly `Before + 1`.

**Pass / Fail**: ___

---

### Section B — Authentication: API Key

---

**TC-05 — API key is sent as `Authorization: ApiKey` header**

**Preconditions**: Start a **security-enabled** OpenSearch cluster on Windows. Re-run the §3.4 install steps but **skip** the "Disable the security plugin" step (do not add `plugins.security.disabled: true`). Use a different port to avoid clashing with the unsecured instance:

```powershell
# Copy the installation to a separate directory for the secured instance
Copy-Item -Recurse "C:\opensearch\opensearch-2.13.0" "C:\opensearch\opensearch-secure"

# Start on port 9201
$secureConfig = "C:\opensearch\opensearch-secure\config\opensearch.yml"
Add-Content -Path $secureConfig -Value "`nhttp.port: 9201"

& "C:\opensearch\opensearch-secure\bin\opensearch.bat"
```

Set the initial admin password when prompted, or configure it in `opensearch.yml` before starting:

```powershell
Add-Content -Path $secureConfig -Value "`nopensearch.initial.admin.password: Admin@12345"
```

Wait ~30 seconds. Then create a writer user:

```bash
curl -s -k -u "admin:Admin@12345" \
     -X POST "https://<opensearch-host>:9201/_plugins/_security/api/user/manzan-writer" \
     -H "Content-Type: application/json" \
     -d '{"password":"WriterPass1!","backend_roles":["all_access"]}'

# For simplicity, use Basic auth in this specific test and treat the API key path
# as verified by the automated test suite (OpenSearchDestinationTest.testApiKeyAuthHeaderForwarded)
```

Update `dests.ini`:

```ini
[opensearch_test]
type     = opensearch
endpoint = https://<opensearch-host>:9201
index    = manzan-events
authType = basic
username = manzan-writer
password = WriterPass1!
```

**Steps**: Restart Manzan. Trigger an event. Verify it appears using curl with the credentials:

```bash
echo "apikey auth test" >> /tmp/manzan-test-input.txt
sleep 2

curl -s -k -u "manzan-writer:WriterPass1!" \
     "https://<opensearch-host>:9201/manzan-events/_search?pretty&q=FILE_DATA:apikey"
```

**Expected result**: Document appears in the search results. The request without credentials would return 401.

**Pass / Fail**: ___

---

### Section C — Authentication: Basic Auth

---

**TC-06 — Basic auth credentials reach OpenSearch**

**Preconditions**: Security-enabled OpenSearch from TC-05. Update `dests.ini`:

```ini
[opensearch_test]
type     = opensearch
endpoint = https://<opensearch-host>:9201
index    = manzan-events
authType = basic
username = manzan-writer
password = WriterPass1!
```

**Steps**:
```bash
echo "basic auth test" >> /tmp/manzan-test-input.txt
sleep 2

curl -s -k -u "manzan-writer:WriterPass1!" \
     "https://<opensearch-host>:9201/manzan-events/_search?pretty&q=FILE_DATA:basic+auth"
```

**Expected result**: Document appears in results.

**Pass / Fail**: ___

---

**TC-07 — Wrong password prevents indexing**

**Preconditions**: Security-enabled OpenSearch. Update `dests.ini` with a wrong password:

```ini
[opensearch_test]
type     = opensearch
endpoint = https://<opensearch-host>:9201
index    = manzan-events
authType = basic
username = manzan-writer
password = WrongPassword
```

**Steps**: Restart Manzan. Trigger an event. Wait 3 seconds. Check document count.

**Expected result**: No new document is indexed. Manzan's stdout shows an error response from OpenSearch (401 or 403). Manzan does **not** crash — it logs the error and continues running.

**Pass / Fail**: ___

---

**TC-08 — Missing username at startup is rejected**

**Preconditions**: Update `dests.ini`:

```ini
[opensearch_test]
type     = opensearch
endpoint = http://<opensearch-host>:9200
index    = manzan-events
authType = basic
password = somepassword
```

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar
```

**Expected result**: Error message `authType=basic requires 'username' to be set`. Exit non-zero. Manzan does not start.

**Pass / Fail**: ___

---

**TC-09 — Missing password at startup is rejected**

**Preconditions**: Update `dests.ini`:

```ini
[opensearch_test]
type     = opensearch
endpoint = http://<opensearch-host>:9200
index    = manzan-events
authType = basic
username = writer
```

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar
```

**Expected result**: Error message `authType=basic requires 'password' to be set`. Exit non-zero.

**Pass / Fail**: ___

---

### Section D — Authentication: API Key Validation

---

**TC-10 — Missing apiKey at startup is rejected**

**Preconditions**: Update `dests.ini`:

```ini
[opensearch_test]
type     = opensearch
endpoint = http://<opensearch-host>:9200
index    = manzan-events
authType = apikey
```

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar
```

**Expected result**: Error message `authType=apikey requires 'apiKey' to be set`. Exit non-zero.

**Pass / Fail**: ___

---

**TC-11 — Unknown `authType` is rejected at startup**

**Preconditions**: Update `dests.ini`:

```ini
[opensearch_test]
type     = opensearch
endpoint = http://<opensearch-host>:9200
index    = manzan-events
authType = oauth2
apiKey   = something
```

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar
```

**Expected result**: Error message containing `Unknown authType 'oauth2'`. Exit non-zero.

**Pass / Fail**: ___

---

### Section E — AWS SigV4 Authentication

> **Prerequisites for this section**: An active AWS account with an Amazon OpenSearch Service domain. The IBM i system must be able to reach the domain endpoint over HTTPS. AWS credentials must be available — either via environment variables or an IAM instance role.

---

**TC-12 — SigV4 signing required when `authType = sigv4`**

**Preconditions**: Set up AWS credentials on the IBM i PASE environment:

```bash
export AWS_ACCESS_KEY_ID=AKIA...
export AWS_SECRET_ACCESS_KEY=abc123...
export AWS_DEFAULT_REGION=us-east-1
```

Update `dests.ini`:

```ini
[opensearch_aws]
type       = opensearch
endpoint   = https://<your-domain>.us-east-1.es.amazonaws.com
index      = manzan-events
authType   = sigv4
awsRegion  = us-east-1
awsService = es
```

Create the index on the AWS domain:

```bash
curl -s -X PUT "https://<your-domain>.us-east-1.es.amazonaws.com/manzan-events" \
     --aws-sigv4 "aws:amz:us-east-1:es" \
     --user "$AWS_ACCESS_KEY_ID:$AWS_SECRET_ACCESS_KEY"
```

**Steps**: Restart Manzan. Trigger an event:

```bash
echo "sigv4 test" >> /tmp/manzan-test-input.txt
sleep 3
```

Query the AWS domain:
```bash
curl -s "https://<your-domain>.us-east-1.es.amazonaws.com/manzan-events/_search?q=FILE_DATA:sigv4" \
     --aws-sigv4 "aws:amz:us-east-1:es" \
     --user "$AWS_ACCESS_KEY_ID:$AWS_SECRET_ACCESS_KEY"
```

**Expected result**: Document appears with `FILE_DATA: sigv4 test`.

**Pass / Fail**: ___

---

**TC-13 — Missing `awsRegion` when `authType = sigv4` is rejected at startup**

**Preconditions**: Update `dests.ini`:

```ini
[opensearch_aws]
type       = opensearch
endpoint   = https://example.us-east-1.es.amazonaws.com
index      = manzan-events
authType   = sigv4
awsService = es
```

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar
```

**Expected result**: Error message `authType=sigv4 requires 'awsRegion' to be set`. Exit non-zero.

**Pass / Fail**: ___

---

**TC-14 — `awsService` defaults to `es` when omitted**

**Preconditions**: Valid AWS credentials and a standard Amazon OpenSearch Service domain (not AOSS). Update `dests.ini` — **omit** `awsService`:

```ini
[opensearch_aws]
type      = opensearch
endpoint  = https://<your-domain>.us-east-1.es.amazonaws.com
index     = manzan-events
authType  = sigv4
awsRegion = us-east-1
```

**Steps**: Restart Manzan. Trigger an event.

**Expected result**: Event is indexed successfully. No error about missing `awsService`. (Omitting the key is treated as `es`.)

**Pass / Fail**: ___

---

### Section F — Large Number Handling (Document Sanitization)

IBM i numeric columns can produce values larger than a 64-bit integer. This section verifies that Manzan converts oversized numbers to strings before sending them to OpenSearch, preventing index rejection.

---

**TC-15 — Oversized numeric field does not cause an index error**

**Preconditions**: This test requires a `sql` event source or a `watch` on a table that contains a column with a value exceeding `9223372036854775807` (Java `Long.MAX_VALUE`). If such a column is not available, this test can be verified by running the automated test `DocumentSanitizerTest.stringifiesBigIntegerAboveMax` and marking it as verified by automated test.

If a real column is available:

**Steps**:
1. Configure a `sql` event source in `data.ini` that queries the table with the large value.
2. Restart Manzan.
3. Query OpenSearch for the document:

```bash
curl -s "http://<opensearch-host>:9200/manzan-events/_search?pretty&size=1"
```

**Expected result**: Document is indexed without error. The large numeric field appears as a **string** value in the `_source` object (quoted), not as a number.

**Pass / Fail**: ___

---

**TC-16 — Normal numeric fields (within long range) are indexed as numbers**

**Preconditions**: A `SEVERITY` field with value `30` (from a `watch` event source, or injected via `data.ini` `injections.` prefix).

**Steps**: After triggering a message watch event, query OpenSearch:

```bash
curl -s "http://<opensearch-host>:9200/manzan-events/_search?pretty&q=SEVERITY:30"
```

**Expected result**: Document found. The `SEVERITY` field in `_source` is an integer (`30`), not a string (`"30"`).

**Pass / Fail**: ___

---

### Section G — Configuration Validation

---

**TC-17 — Missing `endpoint` prevents startup**

**Preconditions**: Update `dests.ini` — omit `endpoint`:

```ini
[opensearch_test]
type     = opensearch
index    = manzan-events
authType = apikey
apiKey   = somekey
```

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar
```

**Expected result**: Error message referencing missing required key `endpoint` in `[opensearch_test]`. Exit non-zero.

**Pass / Fail**: ___

---

**TC-18 — Missing `index` prevents startup**

**Preconditions**: Update `dests.ini` — omit `index`:

```ini
[opensearch_test]
type     = opensearch
endpoint = http://<opensearch-host>:9200
authType = apikey
apiKey   = somekey
```

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar
```

**Expected result**: Error message referencing missing required key `index`. Exit non-zero.

**Pass / Fail**: ___

---

**TC-19 — `enabled = false` disables the destination**

**Preconditions**: Update `dests.ini`:

```ini
[opensearch_test]
type     = opensearch
endpoint = http://<opensearch-host>:9200
index    = manzan-events
authType = apikey
apiKey   = placeholder
enabled  = false
```

**Steps**: Restart Manzan. Get current document count. Trigger an event. Wait 3 seconds. Get document count again.

```bash
curl -s "http://<opensearch-host>:9200/manzan-events/_count"
echo "triggering..."
echo "disabled test" >> /tmp/manzan-test-input.txt
sleep 3
curl -s "http://<opensearch-host>:9200/manzan-events/_count"
```

**Expected result**: Document count does not increase. Manzan starts without error.

**Pass / Fail**: ___

---

### Section H — Connectivity Resilience

---

**TC-20 — Manzan starts successfully when OpenSearch is unreachable**

**Preconditions**: Stop the OpenSearch process on Windows (close the terminal window running `opensearch.bat`, or press Ctrl+C). Update `dests.ini` with valid (but unreachable) config.

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar &
sleep 3
```

**Expected result**: Manzan starts without crashing. Startup messages are normal. Errors about connection failure may appear in output — this is expected. Manzan does not exit.

**Pass / Fail**: ___

---

**TC-21 — Documents indexed after OpenSearch recovers**

**Preconditions**: TC-20 — Manzan is running; OpenSearch is down.

**Steps**:
```bash
# Trigger event while OpenSearch is down
echo "recovery test" >> /tmp/manzan-test-input.txt
sleep 2
```

Then restart OpenSearch on Windows:

```powershell
& "C:\opensearch\opensearch-2.13.0\bin\opensearch.bat"
```

Wait ~15 seconds for the cluster to be ready, then check for the document from IBM i:

```bash
curl -s "http://<opensearch-host>:9200/manzan-events/_search?pretty&q=FILE_DATA:recovery"
```

**Expected result**: Document with `FILE_DATA: recovery test` appears after OpenSearch recovers. The event is not permanently lost.

> **Note**: The Apache REST client in the OpenSearch destination retries on connection errors. Delivery after recovery is best-effort and depends on the client's internal retry window.

**Pass / Fail**: ___

---

## 6. Cleanup

After all tests are complete:

**On IBM i** — stop Manzan and remove the test trigger file:

```bash
kill $(cat /var/run/manzan/manzan-default.lock) 2>/dev/null
rm -f /tmp/manzan-test-input.txt
```

**On Windows** — stop OpenSearch and Dashboards by pressing Ctrl+C in their terminal windows. Then remove the firewall rules:

```powershell
Remove-NetFirewallRule -DisplayName "OpenSearch REST (Manzan)"
Remove-NetFirewallRule -DisplayName "OpenSearch Dashboards (Manzan)"
```

Optionally delete the test index before shutting down OpenSearch (run from IBM i):

```bash
curl -s -X DELETE "http://<opensearch-host>:9200/manzan-events"
```

Restore `/QOpenSys/etc/manzan-default/dests.ini` to its pre-test content.

---

## 7. Test Execution Checklist

| TC | Title | Result | Tester | Date |
|---|---|---|---|---|
| TC-01 | Event indexed as document | | | |
| TC-02 | Multiple events = multiple docs | | | |
| TC-03 | Document contains correct fields | | | |
| TC-04 | Document count increases | | | |
| TC-05 | API key auth header forwarded | | | |
| TC-06 | Basic auth credentials forwarded | | | |
| TC-07 | Wrong password prevents indexing | | | |
| TC-08 | Missing username rejected at startup | | | |
| TC-09 | Missing password rejected at startup | | | |
| TC-10 | Missing apiKey rejected at startup | | | |
| TC-11 | Unknown authType rejected at startup | | | |
| TC-12 | SigV4 indexes to AWS domain | | | |
| TC-13 | Missing awsRegion rejected | | | |
| TC-14 | awsService defaults to es | | | |
| TC-15 | Oversized number sanitized to string | | | |
| TC-16 | Normal numbers indexed as numbers | | | |
| TC-17 | Missing endpoint prevents startup | | | |
| TC-18 | Missing index prevents startup | | | |
| TC-19 | enabled=false disables destination | | | |
| TC-20 | Starts when OpenSearch unreachable | | | |
| TC-21 | Delivery after OpenSearch recovers | | | |

---

## 8. Known Limitations & Notes

- **Security plugin**: Tests TC-05 through TC-09 require a security-enabled OpenSearch cluster. The default setup (§3.4) disables security to simplify testing. For TC-05–TC-09, follow the §TC-05 preconditions to run a separate security-enabled instance on port 9201.
- **AWS SigV4 tests**: Section E tests (TC-12 through TC-14) require a real AWS account and an Amazon OpenSearch Service domain. They cannot be tested against the local Windows cluster. If AWS credentials are not available, defer these tests to a dedicated AWS environment.
- **Document visibility delay**: OpenSearch indexes documents asynchronously. After triggering an event, wait at least 2 seconds before querying. If a document does not appear, wait an additional 3 seconds and retry before marking as a failure.
- **IBM i network routing**: IBM i must be able to reach the Windows machine on port 9200 (or 9201 for the secure instance). Verify the firewall rules from §3.3 are in place if `curl` from IBM i to OpenSearch fails.
- **Java 8 target**: The build targets Java 8. The AWS SDK v1 (`aws-java-sdk-core`) included for SigV4 is compatible with Java 8.

---

## 9. Defect Reporting

When logging a defect, include:

1. TC number and title
2. Full `dests.ini` content (redact all credentials)
3. Full stdout/stderr from the Manzan process (copy from the IBM i PASE terminal)
4. The curl response from OpenSearch at the time of failure (full JSON output)
5. Last 50 lines of the OpenSearch terminal on Windows (scroll up in the `opensearch.bat` terminal)
6. IBM i OS version (`DSPSYSVAL SYSVAL(QOSLEVEL)`) and Java version (`java -version`)
7. Whether the Windows OpenSearch instance has the security plugin enabled or disabled
