package com.spadger.opentracingtest

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
 logger.info("zaza")
    val id = System.getenv("ID") ?: "local"
    val source = System.getenv("SOURCE") ?: "topic-a"
    val dest = System.getenv("DEST") ?: "topic-b"
    TestTopology(id, source, dest).start()
}