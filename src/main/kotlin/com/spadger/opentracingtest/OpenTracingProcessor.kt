package com.spadger.opentracingtest


import io.opentracing.Tracer
import org.apache.kafka.streams.processor.Processor
import org.apache.kafka.streams.processor.ProcessorContext

class OpenTracingProcessor<K, V>(
    private val inner: Processor<K, V>,
    private val tracer: Tracer
) : Processor<K, V> {
    private lateinit var tracingProcessorContext: OpenTracingProcessorContext

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

