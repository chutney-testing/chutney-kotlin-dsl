package com.chutneytesting.kotlin.dsl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.util.regex.Pattern

class ChutneyScenarioDslGenerator {

    fun generateDsl(content: String): String {
        val chutneyScenario = jacksonObjectMapper().readValue<ChutneyScenario>(content)
        val dsl = """
                >val `${chutneyScenario.title}` = Scenario(title = "${chutneyScenario.title}") {
                >    ${mapGivens(chutneyScenario)}
                >    ${mapWhen(chutneyScenario)}
                >    ${mapThens(chutneyScenario)}
                >}
            """.trimMargin(">")

        return dsl

    }

    fun generateDsl(file: File) = generateDsl(file.readText())

    private fun mapGivens(chutneyScenario: ChutneyScenario): String {
        return chutneyScenario.givens.mapIndexed { index: Int, step: ChutneyStep ->
            gwta(index, "Given") + "(\"${step.description}\") ${step.toDsl()}"
        }.joinToString(separator = "\n")
    }

    private fun mapWhen(chutneyScenario: ChutneyScenario) =
        """When("${chutneyScenario.`when`?.description}") ${chutneyScenario.`when`?.toDsl()}"""

    private fun mapThens(chutneyScenario: ChutneyScenario): String {
        return chutneyScenario.thens.mapIndexed { index: Int, step: ChutneyStep ->
            gwta(index, "Then") + "(\"${step.description}\") ${step.toDsl()}"
        }.joinToString(separator = "\n")
    }

    private fun gwta(index: Int, gt: String) = if (index == 0) gt else "And"
}

private fun ChutneyStep.toDsl(): String {
    if (this.subSteps.isNullOrEmpty()) {
        val implementation = this.implementation ?: return mapTODO()
        return when (implementation.type) {
            "context-put" -> mapContexPutTask(implementation)
            "http-get" -> mapHttpGetTask(implementation)
            "http-post" -> mapHttpPostTask(implementation)
            "amqp-clean-queues" -> mapAmqpCleanQueuesTask(implementation)
            "amqp-basic-consume" -> mapAmqpBasicConsumeTask(implementation)
            "json-assert" -> mapJsonAssertTask(implementation)
            "string-assert" -> mapStringAssertTask(implementation)
            "sql" -> mapSqlTask(implementation)
            "sleep" -> mapSleepTask(implementation)
            "debug" -> mapDebugTask(implementation)
            else -> mapTODO()
        }
    } else {
        return this.subSteps
            .joinToString(
                separator = "\n",
                prefix = " {\n",
                postfix = "}\n"
            ) { "Step(\"${it.description}\") ${it.toDsl()}" }

    }
}

private fun mapTODO(): String {
    return """{
       TODO("Not yet implemented")
    }"""
}

private fun mapDebugTask(implementation: ChutneyStepImpl): String {
    return """{
       DebugTask()
    }"""
}

private fun mapSleepTask(implementation: ChutneyStepImpl): String {
    val inputs = implementation.inputs
    val duration = inputAsString(inputs, "duration")
    val listOfArgs = listOf(
        "duration" to duration
    )
    val args = mapArgs(listOfArgs)
    return """{
       SleepTask($args)
    }"""
}

fun mapAmqpBasicConsumeTask(implementation: ChutneyStepImpl): String {
    val inputs = implementation.inputs
    val selector = inputAsString(inputs, "selector")
    val queueName = inputAsString(inputs, "queue-name")
    val timeout = inputAsString(inputs, "queue-name")
    val nbMessages = inputs.get("nb-messages") as Int? ?: 1
    val outputs = outputsAsMap(implementation)
    val target = target(implementation)
    val listOfArgs = listOf(
        "target" to target,
        "queueName" to queueName,
        "nbMessages" to nbMessages,
        "timeout" to timeout,
        "selector" to selector,
        "outputs" to outputs
    )
    val args = mapArgs(listOfArgs)
    return """{
        AmqpBasicConsumeTask($args)
    }"""
}

fun mapJsonAssertTask(implementation: ChutneyStepImpl): String {
    val inputs = implementation.inputs
    val document = inputAsString(inputs, "document")
    val expected = inputAsMap(inputs, "expected")
    val listOfArgs = listOf("document" to document, "expected" to expected)
    val args = mapArgs(listOfArgs)
    return """{
        JsonAssertTask($args)
    }"""
}

fun mapSqlTask(implementation: ChutneyStepImpl): String {
    val inputs = implementation.inputs
    val statements = inputAsList(inputs, "statements")
    val outputs = outputsAsMap(implementation)
    val listOfArgs = listOf("statements" to statements, "outputs" to outputs)
    val args = mapArgs(listOfArgs)
    return """{
        SqlTask($args)
    }"""
}

fun mapStringAssertTask(implementation: ChutneyStepImpl): String {
    val inputs = implementation.inputs
    val document = inputAsString(inputs, "document")
    val expected = inputAsString(inputs, "expected")
    val listOfArgs = listOf("document" to document, "expected" to expected)
    val args = mapArgs(listOfArgs)
    return """{
        StringAssertTask($args)
    }"""
}

