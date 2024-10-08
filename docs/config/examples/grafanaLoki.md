# Grafana Loki

This example shows how to use `loki` as a destination for a `watch` data source.

## Configuration

Be sure to have read up on [Manzan configuration](/config/index.md) to understand where these files exist on your system.

### `data.ini`

```ini
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
url=<loki_url>
username=<loki_username>
password=<loki_password>
```

## Result

<div style="text-align: center; margin: 20px;">
    <img src="../../images/grafanaLoki1.png" alt="Grafana Loki 1" style="box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); border-radius: 8px; max-width: 100%; display: block; margin-bottom: 20px;">
    <img src="../../images/grafanaLoki2.png" alt="Grafana Loki 2" style="box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); border-radius: 8px; max-width: 100%;">
</div>