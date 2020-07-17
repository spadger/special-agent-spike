package com.spadger.opentracingtest

import io.opentracing.Tracer
import org.apache.kafka.streams.processor.AbstractProcessor
import org.apache.kafka.streams.processor.Processor
import org.apache.kafka.streams.processor.ProcessorContext

/**
 * Used to decorate a preexisting Processor, whatever its base-class is
 */
class OpenTracingProcessorDecorator<K, V>(
    private val inner: Processor<K, V>,
    private val tracer: Tracer
) : Processor<K, V> {
     lateinit var tracingProcessorContext: OpenTracingProcessorContext

    override fun init(context: ProcessorContext) {
        this.tracingProcessorContext = OpenTracingProcessorContext(tracer, inner.javaClass.simpleName, context)
        inner.init(this.tracingProcessorContext)
    }

    override fun process(key: K, value: V) {
        tracingProcessorContext.beginSpan()
        inner.process(key, value)
    }

    override fun close() = inner.close()
}

/**
 * Use this if you need to access the active span
 */
open class OpenTracingAwareProcessor<K, V>(private val tracer: Tracer) : AbstractProcessor<K, V>() {

    private lateinit var tracingProcessorContext: OpenTracingProcessorContext

    override fun init(context: ProcessorContext) {
        this.tracingProcessorContext = OpenTracingProcessorContext(tracer, javaClass.simpleName, context)
        super.init(this.tracingProcessorContext)
    }

    override fun process(key: K, value: V) {
        tracingProcessorContext.beginSpan()
    }

    protected fun setBaggeItem(key: String, value: String) = tracingProcessorContext.span?.setBaggageItem(key, value)
}