# Elasticsearch

This example shows how to use `elasticsearch` as a destination for a `watch` data source.

## Configuration

Be sure to have read up on [Manzan configuration](/config/index.md) to understand where these files exist on your system.

### `data.ini`

```ini
[watchout]
type=watch
id=sanjula
destinations=elasticsearch_out
format=$MESSAGE_ID$ (severity $SEVERITY$): $MESSAGE$
strwch=WCHMSG((*ALL)) WCHMSGQ((*HSTLOG))
```

### `dests.ini`

```ini
[elasticsearch_out]
type=elasticsearch

# Set your Elasticsearch endpoint (ie. https://e8a2d5b7c41b49f3b89ab1cba528a79b.us-central1.gcp.cloud.es.io:443)
endpoint=<endpoint>

# Set your Elasticsearch API key (ie. UJANjw2gHYG3K4h1D2l1cHd4HHJ...)
apiKey=<api_key>

# Set your index name
index=manzan
```

## Result

<div style="text-align: center; margin: 20px;">
    <img src="https://github.com/ThePrez/Manzan/blob/main/docs/images/elasticsearch1.png?raw=true" alt="Elasticsearch 1" style="box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); border-radius: 8px; max-width: 100%; display: block; margin-bottom: 20px;">
    <img src="https://github.com/ThePrez/Manzan/blob/main/docs/images/elasticsearch2.png?raw=true" alt="Elasticsearch 2" style="box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); border-radius: 8px; max-width: 100%;">
</div>