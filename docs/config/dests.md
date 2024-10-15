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

Some types have additional properties that they require.

| id               | Description                     | Required properties                                              | Optional properties                                      |
|------------------|---------------------------------|------------------------------------------------------------------|--------------------------------------------------------- |
| `stdout`         | Write all data to standard out. | None.                                                            |                                                          |
| `slack`          | Send data to a Slack channel    | * `webhook` <br> * `channel`                                     |                                                          |
| `fluentd`        | Sent data to FluentD            | * `tag` <br> * `host` <br> * `port`                              |                                                          |
| `smtp`/`smtps`   | Sent data via email             | * `server` <br> * `subject` <br> * `to` <br> * `from`            | * `port`                                                 |
| `sentry`         | Send data into Sentry           | * `dsn`                                                          |                                                          |
| `twilio`         | Send via SMS                    | * `sid` <br> * `token` <br> * `to` <br> * `from`                 |                                                          |
| `loki`           | Send data into Grafana Loki     | * `url` <br> * `username` <br> * `password` <br>                 |                                                          |
| `google-pubsub`  | Send data into Google Pub/Sub   | * `projectId` <br> * `topicName` <br> * `serviceAccountKey` <br> |                                                          |
| `http`/`https`   | Send data via http/https        | * `url`                                                          | * `httpMethod` <br> * `x` where x is any query parameter |

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
sid=x
token=x
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
serviceAccountKey=<path_to_pubsub_service_account_key>

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
```
