## Kafka-Streams OpenTracing
What is it?
A JVM (kotlin) playground to test Kafka-Streams & OpenTracing integration. <br>
Started off as a testbed for [java-specialagent](https://github.com/opentracing-contrib/java-specialagent) 's Kafka integration, but is now a demo on how to instrument individual kafka-streams Processors

# How does it work
First, it makes use of [OpenTracing-contrib's Kafka Client](https://github.com/opentracing-contrib/java-kafka-client) to ensure spans are setup / reused correctly
The main aspect of development here is how it instruments individual processors.

There are tow main ways of integrating: The decorator style and inheritance style, each with their own strengths:

## Decorator style
For simple cases, the decorator style allows you to keep all of your processors unmodified, with integration added in the topology definition.
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

n.b. if you need direct access to trace data, you can still achieve this using the `GlobalTracer`.  When you register your `Tracer`, add this line below
```kotlin
GlobalTracer.registerIfAbsent(tracer)
```

You will then be able to write trace data form your unmodified Processors:

```kotlin
GlobalTracer.get().scopeManager().activeSpan().setTag("z:some-key", "some-value")
```

## Inheritance style
This style provides a slightly-easier to read topology, and simpler access to record Tags to your trace, but your processor will need to inherit from `OpenTracingAwareProcessor`, and must call `super.process()`.  
Since there is no decoration, your topology will look unmodified.

```kotlin
class SomeProcessor(tracer: Tracer) : OpenTracingAwareProcessor<String, String>(tracer){
    override fun process(key: String, value: String) {
        
        // Important, since this initialises the span
        super.process(key, value)

        // direct access to add tags from the Processor        
        setTag("z:correlationId", UUID.randomUUID().toString())

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