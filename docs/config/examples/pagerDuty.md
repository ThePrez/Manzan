# Splunk

This example shows how to use `pagerduty` as a destination for a `file` data source.

## Configuration

Be sure to have read up on [Manzan configuration](/config/index.md) to understand where these files exist on your system.

### `data.ini`

```ini
[serverLogFile]
type=file
file=/home/GITHUBADM/my-cool-application/server/server.log
destinations=pagerduty
```

### `dests.ini`

```ini
[pagerduty]
type=pagerduty

# Set your PagerDuty Events API v2 integration key as the routing key
routingKey=<routing_key>

# (Optional) Set the component of the source machine that is responsible for the event
component=Jetty Web Server

# (Optional) Set the logical grouping of components of a service
group=My Cool Application

# (Optional) Set the class/type of the event
class=Server Error
```

## Result

<div style="text-align: center; margin: 20px;">
    <img src="https://github.com/ThePrez/Manzan/blob/main/docs/images/pagerDuty1.png?raw=true" alt="PagerDuty 1" style="box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); border-radius: 8px; max-width: 100%; display: block; margin-bottom: 20px;">
    <img src="https://github.com/ThePrez/Manzan/blob/main/docs/images/pagerDuty2.png?raw=true" alt="PagerDuty 2" style="box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); border-radius: 8px; max-width: 100%;">
</div>