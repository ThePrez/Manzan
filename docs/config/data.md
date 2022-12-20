`data.ini` is made up of many sections that define data sources. For example, a Manzan data source might be a `file` or `watch`. In the future, there will be many more.

## Schema

Here are the requirements for each section.

* `<id>` is the unique ID that identifies this data source
* `<type>` can be the ID many of the available different data sources
* `<destinations>` are the places where the data should be pushed to, as defined in `dests.ini`
   * Destination IDs can be comma delimited. This will send the same data to multiple destinations.

```ini
[<id>]
# where the data is coming from
type=<type>
# as defined in dests.ini
destinations=<destinations>

# other properties for <id> here..
```

## Available types

Some types have additional properties that they required.

| id    | Description                                 | Required properties                                                                     |
|-------|---------------------------------------------|-----------------------------------------------------------------------------------------|
| `file`  | Triggered when a file changes               | * `file` path of file to watch <br>* `filter` only listen for lines that include this value |
| `watch` | Triggered when the Manzan handler is called | `id` of the watch (session ID)                                                          |

### Example

```ini
[watcher1]
type=watch
id=jesse
destinations=test_out, slackme
enabled=false
​
[logfile1]
type=file
file=test.txt
destinations=email_it, test_out
filter=error
format=$FILE_DATA$
```

## Optional properties

These are optional properties available on all types:

* `format` can be used to define a nicer messages to be sent to the destination
* `enabled` is a boolean (`true` or `false`) so a data source can be defined but disabled
* `filter` is a string that can be used to filter through which events are sent to the destination