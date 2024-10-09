
## Objective

Provide a gateway for publishing IBM i events to a variety of endpoints, which may include user applications, external resources, and/or open source technologies. Example use cases include:

- Monitoring system events with a third-party open source or proprietary tool
- More comprehensive integration with syslog facilities
- Queryable system events
- Consolidated auditing/reporting activity. 

In short, this project aims to make IBM i more integrated with infrastructure monitoring of a heterogeneous IT infrastructure, whether on-prem or cloud.

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

- [ActiveMQ](http://activemq.apache.org/) ⏳
- [AWS Simple Email Service (SES)](https://aws.amazon.com/ses/) ⏳
- [AWS Simple Notification System (SNS)](https://aws.amazon.com/sns/) ⏳
- [ElasticSearch](http://elastic.co) ⏳
- Email (SMTP/SMTPS) ✅
- [FluentD](http://fluentd.org) ✅
- [Google Drive](http://drive.google.com) ⏳
- [Google Pub/Sub](http://cloud.google.com/pubsub) ⏳
- [Grafana Loki](https://grafana.com/oss/loki/) ✅
- HTTP endpoints (REST, etc) ✅
- HTTPS endpoints (REST, etc) ✅
- [Internet of Things (mqtt)](https://www.eclipse.org/paho/) ⏳
- [Kafka](http://kafka.apache.org) ✅
- [Mezmo](http://mezmo.com) ⏳
- [Microsoft Teams](http://teams.microsoft.com) ⏳
- [PagerDuty](http://pagerduty.com) ⏳
- [Sentry](http://sentry.io) ✅
- [Slack](http://slack.com) ✅
- SMS (via [Twilio](http://www.twilio.com)) ✅
- [Splunk](http://splunk.com) ⏳

✅ = implemented
🌗 = partially implemented
⏳ = future

Desired target not on the list? Please open an issue to the repository and let us know!