fun mapAmqpCleanQueuesTask(implementation: ChutneyStepImpl): String {
    val inputs = implementation.inputs
    val queueNames = inputAsString(inputs, "queueNames")
    val target = target(implementation)
    val listOfArgs = listOf("target" to target, "queueNames" to queueNames)
    val args = mapArgs(listOfArgs)
    return """{
        AmqpCleanQueuesTask($args)
    }"""
}

fun mapHttpGetTask(implementation: ChutneyStepImpl): String {
    val inputs = implementation.inputs
    val headers = inputAsMap(inputs, "headers")
    val outputs = outputsAsMap(implementation)
    val target = target(implementation)
    val uri = uri(implementation)
    val timeout = inputAsString(inputs, "timeout")
    val listOfArgs = listOf(
        "target" to target,
        "uri" to uri,
        "headers" to headers,
        "timeout" to timeout,
        "outputs" to outputs,
        "strategy" to null
    )
    val args = mapArgs(listOfArgs)
    return """{
        HttpGetTask($args)
    }"""
}

private fun mapContexPutTask(implementation: ChutneyStepImpl): String {
    val input = implementation.inputs
    val entries = inputAsMap(input, "entries")
    val listOfArgs = listOf(
        "entries" to entries
    )
    val args = mapArgs(listOfArgs)
    return """{
        ContextPutTask($args)
    }"""
}

private fun outputsAsMap(implementation: ChutneyStepImpl) =
    mapOfConstructor(implementation.outputs)

private fun inputAsString(inputs: Map<String, Any>, key: String) =
    escapeKotlin((inputs.get(key) as String? ?: "")).wrapWithQuotes()

private fun mapArgs(listOfArgs: List<Pair<String, Any?>>): String {
    return listOfArgs
        .filterNot { it.second == null || it.second == "".wrapWithQuotes() || it.second == "mapOf()" || it.second == "listOf()" }
        .joinToString(", ") { it.first + " = " + it.second }
}

private fun inputAsList(inputs: Map<String, Any>, key: String) =
    listOfConstructor(inputs.get(key) as List<String>?)

private fun inputAsMap(inputs: Map<String, Any>, key: String) =
    mapOfConstructor(inputs.get(key) as Map<String, Any>?)

fun mapHttpPostTask(implementation: ChutneyStepImpl): String {
    val inputs = implementation.inputs
    val headers = inputAsMap(inputs, "headers")
    val body = if (inputs.get("body") is Map<*, *>) inputAsMap(inputs, "body") else inputAsString(inputs, "body")
    val outputs = outputsAsMap(implementation)
    val target = target(implementation)
    val uri = uri(implementation)
    val timeout = inputAsString(inputs, "timeout")
    val listOfArgs = listOf(
        "target" to target,
        "uri" to uri,
        "headers" to headers,
        "body" to body,
        "timeout" to timeout,
        "outputs" to outputs,
        "strategy" to null
    )
    val args = mapArgs(listOfArgs)

    return """{
        HttpPostTask($args)
    }"""
}

fun uri(implementation: ChutneyStepImpl): String {
    val inputs = implementation.inputs
    return escapeKotlin((inputs.get("uri") as String? ?: "")).wrapWithQuotes()
}

private fun target(implementation: ChutneyStepImpl): String = (implementation.target as String).wrapWithQuotes()

private fun String.wrapWithQuotes(): String {
    return "\"$this\""
}

private fun listOfConstructor(
    list: List<String>?
): String {
    if (list == null) {
        return "listOf()"
    }
    return "listOf(${
    list.joinToString(",\n") {
        it.split("\n").map { (escapeKotlin(it)).wrapWithQuotes() }.joinToString(" +\n")
    }
    })"
}

private fun mapOfConstructor(
    entries: Map<String, Any>?
): String {
    if (entries == null) {
        return "mapOf()"
    }
    return "mapOf(${
    entries.map {
        "\"${it.key}\" to \"${
        escapeKotlin(
            if (it.value is Map<*, *>) {
                jacksonObjectMapper().writeValueAsString(it.value as Map<*, *>)
            } else it.value.toString() //TODO check when is Int
        )
        }\""
    }.joinToString(",\n")
    })"
}

fun escapeKotlin(s: String): String {
    return s//.replace("'$", "'£")
        .replace("\${", "\\\${")
        .replace("\"", "\\\"")
    //.replace("'£", "'$")
}

val spelPattern = Pattern.compile("\\$\\{#(.*?)}")

fun toKotlinSpelString(s: String): String {
    val sb = StringBuffer()
    val m = spelPattern.matcher(s)
    while (m.find()) {
        m.appendReplacement(sb, "\\$\\{\"${m.group(1)}\".spEL()}")
    }
    m.appendTail(sb)
    return sb.toString()
}
