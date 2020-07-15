## special-agent-spike
What is it?
A JVM (kotlin) playground to test Kafka-Streams & java-specialagent integration

# How do I build it
You'll need:

* Bash
* Make
* JDK11
* Docker (with compose) Gradle (to bootstrap the wrapper)

```bash
./gradlew assemble installDist
```

## How do I run it?

The stack consists of Kafka, Zookeeper, Jaeger and a couple instances of the app, sunning in docker-compose

```
make run
```