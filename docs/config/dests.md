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

## Available types

Some types have additional properties that they required.

| id       | Description                     | Required properties                                        |
|----------|---------------------------------|------------------------------------------------------------|
| `stdout`   | Write all data to standard out. | None.                                                      |
| `slack`    | Send data to a Slack channel    | * `webhook` <br> * `channel` <br>                          |
| `fluentd`  | Sent data to FluentD            | * `tag` <br> * `host` <br> * `port` <br>                   |
| `smtp`     | Sent data via email             | * `server` <br> * `subject` <br> * `to` <br> * `from` <br> |
| `sentry` | Send data into Sentry           | `dsn`                                                      |
| `twilio` | Send via SMS | * `sid` <br> * `token` <br> * `to` <br> * `from` |

### Example

```ini
[email_b]
type=smtp
format=Hey, check out this information!! \n\n$FILE_DATA$
  server = my.smtpserver.com
  subject = Testemail
  from=me@mycompany.com
  to=me@mycompany.com

[test_out]
type=stdout

[sentry_out]
type=sentry
dsn=<slackdsn>

[twilio_sms]
type=twilio
sid=x
token=x
to=+x
from=+x

[slackme]
type=slack
channel=open-source-system-status
webhook=https://hooks.slack.com/services/TA3EF58G4...
```
