---
title: Lagom 1.2.0 is almost here!
date: 2016-10-25
author_github: jroper
tags: news
summary: >
    Lagom 1.2.0-RC2 has been released, and we are expecting that 1.2.0 will be released any day now!

---

We're happy to announce that Lagom 1.2.0-RC2 has been released!

This release has a small reworking of the read side event tags so that events can be automatically sharded by Lagom. Previously, your `aggregateTag` function had to compute it's own sharded tags if you wanted to shard your read sides. Now, you can simply return a [single meta tag](//www.lagomframework.com/documentation/1.2.x/java/api/index.html?com/lightbend/lagom/javadsl/persistence/AggregateEventTag.html#sharded-java.lang.Class-int-) to say that the events should be sharded, and how many shards you want, and Lagom will handle the sharding for you. Read [the documentation](//www.lagomframework.com/documentation/1.2.x/java/ReadSide.html#event-tags) for more details.

We've also made some minor improvements around how Kafka message keys are [extracted from messages](//www.lagomframework.com/documentation/1.2.x/java/MessageBrokerApi.html#Partitioning-topics), and provided topic testing support in the testkit.

On the documentation side, there's more documentation for using and testing Kafka, and most importantly, we've written a [migration guide](//www.lagomframework.com/documentation/1.2.x/java/Migration12.html), outlining any changes you may need to make in order to move from Lagom 1.1 to Lagom 1.2.

Lagom 1.2.0 should be released very soon, so stay tuned!

