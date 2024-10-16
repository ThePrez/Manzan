# ActiveMQ

This example shows how to use `activemq` as a destination for a `file` data source. This data will also be routed to the test_out destination, but we will only show the activemq configuration here.

## Configuration

Be sure to have read up on [Manzan configuration](/config/index.md) to understand where these files exist on your system.

### `data.ini`

```ini
[logfile1]
type=file
file=/Users/zakjonat/Documents/myfile.txt
destinations=test_out, mq
format=$FILE_DATA$
```

### `dests.ini`

```ini
[mq]
type=activemq
destinationType=queue
destinationName=TEST.QUEUE
format=Data received: $FILE_DATA$
componentOptions.brokerURL=tcp://myactivemq:61616
```

## Result

<div style="text-align: center; margin: 20px;">
    <img src="https://github.com/ThePrez/Manzan/blob/main/docs/images/activeMQ.png?raw=true" alt="ActiveMQ dashboard" style="box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); border-radius: 8px; max-width: 100%; display: block; margin-bottom: 20px;">
</div>