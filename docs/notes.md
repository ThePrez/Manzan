# watch listens to

* job lob
* message queue
* pal entries 
* vlog entries (lic logs)

# local setup

1. `cd camel`
2. `mvn compile` - builds Manzan, woop
3. `mvn exec:java`
  * creates initial ini files

* `app.ini` general manzan config. library install directory, system connection information
* `data.ini` contains all input data being driven to camel (data queue, file, table, whatever)
* `dests.ini` used to describe where input gets wrtten to

### `app.ini`

```ini
[install]
library=jesseg

[remote]
system=oss73dev
user=linux
password=linux1
```

### `data.ini`

```ini
[input_file]

# where the data is coming from (it's a camel route)
type=file

# camel route attributes
file=test.txt

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

## Running on mac

```
$ mvn exec:java
[INFO] Scanning for projects...
[INFO] 
[INFO] ---------------------< com.github.theprez:manzan >----------------------
[INFO] Building Manzan Camel Component 1.0
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- exec-maven-plugin:1.6.0:java (default-cli) @ manzan ---
Apache Camel version 3.14.0
[ez.manzan.ManzanMainApp.main()] AbstractCamelContext           INFO  Routes startup (total:2 started:2)
[ez.manzan.ManzanMainApp.main()] AbstractCamelContext           INFO      Started test_out (direct://test_out)
[ez.manzan.ManzanMainApp.main()] AbstractCamelContext           INFO      Started file:///Users/barry/Repos/Manzan/camel/test.txt (timer://foo)
[ez.manzan.ManzanMainApp.main()] AbstractCamelContext           INFO  Apache Camel 3.14.0 (camel-1) started in 371ms (build:46ms init:185ms start:140ms)
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

```
$ pwd
/Users/barry/Repos/Manzan/camel
$ echo "yoyo" >> test.txt 
$ echo "yoyo error" >> test.txt 
$ echo "sick bruh error" >> test.txt 
```

# IBM i setup

Ensure is deployed on IBM i

```
-bash-5.1$ pwd
/Manzan
```

1. `gmake install` - installs into `MANZAN`
    * change library with `BUILDLIB=MANZAN2 gmake install`
    * config gets created in `/QOpenSys/etc/manzan/`
2. run with `/opt/manzan/bin/manzan`
3. example exists for watch