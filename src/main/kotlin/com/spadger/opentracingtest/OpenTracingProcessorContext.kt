package com.spadger.opentracingtest

import io.opentracing.Scope
import io.opentracing.Span
import io.opentracing.Tracer
import io.opentracing.contrib.kafka.TracingKafkaUtils
import io.opentracing.log.Fields
import io.opentracing.tag.Tags
import mu.KotlinLogging
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.StreamsMetrics
import org.apache.kafka.streams.processor.*
import java.io.File
import java.time.Duration

private val logger = KotlinLogging.logger {}

class OpenTracingProcessorContext(private val tracer: Tracer, private val processorName: String, private val inner: ProcessorContext):
    ProcessorContext {

    private var span: Span? = null
    private var scope : Scope? = null

    companion object {
        private const val SPAN_KIND_PROCESSOR = "processor"
    }

    fun beginSpan() {
        try {
            val spanContext = TracingKafkaUtils.extractSpanContext(headers(), tracer)
            val spanBuilder = tracer
                .buildSpan(String.format("processor (%s)", processorName))
                .withTag(Tags.SPAN_KIND.key, SPAN_KIND_PROCESSOR)
            if (spanContext != null) {
                spanBuilder.asChildOf(spanContext)
            }
            span = spanBuilder.start()
            scope = tracer.scopeManager().activate(span)
        } catch (e: Exception) {
            if (span != null) {
                Tags.ERROR[span] = true
                span?.log(
                    mapOf(
                        Fields.EVENT to "error",
                        Fields.ERROR_OBJECT to e,
                        Fields.MESSAGE to e.message
                    )
                )
            }
            logger.error("Could not create scope or span.", e)
        }
    }

    private fun closeSpan() {
        try {
            span?.finish()
            scope?.close()
        } catch (e: Exception) {
            logger.error("Could not close scope or span.", e)
        }
    }

    override fun stateDir(): File = inner.stateDir()

    override fun metrics(): StreamsMetrics = inner.metrics()

    override fun schedule(intervalMs: Long, type: PunctuationType?, callback: Punctuator?): Cancellable =
        inner.schedule(intervalMs, type, callback)

    override fun schedule(interval: Duration?, type: PunctuationType?, callback: Punctuator?): Cancellable =
        inner.schedule(interval, type, callback)

    override fun getStateStore(name: String?): StateStore = inner.getStateStore(name)

    override fun commit() = inner.commit()

    override fun appConfigs(): MutableMap<String, Any> = inner.appConfigs()

    override fun appConfigsWithPrefix(prefix: String?): MutableMap<String, Any> = inner.appConfigsWithPrefix(prefix)

    override fun timestamp(): Long = inner.timestamp()

    override fun applicationId(): String = inner.applicationId()

    override fun valueSerde(): Serde<*> = inner.valueSerde()

    override fun topic(): String = inner.topic()

    override fun register(store: StateStore?, stateRestoreCallback: StateRestoreCallback?) = inner.register(store, stateRestoreCallback)

    override fun <K : Any?, V : Any?> forward(key: K, value: V) {
        closeSpan()
        inner.forward(key, value)
    }

    override fun <K : Any?, V : Any?> forward(key: K, value: V, to: To?) {
        closeSpan()
        inner.forward(key, value, to)
    }

    override fun <K : Any?, V : Any?> forward(key: K, value: V, childIndex: Int) {
        closeSpan()
        inner.forward(key, value, childIndex)
    }

    override fun <K : Any?, V : Any?> forward(key: K, value: V, childName: String?) {
        closeSpan()
        inner.forward(key, value, childName)
    }

    override fun keySerde(): Serde<*> = inner.keySerde()

    override fun offset(): Long = inner.offset()

    override fun headers(): Headers = inner.headers()

    override fun taskId(): TaskId = inner.taskId()

    override fun partition(): Int = inner.partition()
}