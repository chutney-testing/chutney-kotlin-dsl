package com.chutneytesting.kotlin.dsl.transformation.from_component_to_kotlin

import com.chutneytesting.kotlin.dsl.SSH_CLIENT_CHANNEL
import com.chutneytesting.kotlin.util.http.ChutneyServerInfo
import com.chutneytesting.kotlin.util.http.HttpClient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.commons.lang3.StringEscapeUtils

fun generateDsl(serverInfo: ChutneyServerInfo) {
    val allComponents: List<ComposableStepDto> = HttpClient.get(serverInfo, "/api/steps/v1/all")
    var result = ""
    allComponents.forEach { component ->
        if (component.steps?.size!! == 0) {
            // Leaf component
            result += (generateComponent(component) + "\n")

        } else {
            // Parent component
            result += (generateParentComponent(component) + "\n")
        }
    }
    System.out.println(result)
}

private fun generateParentComponent(component: ComposableStepDto): String {
    var result = """
        ${kotlinHeader(component)}
        ${kotlinFunctionName(component)}
    """
    result += component.steps?.let {
        steps -> steps.map { step ->
             """
                ${kotlinCallFunction(step)}
            """
        }
        .joinToString(separator = "\n")

    } ?: run {
        ""
    }
    result+= "}"
    return result
}

private fun generateComponent(component: ComposableStepDto): String {
    return when (component.task?.type) {
        "context-put" -> mapContexPutTask(component)
        "http-get" -> mapHttpGetTask(component)
        "http-post" -> mapHttpPostTask(component)
        "http-put" -> mapHttpPutTask(component)
        "amqp-clean-queues" -> mapAmqpCleanQueuesTask(component)
        "amqp-basic-consume" -> mapAmqpBasicConsumeTask(component)
        "json-assert" -> mapJsonAssertTask(component)
        "json-compare" -> mapJsonCompareTask(component)
        "string-assert" -> mapStringAssertTask(component)
        "sql" -> mapSqlTask(component)
        "sleep" -> mapSleepTask(component)
        "assert" -> mapAssertsTask(component)
        "debug" -> mapDebugTask(component)
        "groovy" -> mapGroovyTask(component)
        "ssh-client" -> mapSshClientTask(component)
        "compare" -> mapCompareTask(component)
        else -> mapTODO(component)
    }
}

private fun mapTODO(component: ComposableStepDto): String {
    return """{
       TODO("Not yet implemented") ${component.task?.type}
    }"""
}

private fun mapDebugTask(implementation: ComposableStepDto): String {
    return createStep(implementation, "", "DebugTask")
}

private fun mapCompareTask(implementation: ComposableStepDto): String {
    val inputs = implementation.task?.inputs
    val actual = inputAsString(inputs, "actual")
    val expected = inputAsString(inputs, "expected")
    val mode = inputAsString(inputs, "mode")
    val listOfArgs = listOf(
        "actual" to actual,
        "expected" to expected,
        "mode" to mode
    )
    val args = mapArgs(listOfArgs)
    return createStep(implementation, args, "CompareTask")
}

private fun mapGroovyTask(implementation: ComposableStepDto): String {
    val inputs = implementation.task?.inputs
    val script = inputAsString(inputs, "script")
    val parameters =  inputAsMap(inputs, "parameters")
    val listOfArgs = listOf(
        "script" to "\"\"" + script + "\"\"",
        "parameters" to parameters
    )
    val args = mapArgs(listOfArgs)
    return createStep(implementation, args, "GroovyTask")
}

private fun mapSshClientTask(implementation: ComposableStepDto): String {
    val target = target(implementation.task)
    val inputs = implementation.task?.inputs
    var channel = SSH_CLIENT_CHANNEL.COMMAND
    try {
        channel = SSH_CLIENT_CHANNEL.valueOf(inputAsString(inputs, "channel"))
    } catch (e: Exception) {
    }

    val commands =  inputs?.let { inputAsList(it, "commands") }
    val listOfArgs = listOf(
        "commands" to commands,
        "channel" to "SSH_CLIENT_CHANNEL." + channel,
        "target" to target
    )
    val args = mapArgs(listOfArgs)
    return createStep(implementation, args, "SshClientTask")
}
private fun mapSleepTask(implementation: ComposableStepDto): String {
    val inputs = implementation.task?.inputs
    val duration = inputAsString(inputs, "duration")
    val listOfArgs = listOf(
        "duration" to duration
    )
    val args = mapArgs(listOfArgs)
    return createStep(implementation, args, "SleepTask")
}

