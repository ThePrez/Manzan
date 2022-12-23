# Manzan (WIP!! DO NOT USE!!)

# Documentation

Full documentation is available [here](http://theprez.github.io/Manzan/)

# Objective

Provide a gateway for publishing IBM i events to a variety of endpoints, which may include user applications, external resources, and/or open source technologies. Example use cases include:
- Monitoring system events with a third-party open source or proprietary tool
- More comprehensive integration with syslog facilities
- Queryable system events
- Consolidated auditing/reporting activity. 

In short, this project aims to make IBM i more integrated with infrastructure monitoring of a heterogeneous IT infrastructure, whether on-prem or cloud.

# Architecture

![image](https://user-images.githubusercontent.com/17914061/208200501-d0c14907-ed47-4248-ab89-9728e197ddb6.png)


Manzan consists of the following components:
- **Handler**: Receives and handles system watch and exit point events, transforming the data into a usable format
- **Distributor**: Formats, filters, and sends the data to its ultimate destination. Powered by [Apache Camel](http://camel.apache.org)

# Where can I send these events?

Events can be consumed by your own custom ILE code (documentation forthcoming) by simply consuming from Manzan's data queue or Db2 table. 

Many other destinations will be available. Examples include:
- [Slack](http://slack.com)
- Email
- SMS
- [Sentry](http://sentry.io)
- [Splunk](http://splunk.com)
- [FluentD](http://fluentd.org)
- [Kafka](http://kafka.apache.org)
- [ActiveMQ](http://activemq.apache.org/)
- [Grafana Loki](https://grafana.com/oss/loki/)
- [Mezmo](http://mezmo.com)
- [ElasticSearch](http://elastic.co)



# Why the name "Manzan"?

(documentation forthcoming)
