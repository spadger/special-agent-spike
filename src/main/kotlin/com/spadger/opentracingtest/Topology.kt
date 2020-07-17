package com.spadger.opentracingtest

import io.jaegertracing.Configuration
import io.jaegertracing.Configuration.*
import io.opentracing.Tracer
import io.opentracing.contrib.kafka.streams.TracingKafkaClientSupplier
import mu.KotlinLogging
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.processor.AbstractProcessor
import org.apache.kafka.streams.processor.Processor
import org.apache.kafka.streams.processor.To
import java.util.*
import kotlin.random.Random


private val logger = KotlinLogging.logger {}

class TestTopology(private val id: String, private val source: String, private val dest: String) {

    fun start(){

        val tracer = getTracer(id)

        val topology = Topology()
            .addSource("source", source)

           .addProcessor(
                "processor-1",
               { OpenTracingProcessor(TestProcessor1(id) as Processor<Any, Any>, tracer) },
                arrayOf("source"))

           .addProcessor(
                "processor-2",
               { OpenTracingProcessor(TestProcessor2(id) as Processor<Any, Any>, tracer) },
                arrayOf("processor-1"))

           .addProcessor(
                "processor-3",
                { OpenTracingProcessor(TestProcessor3(id) as Processor<Any, Any>, tracer) },
                arrayOf("processor-2"))

           .addSink("sink", dest, "processor-3")

        val properties = Properties().apply {
            put(StreamsConfig.APPLICATION_ID_CONFIG, id)
            put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "plaintext://kafka:9092")
            put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde().javaClass)
            put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde().javaClass)
        }

        val clientSupplier = TracingKafkaClientSupplier(tracer)

        KafkaStreams(topology, properties, clientSupplier).start()
    }

    fun getTracer(serviceName: String) : Tracer {

        val tracer = Configuration(serviceName)
            .withReporter(
                ReporterConfiguration()
                    .withLogSpans(true)
                    .withFlushInterval(500)
                    .withMaxQueueSize(50)
                    .withSender(SenderConfiguration()
                        .withAgentHost("jaeger")
                        .withAgentPort(6831)
                )
            )
            .withSampler(
                SamplerConfiguration()
                    .withType("const")
                    .withParam(1)
            )
            .tracerBuilder
            .build()

        return tracer
    }
}

abstract class TestProcessor(private val id: String, private val ordinal: Int): AbstractProcessor<String, String>() {

    override fun process(key: String, value: String) {
        logger.info("Processor-$ordinal: $key")

        Thread.sleep(Random.nextLong(150, 1500))

        context().forward(key,  "$id-$ordinal - $value", To.all())
    }
}

class TestProcessor1(private val id: String) : TestProcessor(id, 1)
class TestProcessor2(private val id: String) : TestProcessor(id, 2)
class TestProcessor3(private val id: String) : TestProcessor(id, 3)