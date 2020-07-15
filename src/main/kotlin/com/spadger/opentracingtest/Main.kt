package com.spadger.opentracingtest

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
 logger.info("zaza")
    val id = System.getenv("ID")
    val source = System.getenv("SOURCE")
    val dest = System.getenv("DEST")
    TestTopology(id, source, dest).start()
}