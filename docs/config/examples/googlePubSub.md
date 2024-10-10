# Grafana Loki

This example shows how to use `google-pubsub` as a destination for a `watch` data source.

## Configuration

Be sure to have read up on [Manzan configuration](/config/index.md) to understand where these files exist on your system.

### `data.ini`

```ini
[watchout]
type=watch
id=sanjula
destinations=pubsub_out
format=$MESSAGE_ID$ (severity $SEVERITY$): $MESSAGE$
strwch=WCHMSG((*ALL)) WCHMSGQ((*HSTLOG))
```

### `dests.ini`

```ini
[pubsub_out]
type=google-pubsub

# Set your project ID (ie. my-project-438217)
projectId=<pubsub_project_id>

# Set your topic name (ie. my-topic)
topicName=<pubsub_topic_name>

# Set your service account name (ie. file:/QOpenSys/etc/manzan/my-project-438217-b7392819a7hf.json)
serviceAccountKey=<pubsub_service_account_key>
```

## Result

<div style="text-align: center; margin: 20px;">
    <img src="https://github.com/ThePrez/Manzan/blob/main/docs/images/googlePubSub1.png?raw=true" alt="Google Pub/Sub 1" style="box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); border-radius: 8px; max-width: 100%; display: block; margin-bottom: 20px;">
</div>