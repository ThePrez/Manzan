# File

This example shows how to use `file` as a data source.

## Configuration

Be sure to have read up on [Manzan configuration](/config/index.md) to understand where these files exist on your system.

### `app.ini`

```ini
[install]
library=jesseg

# remote is not required if running on IBM i
[remote]
system=xxx
user=xxx
password=xxx
```

### `data.ini`

```ini
[input_file]

type=file
file=/path/to/file/test.txt
# as defined in dests.ini
destinations=test_out

# filter is optional. only logs messages that contain this value
filter=error

# format is also optional. logs FILE_DATA as-is if not provided
format=YOYO $FILE_DATA$
```

### `dests.ini`

```ini
[test_out]
# the camel out. stdout is a manzan special baby
type=stdout
```

## Starting Manzan

* If on IBM i, use `/opt/manzan/bin/manzan`
* If on a non-IBM i system, use `mvn exec:java` from the `Manzan/camel` directory

```
...
Apache Camel version 3.14.0
[ez.manzan.ManzanMainApp.main()] AbstractCamelContext           INFO  Routes startup (total:2 started:2)
[ez.manzan.ManzanMainApp.main()] AbstractCamelContext           INFO      Started test_out (direct://test_out)
[ez.manzan.ManzanMainApp.main()] AbstractCamelContext           INFO      Started file:///Users/barry/Repos/Manzan/camel/test.txt (timer://foo)
[ez.manzan.ManzanMainApp.main()] AbstractCamelContext           INFO  Apache Camel 3.14.0 (camel-1) started in 371ms (build:46ms init:185ms start:140ms)
```

Use another shell to write to the file we defined to test our destination.

```sh
$ echo "yoyo" >> test.txt 
$ echo "yoyo error" >> test.txt 
$ echo "sick bruh error" >> test.txt 
```

And in the shell running Manzan, you should see the file data written.

```json
{
  "FILE_NAME" : "test.txt",
  "FILE_PATH" : "/Users/barry/Repos/Manzan/camel/test.txt",
  "FILE_DATA" : "yoyo error"
}
{
  "FILE_NAME" : "test.txt",
  "FILE_PATH" : "/Users/barry/Repos/Manzan/camel/test.txt",
  "FILE_DATA" : "sick bruh error"
}
```