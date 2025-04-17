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
   * **Audit COMMON (Every audit event has these columns)**: `ENTRY_TIMESTAMP`, `SEQUENCE_NUMBER`, `USER_NAME`, `QUALIFIED_JOB_NAME`, `JOB_NAME`, `JOB_USER`, `JOB_NUMBER`, `THREAD`, `PROGRAM_LIBRARY`, `PROGRAM_NAME`, `PROGRAM_LIBRARY_ASP_DEVICE`, `PROGRAM_LIBRARY_ASP_NUMBER`, `REMOTE_PORT`, `REMOTE_ADDRESS`, `SYSTEM_NAME`, `SYSTEM_SEQUENCE_NUMBER`, `RECEIVER_LIBRARY`, `RECEIVER_NAME`, `RECEIVER_ASP_DEVICE`, `RECEIVER_ASP_NUMBER`, `ARM_NUMBER`,
   * **Audit PASSWORD Events**: `VIOLATION_TYPE`, `VIOLATION_TYPE_DETAIL`, `AUDIT_USER_NAME`, `DEVICE_NAME`, `INTERFACE_NAME`, `REMOTE_LOCATION`, `LOCAL_LOCATION`, `NETWORK_ID`, `DECRYPT_HOST_VARIABLE`, `DECRYPT_OBJECT_LIBRARY`, `DECRYPT_OBJECT_NAME`, `DECRYPT_OBJECT_TYPE`, `DECRYPT_OBJECT_ASP_NAME`, `DECRYPT_OBJECT_ASP_NUMBER`
   * **Audit AUTHORITY_FAILURE Events**: `VIOLATION_TYPE`, `VIOLATION_TYPE_DETAIL`, `VALIDATION_ERROR_ACTION`, `VALIDATION_ERROR_ACTION_DETAIL`, `OPERATION_VIOLATION_CODE`, `OBJECT_LIBRARY`, `OBJECT_NAME`, `OBJECT_TYPE`, `OBJECT_ASP_NAME`, `OBJECT_ASP_NUMBER`, `FIELD_NAME`, `TCPIP_PORT`, `API_NAME`, `PTF_NUMBER`, `AAC_NAME`, `USER_PROFILE_NAME`, `WORKSTATION_NAME`, `PROGRAM_INSTRUCTION`, `PATH_NAME`, `PATH_NAME_INDICATOR`, `RELATIVE_DIRECTORY_FILE_ID`, `IFS_OBJECT_NAME`, `OBJECT_FILE_ID`, `PARENT_FILE_ID`, `OFFICE_USER`, `OFFICE_ON_BEHALF_OF_USER`, `DLO_NAME`, `FOLDER_PATH`
   * **Audit AUTHORITY_CHANGES Events**: `OBJECT_LIBRARY`, `OBJECT_NAME`, `OBJECT_TYPE`, `OBJECT_ATTRIBUTE`, `OBJECT_ASP_NAME`, `OBJECT_ASP_NUMBER`, `FIELD_NAME`, `COMMAND_TYPE`, `USER_PROFILE_NAME`, `AUTHORIZATION_LIST_MANAGEMENT`, `OBJECT_EXCLUDE`, `OBJECT_OPERATIONAL`, `OBJECT_MANAGEMENT`, `OBJECT_EXISTENCE`, `OBJECT_ALTER`, `OBJECT_REFERENCE`, `DATA_READ`, `DATA_ADD`, `DATA_UPDATE`, `DATA_DELETE`, `DATA_EXECUTE`, `PREV_AUTHORIZATION_LIST_MANAGEMENT`, `PREV_OBJECT_EXCLUDE`, `PREV_OBJECT_OPERATIONAL`, `PREV_OBJECT_MANAGEMENT`, `PREV_OBJECT_EXISTENCE`, `PREV_OBJECT_ALTER`, `PREV_OBJECT_REFERENCE`, `PREV_DATA_READ`, `PREV_DATA_ADD`, `PREV_DATA_UPDATE`, `PREV_DATA_DELETE`, `PREV_DATA_EXECUTE`, `AUTHORIZATION_LIST`, `AUTHORIZATION_LIST_PUBLIC`, `PREV_AUTHORIZATION_LIST`, `PREV_AUTHORIZATION_LIST_PUBLIC`, `PERSONAL_STATUS_CHANGED`, `ACCESS_CODE_CHANGED`, `ACCESS_CODE`, `PATH_NAME`, `PATH_NAME_INDICATOR`, `RELATIVE_DIRECTORY_FILE_ID`, `IFS_OBJECT_NAME`, `OBJECT_FILE_ID`, `PARENT_FILE_ID`, `OFFICE_USER`, `OFFICE_ON_BEHALF_OF_USER`, `DLO_NAME`, `FOLDER_PATH`
   * **Audit COMMAND_STRING Events**: `ENTRY_TYPE`, `ENTRY_TYPE_DETAIL`, `OBJECT_LIBRARY`, `OBJECT_NAME`, `OBJECT_TYPE`, `OBJECT_ASP_NAME`, `OBJECT_ASP_NUMBER`, `WHERE_RUN`, `WHERE_RUN_DETAIL`, `COMMAND_STRING`
   * **Audit CREATE_OBJECT Events**: `ENTRY_TYPE `, `ENTRY_TYPE_DETAIL `, `OBJECT_LIBRARY `, `OBJECT_NAME `, `OBJECT_TYPE `, `OBJECT_ATTRIBUTE `, `OBJECT_ASP_NAME `, `OBJECT_ASP_NUMBER `, `PATH_NAME `, `PATH_NAME_INDICATOR `, `RELATIVE_DIRECTORY_FILE_ID `, `IFS_OBJECT_NAME `, `OBJECT_FILE_ID `, `PARENT_FILE_ID `, `OFFICE_USER `, `OFFICE_ON_BEHALF_OF_USER `, `DLO_NAME `, `FOLDER_PATH `
   * **Audit USER_PROFILE_CHANGES Events**: `ENTRY_TYPE`, `ENTRY_TYPE_DETAIL`, `USER_PROFILE`, `COMMAND_TYPE`, `STATUS`, `PASSWORD_CHANGED`, `NO_PASSWORD_INDICATOR`, `PASSWORD_EXPIRED`, `LOCAL_PASSWORD_MANAGEMENT`, `PASSWORD_CONFORMANCE`, `BLOCK_PASSWORD_CHANGE`, `PASSWORD_EXPIRATION_INTERVAL`, `USER_EXPIRATION_DATE`, `USER_EXPIRATION_ACTION`, `OWNED_OBJECT_OPTION`, `OWNED_OBJECT_OPTION_NEW_OWNER`, `PRIMARY_GROUP_OPTION`, `NEW_PRIMARY_GROUP`, `NEW_PRIMARY_GROUP_AUTHORITY`, `USER_CLASS_NAME`, `SPECIAL_AUTHORITIES`, `ALLOBJ`, `JOBCTL`, `SAVSYS`, `SECADM`, `SPLCTL`, `SERVICE`, `AUDIT`, `IOSYSCFG`, `PREVIOUS_SPECIAL_AUTHORITIES`, `PREVIOUS_ALLOBJ`, `PREVIOUS_JOBCTL`, `PREVIOUS_SAVSYS`, `PREVIOUS_SECADM`, `PREVIOUS_SPLCTL`, `PREVIOUS_SERVICE`, `PREVIOUS_AUDIT`, `PREVIOUS_IOSYSCFG`, `GROUP_PROFILE_NAME`, `GROUP_OWNER`, `GROUP_AUTHORITY`, `GROUP_AUTHORITY_TYPE`, `SUPPLEMENTAL_GROUP_LIST`, `INITIAL_PROGRAM_LIBRARY`, `INITIAL_PROGRAM`, `INITIAL_MENU_LIBRARY`, `INITIAL_MENU`, `CURRENT_LIBRARY_NAME`, `HOME_DIRECTORY`, `LOCALE_PATH_NAME`, `LIMIT_CAPABILITIES`, `ASSISTANCE_LEVEL`, `USER_OPTIONS`, `SPECIAL_ENVIRONMENT`, `DISPLAY_SIGNON_INFORMATION`, `LIMIT_DEVICE_SESSIONS`, `KEYBOARD_BUFFERING`, `MAXIMUM_ALLOWED_STORAGE`, `PRIORITY_LIMIT`, `JOB_DESCRIPTION_LIBRARY`, `JOB_DESCRIPTION`, `ALTERNATE_SUBSYSTEM_NAME`, `ALTERNATE_SERVER_JOB_NAME`, `ACCOUNTING_CODE`, `MESSAGE_QUEUE_LIBRARY`, `MESSAGE_QUEUE`, `MESSAGE_QUEUE_DELIVERY_METHOD`, `MESSAGE_QUEUE_SEVERITY`, `PRINT_DEVICE`, `OUTPUT_QUEUE_LIBRARY`, `OUTPUT_QUEUE`, `ATTENTION_KEY_HANDLING_ PROGRAM_LIBRARY`, `ATTENTION_KEY_HANDLING_PROGRAM`, `SORT_SEQUENCE_TABLE_LIBRARY`, `SORT_SEQUENCE_TABLE`, `LANGUAGE_ID`, `COUNTRY_OR_REGION_ID`, `CCSID`, `CHARACTER_IDENTIFIER_CONTROL`, `LOCALE_JOB_ATTRIBUTES`, `DOCUMENT_PASSWORD_CHANGED`, `DOCUMENT_PASSWORD_NONE`, `EIM_ID`, `EIM_ASSOCIATION_TYPE`, `EIM_ASSOCIATION_ACTION`, `CREATE_EIM_ID`, `USER_ID_NUMBER`, `GROUP_ID_NUMBER`
   * **Audit DELETE_OPERATION Events**: `ENTRY_TYPE `, `ENTRY_TYPE_DETAIL`, `OBJECT_LIBRARY `, `OBJECT_NAME `, `OBJECT_TYPE `, `OBJECT_ATTRIBUTE `, `OBJECT_ASP_NAME `, `OBJECT_ASP_NUMBER `, `PATH_NAME `, `PATH_NAME_INDICATOR `, `RELATIVE_DIRECTORY_FILE_ID `, `IFS_OBJECT_NAME `, `OBJECT_FILE_ID `, `PARENT_FILE_ID `, `OFFICE_USER `, `OFFICE_ON_BEHALF_OF_USER `, `DLO_NAME `, `FOLDER_PATH `
   * **Audit ENVIRONMENT_VARIABLE Events**: `ENTRY_TYPE `, `ENTRY_TYPE_DETAIL`, `ENVIRONMENT_VARIABLE_NAME`, `NAME_TRUNCATED`, `ENVIRONMENT_VARIABLE_VALUE`, `VALUE_TRUNCATED`
   * **Audit GENERIC_RECORD Events**: `ENTRY_TYPE`, `ENTRY_TYPE_DETAIL`, `ACTION`, `ACTION_DETAIL`, `EXIT_POINT_NAME`, `EXIT_POINT_FORMAT`, `EXIT_PROGRAM_NUMBER`, `EXIT_PROGRAM_LIBRARY`, `EXIT_PROGRAM`, `USER_PROFILE_NAME`, `FUNCTION _REGISTRATION_OPERATION`, `FUNCTION_NAME`, `USAGE_SETTING`, `PREVIOUS_USAGE`, `FUNCTION_ALLOBJ`, `PREVIOUS_ALLOBJ`, `OBJECTCONNECT_COMMAND`, `SAVE_SYSTEM`, `SAVE_ASP`, `SAVE_LIBRARY`, `RESTORE_ASP_DEVICE`, `RESTORE_ASP_NUMBER`, `RESTORE_LIBRARY`, `OBJECTCONNECT_UUID`, `RESTORE_USER`
   * **Audit JOB_CHANGE Events**: `ENTRY_TYPE`, `ENTRY_TYPE_DETAIL`, `JOB_TYPE`, `JOB_TYPE_BASIC`, `JOB_SUBTYPE`, `TARGET_QUALIFIED_JOB_NAME`, `TARGET_JOB_NAME`, `TARGET_JOB_USER`, `TARGET_JOB_NUMBER`, `DEVICE_NAME`, `EFFECTIVE_USER_PROFILE`, `EFFECTIVE_GROUP_PROFILE`, `SUPPLEMENTAL_GROUP_PROFILES`, `REAL_USER_PROFILE`, `SAVED_USER_PROFILE`, `REAL_GROUP_PROFILE`, `SAVED_GROUP_PROFILE`, `REAL_USER_CHANGED`, `EFFECTIVE_USER_CHANGED`, `SAVED_USER_CHANGED`, `REAL_GROUP_CHANGED`, `EFFECTIVE_GROUP_CHANGED`, `SAVED_GROUP_CHANGED`, `SUPPLEMENTAL_GROUPS_CHANGED`, `JOB_DESCRIPTION_LIBRARY`, `JOB_DESCRIPTION`, `JOB_DESCRIPTION_ASP_NAME`, `JOB_DESCRIPTION_ASP_NUMBER`, `JOB_QUEUE_LIBRARY`, `JOB_QUEUE_NAME`, `JOB_QUEUE_ASP_NAME`, `JOB_QUEUE_ASP_NUMBER`, `OUTPUT_QUEUE_LIBRARY`, `OUTPUT_QUEUE_NAME`, `PRINTER_DEVICE`, `TIME_ZONE_DESCRIPTION_NAME`, `THREAD_ASP_NAME`, `LIBRARY_LIST_COUNT`, `LIBRARY_LIST`, `JOB_USER_IDENTITY_DESCRIPTION`, `JOB_USER_IDENTITY`, `WORKLOAD_GROUP`, `EXIT_QUALIFIED_JOB_NAME`, `EXIT_JOB_NAME`, `EXIT_JOB_USER`, `EXIT_JOB_NUMBER`, `EXIT_PROGRAM_LIBRARY`, `EXIT_PROGRAM`
   * **Audit OBJECT_MANAGEMENT_CHANGE Events**: `ENTRY_TYPE`, `ENTRY_TYPE_DETAIL`, `LIBRARY_NAME`, `OBJECT_NAME`, `OBJECT_TYPE`, `OBJECT_ATTRIBUTE`, `PREV_LIBRARY_NAME`, `PREV_OBJECT_NAME`, `OBJECT_ASP_NAME`, `OBJECT_ASP_NUMBER`, `PREV_OBJECT_ASP_NAME`, `PREV_OBJECT_ASP_NUMBER`, `PATH_NAME`, `PATH_NAME_INDICATOR`, `RELATIVE_DIRECTORY_FILE_ID`, `IFS_OBJECT_NAME`, `OBJECT_FILE_ID`, `PARENT_FILE_ID`, `PREV_PATH_NAME`, `PREV_PATH_NAME_INDICATOR`, `PREV_RELATIVE_DIRECTORY_FILE_ID`, `PREV_IFS_OBJECT_NAME`, `PREV_OBJECT_FILE_ID`, `PREV_PARENT_FILE_ID`, `OFFICE_USER`, `OFFICE_ON_BEHALF_OF_USER`, `DLO_NAME`, `FOLDER_PATH`, `PREV_DLO_NAME`, `PREV_FOLDER_PATH`
   * **Audit OWNERSHIP_CHANGE Events**: `OBJECT_LIBRARY`, `OBJECT_NAME`, `OBJECT_TYPE`, `OBJECT_ASP_NAME`, `OBJECT_ASP_NUMBER`, `PREVIOUS_OWNER`, `NEW_OWNER`, `PATH_NAME`, `PATH_NAME_INDICATOR`, `RELATIVE_DIRECTORY_FILE_ID`, `IFS_OBJECT_NAME`, `OBJECT_FILE_ID`, `PARENT_FILE_ID`, `OFFICE_USER`, `OFFICE_ON_BEHALF_OF_USER`, `DLO_NAME`, `FOLDER_PATH`
   * **Audit SERVICE_TOOLS_ACTION Events**: `ENTRY_TYPE`, `ENTRY_TYPE_DETAIL`, `SERVICE_TOOL`, `SERVICE_TOOL (continued)`, `SERVICE_TOOL_DETAIL`, `OBJECT_LIBRARY`, `OBJECT_NAME`, `OBJECT_TYPE`, `OBJECT_ASP_NAME`, `OBJECT_ASP_NUMBER`, `DLO_NAME`, `FOLDER_PATH`, `SOURCE_NODE_ID`, `SOURCE_USER`, `TARGET_QUALIFIED_JOB_NAME`, `TARGET_JOB_NAME`, `TARGET_JOB_USER`, `TARGET_JOB_NUMBER`, `TARGET_JOB_USER_IDENTITY`, `DMPSYSOBJ_LIBRARY`, `DMPSYSOBJ_OBJECT`, `DMPSYSOBJ_TYPE`, `DMPSYSOBJ_ASP_NAME`, `DMPSYSOBJ_ASP_NUMBER`, `AA_COMMAND_NAME`, `EARLY_TRACE_ACTION`, `ARM_TRACE`, `TRCTCPAPP_OPTION`, `APPLICATION_TRACED`, `SERVICE_TOOLS_PROFILE`, `CONSOLE_TYPE`, `CONSOLE_ACTION`, `ADDRESS_FAMILY`, `CURRENT_IP_ADDRESS`, `CURRENT_DEVICE_ID`, `PREVIOUS_IP_ADDRESS`, `PREVIOUS_DEVICE_ID`, `WATCH_SESSION`, `SERVICE_TOOL_USERID`, `USER_PROFILE`, `LIC_RU_NAME`, `ADDRESS_OF_ALTERED_STORAGE`, `SEGMENT_TYPE`, `NUMBER_OF_ALTERED_BYTES`, `ALTERED_STORAGE_VALUE`, `PREVIOUS_STORAGE_VALUE`
   * **Audit ACTION_TO_SYSTEM_VALUE Events**: `ENTRY_TYPE`, `ENTRY_TYPE_DETAIL`, `SYSTEM_VALUE`, `OLD_VALUE`, `NEW_VALUE`

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

