---
title: What's next in Lagom 1.2?
date: 2016-09-30
author_github: jroper
tags: news
summary: >
 The Lagom team have released the first milestone of Lagom 1.2, with three new major features to try out.
---

Today we released Lagom 1.2.0-RC1, the first release candidate for the upcoming Lagom 1.2. This release has three exciting new features that we'd like to tell you about.

## Message broker support

The biggest feature of Lagom 1.2 is message broker support. This means Lagom now allows both direct streaming of messages between services, as well as streaming of messages through a broker.

Lagom's message broker support does not require any one particular message broker implementation, however out of the box we have only implemented support for Kafka. Kafka is a popular very scalable message broker, and fits well in the Lagom architectural philosophy.

Lagom will automatically run Kafka for you when you run the `runAll` command, both in Maven and sbt, which makes getting started with a project that uses Kafka very simple. It also abstracts the publishing, partitioning, consuming and failure handling of messaging away from you, so you can focus primarily on your business concerns.

As with Lagom `ServiceCall`'s, which provide a protocol agnostic way of directly communicating with other services that maps down onto HTTP, Lagom provides a new `Topic` abstraction, which represents a topic that one service publishes, and one or more services consume. Through the use of service descriptors, Lagom allows all the serialization and deserialization logic for these messages to be captured and shared between services.

As a first and primary use case, we have targetted publishing a persistent entity event stream for the source of topics. This allows the implementation of remote read sides, as well as provides a great basis for guaranteed at least once delivery of events between services.

To explore more of Lagom's message broker support, read the [documentation](http://www.lagomframework.com/documentation/1.2.x/java/MessageBroker.html).

## JDBC support

Feedback that we got from many potential users was that in order to introduce Lagom into their organisation, it would need to have support for using their existing relational database infrastructure. While a relational database is often not the best choice, particularly for storing event sourced entities, it's not necessarily a bad choice. We felt that having no JDBC support was an unnecessary blocker to organisations getting the benefits of a Lagom based architecture.

Lagom supports JDBC both for [storing persistent entities](http://www.lagomframework.com/documentation/1.2.x/java/PersistentEntityRDBMS.html), as well as for implementing [read-sides](http://www.lagomframework.com/documentation/1.2.x/java/ReadSideRDBMS.html).

## Read-side overhaul

Lagom's read-side support has been overhauled. There are three major parts to this. First of all, read sides that talk to any data store can now be implemented - previously the read side processor API was specific to Cassandra. We still provide a Cassandra specific utility for building Cassandra read-sides, as well as a new JDBC specific utility for building JDBC read-sides, but these are just helpers for building read-sides, you can build a read-side in anything. Documentation for the new read-side API can be found [here](http://www.lagomframework.com/documentation/1.2.x/java/ReadSide.html).

The second is that read-sides can now be sharded, by tagging persistent entity events with sharded tags. Lagom's read side processors now declare a list of tags that they process, rather than just one, and Lagom will automatically distribute the processing of these tags across the cluster. This is great for services with a high throughput of events, or when event processing can be very slow or expensive.

The final feature is that both the Cassandra and JDBC specific read side support utilities provide automatic offset tracking, meaning that your read side processors no longer need to explicitly load and persist offsets. This makes it much simpler to implement a read side processor. Documentation is available for both the [Cassandra read-side support](http://www.lagomframework.com/documentation/1.2.x/java/ReadSideCassandra.html) and the [JDBC read-side support](http://www.lagomframework.com/documentation/1.2.x/java/ReadSideRDBMS.html).

The existing Cassandra read-side support is still supported, but is deprecated.

## Onwards to 1.2.0

We are hoping to release Lagom 1.2.0 in the coming month, so please check it out, jump on the [Gitter channel](https://gitter.im/lagom/lagom) and give us feedback!
