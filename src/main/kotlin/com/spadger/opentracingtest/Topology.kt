package com.spadger.opentracingtest

import mu.KotlinLogging
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.processor.AbstractProcessor
import org.apache.kafka.streams.processor.Processor
import org.apache.kafka.streams.processor.To
import java.util.Properties

private val logger = KotlinLogging.logger {}

class TestTopology(private val id: String, private val source: String, private val dest: String) {

    fun start(){
       val topology = Topology()
            .addSource("source", source)
            .addProcessor("processor", { TestProcessor(id) as Processor<Any, Any> }, arrayOf("source"))
            .addSink("sink", dest, "processor")

        val properties = Properties().apply {
            put(StreamsConfig.APPLICATION_ID_CONFIG, id)
            put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "plaintext://kafka:9092")
            put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde().javaClass)
            put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde().javaClass)
        }

        KafkaStreams(topology, properties).start()
    }
}

class TestProcessor(private val id: String): AbstractProcessor<String, String>() {

    override fun process(key: String, value: String) {
        logger.info("Processing $key")
        context().forward(key,  "FROM: $id  - $value:$value:$value", To.all())
    }
}