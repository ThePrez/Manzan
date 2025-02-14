# Splunk

This example shows how to use `splunk-hec` (sends data to Splunk using the HTTP Event Collector) as a destination for a `watch` data source.

## Configuration

Be sure to have read up on [Manzan configuration](/config/index.md) to understand where these files exist on your system.

### `data.ini`

```ini
[watcher1]
type=watch
id=sanjula
destinations=splunk
format=$MESSAGE_ID$ (severity $SEVERITY$): $MESSAGE$
strwch=WCHMSG((*ALL)) WCHMSGQ((*HSTLOG))
```

### `dests.ini`

```ini
[splunk]
type=splunk-hec

# Set your Splunk host and port (ie. my_splunk_server:8089)
splunkUrl=<splunk_host>:<splunk_port>

# Set your Splunk token (ie. 37cab231-1531-2552-14...)
token=<splunk_token>

# Set your Splunk index to write to
index=<splunk_index>

# Disable Splunk HEC TLS verification (optional)
skipTlsVerify=true
```

## Result

<div style="text-align: center; margin: 20px;">
    <img src="https://github.com/ThePrez/Manzan/blob/main/docs/images/splunk.png?raw=true" alt="Splunk" style="box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); border-radius: 8px; max-width: 100%; display: block; margin-bottom: 20px;">
</div>