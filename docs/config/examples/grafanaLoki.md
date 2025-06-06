# Grafana Loki

This example shows how to use `loki` as a destination for a `watch` data source.

## Configuration

Be sure to have read up on [Manzan configuration](/config/index.md) to understand where these files exist on your system.

### `data.ini`

```ini
[watchout]
type=watch
id=sanjula
destinations=loki_out
format=$MESSAGE_ID$ (severity $SEVERITY$): $MESSAGE$
strwch=WCHMSG((*ALL)) WCHMSGQ((*HSTLOG))
```

### `dests.ini`

```ini
[loki_out]
type=loki

# Set your Grafana Loki url (ie. https://logs-prod-002.grafana.net)
url=<loki_url>

# Set your Grafana Loki username (ie. 994212)
username=<loki_username>

# Set your Grafana Loki password (ie. glc_ycBaPyajPJUdKLK2UIO...)
password=<loki_password>
```

## Result

<div style="text-align: center; margin: 20px;">
    <img src="https://github.com/ThePrez/Manzan/blob/main/docs/images/grafanaLoki1.png?raw=true" alt="Grafana Loki 1" style="box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); border-radius: 8px; max-width: 100%; display: block; margin-bottom: 20px;">
    <img src="https://github.com/ThePrez/Manzan/blob/main/docs/images/grafanaLoki2.png?raw=true" alt="Grafana Loki 2" style="box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); border-radius: 8px; max-width: 100%;">
</div>