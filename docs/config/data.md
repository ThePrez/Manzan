`data.ini` is made up of many sections that define data sources. For example, a Manzan data source might be a `file` or `watch`. In the future, there will be many more.

## Schema

Here are the requirements for each section.

* `<id>` is the unique ID that identifies this data source
* `type` can be the ID many of the available different data sources
* `destinations` are the places where the data should be pushed to, as defined in `dests.ini`
   * Destination IDs can be comma delimited. This will send the same data to multiple destinations.


### Optional properties for all types

These are optional properties available on all types:

* `format` can be used to define a nicer messages to be sent to the destination.\
 For file events we have the following variables available: FILE_DATA, FILE_NAME, FILE_PATH.\
 For watch events we have the following variables available: SESSION_ID, MESSAGE_ID, MESSAGE_TYPE, SEVERITY, JOB, SENDING_USRPRF, SENDING_PROGRAM_NAME, SENDING_MODULE_NAME, SENDING_PROCEDURE_NAME, MESSAGE_TIMESTAMP, MESSAGE.\
 \
  By specifying the variable in your format string surrounded by dollar signs, the variables value will be replaced in your format string. Ex. For a file `a.txt` that received the data `hello world` the format string `Data: $FILE_DATA$, Name: $FILE_NAME$` will evaluate to `Data: hello world, Name: a.txt`. `format` can be provided in both data sources and destinations.
 
* `enabled` is a boolean (`true` or `false`) so a data source can be defined but disabled

```ini
[<id>]
# where the data is coming from
type=<type>
# as defined in dests.ini
destinations=<destinations>

# other properties for <id> here..
```

### Available types

Some types have additional properties that they required.

| id      | Description                                 | Required properties            | Optional properties                                                                                                        |
|---------|---------------------------------------------|--------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `file`  | Triggered when a file changes               | `file` path of file to watch   | `filter` only listen for lines that include this value                                                                |
| `watch` | Triggered when the Manzan handler is called | `id` of the watch (session ID) | `strwch` is part of the `STRWCH` CL command that can be used to describe how to start the watch when Manzan starts up |


### Example

```ini
# This will manage information from the watch session with id "jesse". It is disabled.
[watcher1]
type=watch
id=jesse
destinations=test_out, slackme
enabled=false
​
# This will trigger an event whenever anything is appended to "test.txt"
[logfile1]
type=file
file=test.txt
destinations=email_it, test_out
filter=error
format=$FILE_DATA$

# This will manage information from the watch session with id "jesse".
# The event will be formatted as specified in the format value, and
# the system watch will automatically be started when the Manzan Distributor is run
[watchout]
type=watch
id=jesse
destinations=test_out, slackme
format=$MESSAGE_ID$ (severity $SEVERITY$): $MESSAGE$ 
strwch=WCHMSG((*ALL)) WCHMSGQ((*HSTLOG))
```