fun mapAssertsTask(implementation: ComposableStepDto): String {
    val input = implementation.task?.inputs
    val asserts = input?.let { inputAsList(it, "asserts") }
    val listOfArgs = listOf(
        "asserts" to asserts
    )
    val args = mapArgs(listOfArgs)
    return createStep(implementation, args, "AssertTrueTask")
}


fun mapAmqpBasicConsumeTask(implementation: ComposableStepDto): String {
    val inputs = implementation.task?.inputs
    val selector = inputAsString(inputs, "selector")
    val queueName = inputAsString(inputs, "queue-name")
    val timeout = inputAsString(inputs, "timeout")
    val nbMessages = inputs?.get("nb-messages") as Int? ?: 1
    val outputs = outputsAsMap(implementation.task)
    val target = target(implementation.task)
    val listOfArgs = listOf(
        "target" to target,
        "queueName" to queueName,
        "nbMessages" to nbMessages,
        "timeout" to timeout,
        "selector" to selector,
        "outputs" to outputs
    )
    val args = mapArgs(listOfArgs)
    return createStep(implementation, args, "AmqpBasicConsumeTask")
}

fun mapJsonAssertTask(implementation: ComposableStepDto): String {
    val inputs = implementation.task?.inputs
    val document = inputAsString(inputs, "document")
    val expected = inputAsMap(inputs, "expected")
    val listOfArgs = listOf("document" to document, "expected" to expected)
    val args = mapArgs(listOfArgs)
    return createStep(implementation, args, "JsonAssertTask")
}

fun mapJsonCompareTask(implementation: ComposableStepDto): String {
    val inputs = implementation.task?.inputs
    val document1 = inputAsString(inputs, "document1")
    val document2 = inputAsString(inputs, "document2")
    val comparingPaths = inputAsMap(inputs, "comparingPaths")
    val listOfArgs = listOf("document1" to document1, "document2" to document2, "comparingPaths" to comparingPaths)
    val args = mapArgs(listOfArgs)
    return createStep(implementation, args, "JsonCompareTask")
}

fun mapSqlTask(implementation: ComposableStepDto): String {
    val inputs = implementation.task?.inputs
    val statements = inputs?.let { inputAsList(it, "statements") }
    val outputs = outputsAsMap(implementation.task)
    val target = target(implementation.task)
    val listOfArgs = listOf(
        "statements" to statements,
        "outputs" to outputs,
        "target" to target
    )
    val args = mapArgs(listOfArgs)
    return createStep(implementation, args, "SqlTask")
}

fun mapStringAssertTask(implementation: ComposableStepDto): String {
    val inputs = implementation.task?.inputs
    val document = inputAsString(inputs, "document")
    val expected = inputAsString(inputs, "expected")
    val listOfArgs = listOf("document" to document, "expected" to expected)
    val args = mapArgs(listOfArgs)
    return createStep(implementation, args, "StringAssertTask")
}

fun mapAmqpCleanQueuesTask(implementation: ComposableStepDto): String {
    val inputs = implementation.task?.inputs
    val queueNames = inputAsString(inputs, "queueNames")
    val target = target(implementation.task)
    val listOfArgs = listOf("target" to target, "queueNames" to queueNames)
    val args = mapArgs(listOfArgs)
    return createStep(implementation, args, "AmqpCleanQueuesTask")
}

fun mapJmsSenderTask(implementation: ComposableStepDto): String {
    val inputs = implementation.task?.inputs
    val target = target(implementation.task)
    val headers = inputAsString(inputs, "dateDemande")
    val queueName = inputAsString(inputs, "destination")
    val payload = inputAsString(inputs, "body")
    val listOfArgs = listOf(
        "target" to target,
        "headers" to headers,
        "queueName" to queueName,
        "payload" to payload
    )
    val args = mapArgs(listOfArgs)
    return createStep(implementation, args, "JmsSenderTask")
}

fun mapHttpGetTask(implementation: ComposableStepDto): String {
    val inputs = implementation.task?.inputs
    val headers = inputAsMap(inputs, "headers")
    val outputs = outputsAsMap(implementation.task)
    val target = target(implementation.task)
    val uri = uri(implementation.task)
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
    return createStep(implementation, args, "HttpGetTask")
}

fun mapHttpPostTask(implementation: ComposableStepDto): String {
    val inputs = implementation.task?.inputs
    val headers = inputAsMap(inputs, "headers")
    val body = if (inputs?.get("body") is Map<*, *>) inputAsMap(
        inputs,
        "body"
    ) else inputAsString(inputs, "body")
    val outputs = outputsAsMap(implementation.task)
    val target = target(implementation.task)
    val uri = uri(implementation.task)
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
    return createStep(implementation, args, "HttpPostTask")
}

