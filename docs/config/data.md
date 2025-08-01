`data.ini` is made up of many sections that define data sources. A Manzan data source might be a `file`, `watch`, or `table`. In the future, there will be many more.

## Schema

Here are the requirements for each section.

* `<id>` is the unique ID that identifies this data source
* `type` can be the ID many of the available different data sources
* `destinations` are the places where the data should be pushed to, as defined in `dests.ini`
   * Destination IDs can be comma delimited. This will send the same data to multiple destinations.


### Optional properties for all types

These are optional properties available on all types:

* `format` Please see the dedication section on [Data Formatting](./config/format.md)
* `injections.*` is a property that can be specified to inject key value pairs into the retrieved data. For example specifying `injections.HOSTNAME=abcd@myibmserver.com` would make it so `HOSTNAME=abcd@myibmserver.com` is always part of the retrieved data for the specified datasource. This property can then be used as a format option (but you don't have to) by specifying `$HOSTNAME$`. You can specify as many injections as you want, on separate lines.
* `interval` can be used to configure how often the distributor checks for events in milliseconds (default `1000`)
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

Some types have additional properties that they require.

| id      | Description                                 | Required properties            | Optional properties                                                                                                        |
|---------|---------------------------------------------|--------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `file`  | Triggered when a file changes               | `file` path of file to watch   | * `filter` only listen for lines that include this value                                                                |
| `watch` | Triggered when the Manzan handler is called | `id` of the watch (session ID) <br> `strwch` is part of the `STRWCH` CL command that can be used to describe how to start the watch when Manzan starts up | * `numToProcess` can be used to configure how many messages are queried for by the distributor (default `1000`) <br> * `interval` the interval in ms at which to query for new messages|
| `table` | Triggered when data is inserted into the specified table | `table` name of the table to watch  <br> `schema` the schema in which the table resides | * `numToProcess` can be used to configure how many messages are queried for by the distributor (default `1000`) <br> * `interval` the interval in ms at which to query for new messages|
| `audit` | Triggered when the specified `auditType` catches an event. | `auditType` options are `AUTHORITY_FAILURE`, `AUTHORITY_CHANGES`, `COMMAND_STRING`, `CREATE_OBJECT`, `USER_PROFILE_CHANGES`, `DELETE_OPERATION`, `ENVIRONMENT_VARIABLE`, `GENERIC_RECORD`, `JOB_CHANGE`, `OBJECT_MANAGEMENT_CHANGE`, `OWNERSHIP_CHANGE`, `PASSWORD`, `SERVICE_TOOLS_ACTION`, `ACTION_TO_SYSTEM_VALUE`, `READ_OF_OBJECT`, `CHANGE_TO_OBJECT`, `NETWORK_PASSWORD_ERROR`, `SYSTEMS_MANAGEMENT_CHANGE`, `SOCKETS_CONNECTIONS`, `PRIMARY_GROUP_CHANGE_FOR_RESTORED_OBJECT`, `OWNERSHIP_CHANGE_FOR_RESTORED_OBJECT`, `AUTHORITY_CHANGE_FOR_RESTORED_OBJECT`, `PTF_OBJECT_CHANGE`, `PROFILE_SWAP`, `PRIMARY_GROUP_CHANGE`, `PTF_OPERATIONS`, `PROGRAM_ADOPT`, `OBJECT_RESTORE`, `ATTRIBUTE_CHANGE`, `DB2_MIRROR_REPLICATION_STATE`, `DB2_MIRROR_PRODUCT_SERVICES`, `DB2_MIRROR_REPLICATION_SERVICES`, `DB2_MIRROR_COMMUNICATION_SERVICES`, `DB2_MIRROR_SETUP_TOOLS`, `LINK_UNLINK_SEARCH_DIRECTORY`, `INTRUSION_MONITOR`, `SERVICE_TOOLS_USER_ID_AND_ATTRIBUTE_CHANGES`, `ROW_AND_COLUMN_ACCESS_CONTROL`, `ATTRIBUTE_CHANGES`, `ADOPTED_AUTHORITY`, `AUDITING_CHANGE`  | * `fallbackStartTime` is the number of hours prior to the current date that we will query for audit messages, if no audit messages have been queried before <br> * `numToProcess` can be used to configure how many messages are queried for by the distributor (default `1000`) <br> * `interval` the interval in ms at which to query for new messages|
| `sql`  | Executes arbitrary sql at a predefined interval    | `query` to be executed  | * `interval`  the interval in ms at which to run the sql statement |
| `cmd`  | Executes an arbitrary command at a predefined interval  | `cmd` command to be executed  | * `args` The arguments to be passed to this command. The full command to be executed will be `<cmd> <args>`. Note. Chaining commands together will not work. In the case of needing to execute multiple commands, try putting them in a script and then executing the script. <br> * `interval`  the interval in ms at which to run the command. |
| `http` | Fetches data from an http endpoint at the specified interval | `url` to make an http request from including any path parameters | * `interval` the interval at which to query for new messages <br> * `filter` only listen for responses that include this value <br> * \<header\> any header key value pair to be used for this http request|

### Special event types
The `table` event type is primarily used as a mechanism to transport arbitrary data to a chosen destination. This data can be programmatically inserted into the table, or it can be inserted manually. Note that this data will be deleted from the table after it is processed. In the case that you want to persist the data in the database, consider using a different event type such as `file` or `watch`.

If using the table event type, this needs to be a specially crafted table with a primary key column named `ID`. `ID` must be an integer data type. The data in this table will be removed after it is processed. 

The `audit` event type is used to monitor events from the various IBM i [Audit journal entry services](https://www.ibm.com/docs/en/i/7.4.0?topic=services-audit-journal-entry). Each audit type corresponds to one of the IBM i audit tables.

Note: You will need to enable audit journaling on your system before you can watch for audit journal events
through Manzan. Please check out the following [documentation](https://www.ibm.com/docs/en/i/7.4.0?topic=journal-setting-up-security-auditing) for instructions on how to start the audit journal on IBM i.

The `sql` event type is not watching a data source like the other event types. Instead, it is executing an sql statement provided by the user at the predefined interval (default is every second).

### Example

```ini
# This will watch events from the QSYSOPR message queue. It is disabled.
[watcher1]
type=watch
id=QSYSOPR
destinations=test_out, slackme
strwch=WCHMSG((*ALL)) WCHMSGQ((QSYS/QSYSOPR))
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

# This will write messages to slack whenever there is a login failure on the system
[audit1]
type=audit
destinations=slackme
auditType=PASSWORD
fallbackStartTime=1
format=Violation type: $VIOLATION_TYPE_DETAIL$, username: $AUDIT_USER_NAME$ hostname: $HOSTNAME$ timezone:$TIMEZONE$
interval=5000
injections.HOSTNAME="myhost@abc.com"
injections.TIMEZONE="EST"

# Get the newest data from the sample.department table every 5 minutes
[sql1]
type=sql
query=SELECT * FROM sample.department WHERE time > CURRENT_TIMESTAMP - 5 MINUTES
destinations=googpubsub, stdout
format=department: $DEPTNAME$
interval=300000

# Display the job log of JOB(047284/QTMHHTTP/ADMIN) every minute
[cmd1]
type=cmd
destinations=stdout
cmd=system 
args="DSPJOBLOG JOB(047284/QTMHHTTP/ADMIN)"
format=cmd $CMD$ args $ARGS$ exitval $EXITVALUE$ stderr $STDERR$ stdout $STDOUT$
interval=60000

# Fetch data from https://fakeusergenerator.com every 5 minutes, with the specified 
# authorization header
[http1]
type=http
destinations=test_out
format=Result: $results.name.first$ $results.name.last$ $json:results.location$
url=https://fakeusergenerator.com
interval=300000
authorization=bearer xxxxxx
```
