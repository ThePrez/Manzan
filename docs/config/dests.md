`dests.ini` is made up of many sections that define data destinations. A destination is really a place (like a service) where the data can be sent to.

## Schema

Here are the requirements for each section.

* `<id>` is the unique ID that identifies this destination
* `type` can be the ID many of the available different destnations

```ini
[<id>]
# the type of destination the data is going to
type=<type>

# other properties for <id> here..
```
As well, each section can provide `format` as an optional type.

## Available types

Some types have additional properties that they require. Note, to specify a property that is a [component option](https://camel.apache.org/manual/component.html#_configuring_component_options),
you need to prefix the property with `componentOptions.` in your dest.ini configuration file. For example, to specify the option [brokerURL](https://camel.apache.org/components/3.22.x/activemq-component.html#_component_option_brokerURL) 
for ActiveMQ, you should write the option as `componentOptions.brokerURL=<yourActiveMQUrl>`

| id               | Description                     | Required properties                                        | Commonly used properties                                 | All properties
|------------------|---------------------------------|------------------------------------------------------------| -------------------------------------------------------- |----------------------------------------------------------------------|
| `stdout`         | Write all data to standard out. | None.                                                      |                                                          | https://camel.apache.org/components/3.22.x/stream-component.html     |  
| `slack`          | Send data to a Slack channel    | * `webhook` <br> * `channel`                               |                                                          | https://camel.apache.org/components/3.22.x/slack-component.html      |
| `fluentd`        | Send data to FluentD            | * `tag` <br> * `host` <br> * `port`                        |                                                          |                                                                      |
| `file`           | Send data to a file             | * `file`                                                   |                                                          | https://camel.apache.org/components/3.22.x/stream-component.html     |
| `dir`            | Send data to a directory        | * `dir`                                                    |                                                          | https://camel.apache.org/components/3.22.x/file-component.html                                                                     |    
| `smtp`/`smtps`   | Sent data via email             | * `server` <br> * `subject` <br> * `to` <br> * `from`      | * `port`                                                 | https://camel.apache.org/components/3.22.x/mail-component.html       |
| `sentry`         | Send data into Sentry           | * `dsn`                                                    |                                                          |                                                                      |
| `twilio`         | Send via SMS                    | * `sid` <br> * `token` <br> * `to` <br> * `from`           |                                                          | https://camel.apache.org/components/3.22.x/twilio-component.html     |
| `loki`           | Send data into Grafana Loki     | * `url` <br> * `username` <br> * `password` <br>           |                                                          |                                                                      |
| `http`/`https`   | Send data via http/https        | * `url`                                                    | * `httpMethod` <br> * `x` where x is any query parameter | https://camel.apache.org/components/3.22.x/http-component.html       |
| `activemq`       | Send data to ActiveMQ           | * `destinationName`                                        | * `destinationType` <br> * `brokerURL`                   | https://camel.apache.org/components/3.22.x/activemq-component.html   |
| `splunk-hec`     | Send data to Splunk             | * `splunkUrl` <br> * `token` <br> * `index`                | * `skipTlsVerify`                                        | https://camel.apache.org/components/3.22.x/splunk-hec-component.html |


### Example

```ini
[email_b]
type=smtps
format=Hey, check out this information!! \n\n$FILE_DATA$
  server = my.smtpserver.com
  subject = Testemail
  from=me@mycompany.com
  to=me@mycompany.com

[test_out]
type=stdout

[sentry_out]
type=sentry
dsn=<sentry_dsn>

[twilio_sms]
type=twilio
componentOptions.sid=x
componentOptions.token=x
to=+x
from=+x

[loki_out]
type=loki
url=<loki_url>
username=<loki_username>
password=<loki_password>

[pubsub_out]
type=google-pubsub
projectId=<pubsub_project_id>
topicName=<pubsub_topic_name>
componentOptions.serviceAccountKey=<path_to_pubsub_service_account_key>

[slackme]
type=slack
channel=open-source-system-status
webhook=https://hooks.slack.com/services/TA3EF58G4...

[myLocalHttpServer]
type=http
url=http://localhost:3000
a=54
b=heybuddy
httpMethod=POST
format={"message": "$FILE_DATA$"}

[myProdServer]
type=https
url=https://production.com
foo=bar
httpMethod=POST
format={"message": "$FILE_DATA$", "path": "$FILE_PATH$", "name": "$FILE_NAME$"}

[mq]
type=activemq
destType=queue
destName=TEST.QUEUE
format=This is the $FILE_DATA$
componentOptions.brokerURL=tcp://myactivemq:61616

[splunk]
type=splunk-hec
splunkUrl=<splunk_host>:<splunk_port>
token=<splunk_token>
index=<splunk_index>
```