Some types have additional properties that they require.

| id      | Description                                 | Required properties            | Optional properties                                                                                                        |
|---------|---------------------------------------------|--------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `file`  | Triggered when a file changes               | `file` path of file to watch   | * `filter` only listen for lines that include this value                                                                |
| `watch` | Triggered when the Manzan handler is called | `id` of the watch (session ID) | * `strwch` is part of the `STRWCH` CL command that can be used to describe how to start the watch when Manzan starts up <br> * `numToProcess` can be used to configure how many messages are queried for by the distributor (default `1000`) <br> * `interval` the interval at which to query for new messages|
| `table` | Triggered when data is inserted into the specified table | `table` name of the table to watch  <br> `schema` the schema in which the table resides | * `numToProcess` can be used to configure how many messages are queried for by the distributor (default `1000`) <br> * `interval` the interval at which to query for new messages|
| `audit` | Triggered when the specified `auditType` catches an event. | `auditType` Options are `AUTHORITY_FAILURE`, `AUTHORITY_CHANGES`, `COMMAND_STRING`, `CREATE_OBJECT`, `USER_PROFILE_CHANGES`, `DELETE_OPERATION`, `ENVIRONMENT_VARIABLE`, `GENERIC_RECORD`, `JOB_CHANGE`, `OBJECT_MANAGEMENT_CHANGE`, `OWNERSHIP_CHANGE`, `PASSWORD`, `SERVICE_TOOLS_ACTION`, `ACTION_TO_SYSTEM_VALUE`  | * `fallbackStartTime` is the number of hours prior to the current date that we will query for audit messages, if no audit messages have been queried before <br> * `numToProcess` can be used to configure how many messages are queried for by the distributor (default `1000`) <br> * `interval` the interval at which to query for new messages|

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

# This will write messages to slack whenever there is a login failure on the system
[audit1]
type=audit
destinations=slackme
auditType=PASSWORD
fallbackStartTime=1
format=Violation type: $VIOLATION_TYPE_DETAIL$, username: $AUDIT_USER_NAME$
interval=5000
```