fun mapHttpPutTask(implementation: ComposableStepDto): String {
    val inputs = implementation.task?.inputs
    val target = implementation.task?.let { target(it) }
    val headers = inputAsMap(inputs, "headers")
    val body = inputAsMap(inputs, "body")
    val uri = implementation.task?.let { uri(it) }
    val timeout = inputAsString(inputs, "timeout")
    val listOfArgs = listOf(
        "target" to target,
        "uri" to uri,
        "headers" to headers,
        "timeout" to timeout,
        "body" to body,
        "strategy" to null
    )
    val args = mapArgs(listOfArgs)
    return createStep(implementation, args, "HttpPutTask")
}

fun mapContexPutTask(implementation: ComposableStepDto): String {
    val input = implementation.task?.inputs
    val entries = inputAsMap(input, "entries")
    val listOfArgs = listOf(
        "entries" to entries
    )
    val args = mapArgs(listOfArgs)

    return createStep(implementation, args, "ContextPutTask")
}

private fun createStep(
    implementation: ComposableStepDto,
    args: String,
    stepName: String
) = """
        ${kotlinHeader(implementation)}
        ${kotlinFunctionName(implementation)}
            Step("${implementation.name}") {
                $stepName($args)
            }
        }
     """

private fun kotlinFunctionName(implementation: ComposableStepDto) = " public fun ChutneyStepBuilder.`${implementation.name}`() { "

private fun kotlinCallFunction(implementation: ComposableStepDto) = " `${implementation.name}`() "

private fun kotlinHeader(implementation: ComposableStepDto) =
    """ /**
        * id : ${implementation.id}
        * strategy: ${implementation.strategy.toString()}
        * computed parameters ${implementation.computedParameters?.joinToString(",") { it.key + " = " + it.value }}
        * parameters ${implementation.parameters?.joinToString(",") { it.key + " = " + it.value }}
        * tags: ${implementation.tags}
        **/"""


public fun outputsAsMap(implementation: StepImplementation?) =
    mapOfConstructor(implementation?.outputs)

public fun inputAsString(inputs: Map<String, Any?>?, key: String) =
    escapeKotlin((inputs?.get(key) as String? ?: "")).wrapWithQuotes()

public fun mapArgs(listOfArgs: List<Pair<String, Any?>>): String {
    return listOfArgs
        .filterNot { it.second == null || it.second == "".wrapWithTripleQuotes() || it.second == "mapOf()" || it.second == "listOf()" }
        .joinToString(", ") { it.first + " = " + it.second }
}

public fun inputAsMapList(inputs: Map<String, Any?>, key: String) =
    listOfMapConstructor(inputs.get(key) as List<Map<String, Any>>?)

public fun inputAsList(inputs: Map<String, Any?>, key: String) =
    listOfConstructor(inputs.get(key) as List<String>?)

public fun inputAsMap(inputs: Map<String, Any?>?, key: String) =
    mapOfConstructor(inputs?.get(key) as Map<String, Any>?)

public fun String.wrapWithQuotes(): String {
    return "\"$this\""
}
public fun String.wrapWithTripleQuotes(): String {
    return "\"\"\"$this\"\"\""
}
public fun listOfConstructor(
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

public fun listOfMapConstructor(
    list: List<Map<String, Any>>?
): String {
    if (list == null) {
        return "listOf()"
    }
    return "listOf(${
        list.joinToString(",\n") {
            mapOfConstructor(it)
        }
    })"
}

public fun mapOfConstructor(
    entries: Map<String, Any?>?
): String {
    if (entries == null) {
        return "mapOf()"
    }
    return "mapOf(${
        entries.map {
            "\"${it.key}\" to \"${
                escapeKotlin(
                    if (it.value is Map<*, *>) {
                        escapeKotlin(jacksonObjectMapper().writeValueAsString(it.value as Map<*, *>))
                    } else it.value.toString() //TODO check when is Int
                )
            }\""
        }.joinToString(",\n")
    })"
}

public fun escapeKotlin(s: String): String {
    return s
        .replace("\${", "\\\${")
        .replace("\"", "\\\"")
}

public fun uri(implementation: StepImplementation?): String {
    val inputs = implementation?.inputs
    if (inputs != null) {
        return escapeKotlin(
            (inputs.get("uri") as String? ?: "")
        ).wrapWithQuotes()
    } else {
        return ""
    }
}

public fun target(implementation: StepImplementation?): String = (implementation?.target as String).wrapWithQuotes()

fun main() {
    val serverInfo = ChutneyServerInfo("https://", "", "");
    generateDsl(serverInfo);
}
