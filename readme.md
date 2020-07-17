## Kafka-Streams OpenTracing
What is it?
A JVM (kotlin) playground to test Kafka-Streams & OpenTracing integration. <br>
Started off as a testbed for [java-specialagent](https://github.com/opentracing-contrib/java-specialagent) 's Kafka integration, but is now a demo on how to instrument individual kafka-streams Processors

# How does it work
First, it makes use of [OpenTracing-contrib's Kafka Client](https://github.com/opentracing-contrib/java-kafka-client) to ensure spans are setup / reused correctly
The main aspect of development here is how it instruments individual processors.

There are tow main ways of integrating: The decorator style and inheritance style, each with their own strengths:

## Decorator style
For simple cases, the decorator style allows you to keep all of your processors unmodified, with integraiton being added in the topology definition.
This approach allows your processors to inherit from any base-class.

Ensure each processor supplier returns your processor decorated with the `OpenTracingProcessor`. The `OpenTracingProcessor` starts a new span as soon as `process(key, value)` is invoked, but also ensures a special `OpenTracingProcessorContext` is used.
This wraps the standard `ProcessorContext`, which closes off a `Span` whenever the processor invokes `context.forward(...)` or `context.commit()`
```kotlin
 val topology = Topology()
    .addSource("source", source)

   .addProcessor(
        "processor",
       { SomeProcessor() as Processor<Any, Any> },
        arrayOf("source"))

   .addSink("sink", dest, "processor")
``` 
with
```kotlin
 val topology = Topology()
    .addSource("source", source)

   .addProcessor(
        "processor",
       { OpenTracingProcessorDecorator(SomeProcessor() as Processor<Any, Any>, tracer) },
        arrayOf("source"))

   .addSink("sink", dest, "processor")
``` 

## Inheritance style
If you need access to baggage, use the inheritor style. Your processor will need to inherit from `OpenTracingAwareProcessor`, and must call `super.process()`.  Afterward, you will get access to the baggage collection

```kotlin
class SomeProcessor(tracer: Tracer) : OpenTracingAwareProcessor<String, String>(tracer){
    override fun process(key: String, value: String) {
        super.process(key, value) //important
        super.setBaggeItem("correlationId", UUID.randomUUID().toString())

        context().forward(key, value, To.all())
    }
}
```

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