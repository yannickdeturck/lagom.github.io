---
title: Lagom 1.1 is released!
date: 2016-09-21
author_github: jroper
tags: news
summary: >
    The Lagom team are proud to announce the release of Lagom 1.1!  The primary new feature of Lagom 1.1 is maven support - this includes support for running the Lagom development environment in Maven.
---

The Lagom team are proud to announce the release of Lagom 1.1!  The primary new feature of Lagom 1.1 is maven support - this includes support for running the Lagom development environment in Maven.

Although running many services with hot reloads in Maven is not a traditional thing to do, as much as possible the Lagom Maven plugin makes idiomatic usage of Maven, meaning that it's still compatible with whatever Maven plugins or tooling that you use, including IDEs, code generators, testing tools and so on.

Lagom's Maven integration also supports starting a full system environment as part of your integration tests, so you can do comprehensive testing of inter service communication.

You can get started today with a Lagom starter app by running:

```
mvn archetype:generate -DarchetypeGroupId=com.lightbend.lagom \
  -DarchetypeArtifactId=maven-archetype-lagom-java -DarchetypeVersion=1.1.0
```

For more details, read the documentation on [Getting started with Maven](http://www.lagomframework.com/documentation/1.1.x/java/GettingStartedMaven.html).  Enjoy!
