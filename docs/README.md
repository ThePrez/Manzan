# 🎆👉**Manzan is now available as a Technology Preview!** 👈 🎆

## Objective

Provide a gateway for publishing IBM i events to a variety of endpoints, which may include user applications, external resources, and/or open source technologies. Example use cases include:

- Monitoring system events with a third-party open source or proprietary tool
- More comprehensive integration with syslog facilities
- Queryable system events
- Consolidated auditing/reporting activity. 

In short, this project aims to make IBM i more integrated with infrastructure monitoring of a heterogeneous IT infrastructure, whether on-prem or cloud.

## Why the name "Manzan"?
Manzan, the open source project designed to simplify IBM i system operations and monitoring, owes its name to a
tranquil haven that brings solace to its creator, Jesse Gorzinski. Just as the gentle lapping of waves against the
shore can calm the mind, Manzan aims to soothe the stresses that come with managing complex systems. By streamlining
tasks and making the mundane effortless, Manzan helps IT professionals find their own peaceful haven amidst the chaos
of their daily work. With Manzan, the ebb and flow of system administration becomes a breeze, allowing users to focus
on more important things – just as Jesse's favorite retreat helps him clear his mind and recharge. By harnessing the
power of Manzan, users can let their worries wash away, leaving them feeling refreshed and in control.

## Architecture

![image](https://user-images.githubusercontent.com/17914061/208200501-d0c14907-ed47-4248-ab89-9728e197ddb6.png)

Manzan consists of the following components:

### Handler:

Receives and handles system watch and exit point events, transforming the data into a usable format. Read about the types of data we handle.

### Distributor

Formats, filters, and sends the data to its ultimate destination. Powered by [Apache Camel](http://camel.apache.org).


## Where can I send these events?

Events can be consumed by your own custom ILE code (documentation forthcoming) by simply consuming from Manzan's data queue or Db2 table. 

Many other destinations will be available. Examples include:

- [ActiveMQ](http://activemq.apache.org/) ✅
- [AWS Simple Email Service (SES)](https://aws.amazon.com/ses/) ⏳
- [AWS Simple Notification System (SNS)](https://aws.amazon.com/sns/) ⏳
- [Elasticsearch](http://elastic.co) ✅
- Email (SMTP/SMTPS) ✅
- [FluentD](http://fluentd.org) ✅
- [Google Drive](http://drive.google.com) ⏳
- [Google Pub/Sub](http://cloud.google.com/pubsub) ✅
- [Grafana Loki](https://grafana.com/oss/loki/) ✅
- HTTP endpoints (REST, etc) ✅
- HTTPS endpoints (REST, etc) ✅
- [Internet of Things (mqtt)](https://www.eclipse.org/paho/) ⏳
- [Kafka](http://kafka.apache.org) ✅
- [Mezmo](http://mezmo.com) ✅
- [Microsoft Teams](http://teams.microsoft.com) ⏳
- [PagerDuty](http://pagerduty.com) ✅
- [Sentry](http://sentry.io) ✅
- [Slack](http://slack.com) ✅
- SMS (via [Twilio](http://www.twilio.com)) ✅
- [Splunk](http://splunk.com) ✅

✅ = implemented
🌗 = partially implemented
⏳ = future

Desired target not on the list? Please open an issue to the repository and let us know!
