# Mezmo

This example shows how to use `mezmo` as a destination for a `watch` data source.

## Configuration

Be sure to have read up on [Manzan configuration](/config/index.md) to understand where these files exist on your system.

### `data.ini`

```ini
[watchout]
type=watch
id=sanjula
destinations=mezmo_out
format=$MESSAGE_ID$ (severity $SEVERITY$): $MESSAGE$
strwch=WCHMSG((*ALL)) WCHMSGQ((*HSTLOG))
```

### `dests.ini`

```ini
[mezmo_out]
type=mezmo

# Set your Mezmo ingestion key as the API key (ie. 5d65017h732a52283...)
apiKey=<api_key>

# (Optional) Set the tag used to dynamically group hosts
tags=IBM i

# (Optional) Set the name of the application that generates the log line
app=Jetty Web Server
```

## Result

<div style="text-align: center; margin: 20px;">
    <img src="https://github.com/ThePrez/Manzan/blob/main/docs/images/mezmo1.png?raw=true" alt="Mezmo 1" style="box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); border-radius: 8px; max-width: 100%; display: block; margin-bottom: 20px;">
    <img src="https://github.com/ThePrez/Manzan/blob/main/docs/images/mezmo2.png?raw=true" alt="Mezmo 2" style="box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); border-radius: 8px; max-width: 100%;">
</div>