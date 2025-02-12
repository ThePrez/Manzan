`data.ini` is made up of many sections that define data sources. A Manzan data source might be a `file`, `watch`, or `table`. In the future, there will be many more.

## Schema

Here are the requirements for each section.

* `<id>` is the unique ID that identifies this data source
* `type` can be the ID many of the available different data sources
* `destinations` are the places where the data should be pushed to, as defined in `dests.ini`
   * Destination IDs can be comma delimited. This will send the same data to multiple destinations.


### Optional properties for all types

These are optional properties available on all types:

* `format` can be used to define a nicer messages to be sent to the destination. By specifying the variable in your format string surrounded by dollar signs, the variables value will be replaced in your format string. Ex. For a file `a.txt` that received the data `hello world` the format string `Data: $FILE_DATA$, Name: $FILE_NAME$` will evaluate to `Data: hello world, Name: a.txt`. `format` can be provided in both data sources and destinations.
   * **File Events**: `FILE_DATA`, `FILE_NAME`, `FILE_PATH`
   * **Message Queue Watch Event**: `SESSION_ID`, `MESSAGE_ID`, `MESSAGE_TYPE`, `SEVERITY`, `JOB`, `SENDING_USRPRF`, `SENDING_PROGRAM_NAME`, `SENDING_MODULE_NAME`, `SENDING_PROCEDURE_NAME`, `MESSAGE_TIMESTAMP`, `MESSAGE`
   * **Licensed Internal Code (LIC) Log Watch Event**: `SESSION_ID`, `MAJOR_CODE`, `MINOR_CODE`, `LOG_ID`, `LOG_TIMESTAMP`, `TDE_NUM`, `TASK_NAME`, `SERVER_TYPE`, `EXCEPTION_ID`, `JOB`, `THREAD_ID`, `MODULE_OFFSET`, `MODULE_RU_NAME`, `MODULE_NAME`, `MODULE_ENTRY_POINT_NAME`
   * **Product Activity Log Watch Event**: `SESSION_ID`, `SYSTEM_REFERENCE_CODE`, `DEVICE_NAME`, `MODEL`, `SERIAL_NUMBER`, `RESOURCE_NAME`, `LOG_ID`, `PAL_TIMESTAMP`, `REFERENCE_CODE`, `SECONDARY_CODE`, `TABLE_ID`, `SEQUENCE_NUM`
* `interval` can be used to configure how often the distributor checks for events in milliseconds (default `5`)
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
| `file`  | Triggered when a file changes               | `file` path of file to watch   | * `filter` only listen for lines that include this value                                                                |
| `watch` | Triggered when the Manzan handler is called | `id` of the watch (session ID) | * `strwch` is part of the `STRWCH` CL command that can be used to describe how to start the watch when Manzan starts up <br> * `numToProcess` can be used to configure how many messages are queried for by the distributor (default `1000`) <br> * `interval` the interval at which to query for new messages|
| `table` | Triggered when data is inserted into the specified table | `table` name of the table to watch  <br> `schema` the schema in which the table resides | * `numToProcess` can be used to configure how many messages are queried for by the distributor (default `1000`) <br> * `interval` the interval at which to query for new messages|

### Special event types
The table event type is primarily used as a mechanism to transport arbitrary data to a chosen destination. This data can be programmatically inserted into the table, or it can be inserted manually. Note that this data will be deleted from the table after it is processed. In the case that you want to persist the data in the database, consider using a different event type such as `file` or `watch`.

If using the table event type, this needs to be a specially crafted table with a primary key column named `ID`. `ID` must be an integer data type. The data in this table will be removed after it is processed. 

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
# the system watch will automatically be started when the Manzan Distributor is run.
# The distributor will query for 10000 messages every 10 milliseconds.
[watchout]
type=watch
id=jesse
destinations=test_out, slackme
format=$MESSAGE_ID$ (severity $SEVERITY$): $MESSAGE$ 
strwch=WCHMSG((*ALL)) WCHMSGQ((*HSTLOG))
interval=10
numToProcess=10000

# This will write messages to slack and kafka destinations whenever data is inserted into the foo.announcements table
[tab1]
type=table
table=announcements
schema=foo
destinations=slack, kafka
format=$DATA$ 
```
