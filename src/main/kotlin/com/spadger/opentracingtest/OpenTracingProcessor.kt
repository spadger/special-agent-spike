package com.spadger.opentracingtest


import io.opentracing.Scope
import io.opentracing.Span
import io.opentracing.Tracer
import io.opentracing.contrib.kafka.TracingKafkaUtils
import io.opentracing.log.Fields
import io.opentracing.tag.Tags
import mu.KotlinLogging
import org.apache.kafka.streams.processor.Processor
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.processor.ProcessorSupplier

private val logger = KotlinLogging.logger {}

class OpenTracingProcessor<K, V>(
    private val processor: Processor<K, V>,
    private val tracer: Tracer
) : Processor<K, V> {
    private var context: ProcessorContext? = null
    override fun init(context: ProcessorContext) {
        this.context = context
        processor.init(context)
    }

    override fun process(key: K, value: V) {
        var span: Span? = null
        var scope: Scope? = null
        // Catch errors caused by tracing, but not errors caused by the actual processing
        try {
            val spanContext = TracingKafkaUtils.extractSpanContext(context!!.headers(), tracer)
            val spanBuilder = tracer
                .buildSpan(String.format("processor (%s)", processor.javaClass.simpleName))
                .withTag(Tags.SPAN_KIND.key, SPAN_KIND_PROCESSOR)
            if (spanContext != null) {
                spanBuilder.asChildOf(spanContext)
            }
            span = spanBuilder.start()
            scope = tracer.scopeManager().activate(span)
        } catch (e: Exception) {
            if (span != null) {
                Tags.ERROR[span] = true
                span.log(
                    mapOf(
                        Fields.EVENT to  TracingEventType.ERROR,
                        Fields.ERROR_OBJECT to e,
                        Fields.MESSAGE to e.message
                    )
                )
            }
            logger.error("Could not create scope or span.", e)
        }
        processor.process(key, value)
        try {
            span!!.finish()
            scope!!.close()
        } catch (e: Exception) {
            logger.error("Could not close scope or span.", e)
        }
    }

    override fun close() {
        processor.close()
    }

    companion object {
        private const val SPAN_KIND_PROCESSOR = "processor"
    }
}


class OpenTracingProcessorSupplier<K, V>(private val supplier: ProcessorSupplier<K, V>, private val tracer: Tracer) :
    ProcessorSupplier<K, V> {
    override fun get(): Processor<K, V> {
        return OpenTracingProcessor(supplier.get(), tracer)
    }
}

object TracingEventType {
    const val ERROR = "error"
    const val NO_HEADER_IN_MESSAGE = "error: no header in message"
}