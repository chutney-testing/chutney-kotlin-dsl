package com.chutneytesting.kotlin.dsl

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.core.util.Separators
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule


@DslMarker
annotation class ChutneyScenarioDsl

fun Scenario(id: Int? = null, title: String, block: ChutneyScenarioBuilder.() -> Unit): ChutneyScenario {
    return ChutneyScenarioBuilder(id, title).apply(block).build()
}

@ChutneyScenarioDsl
class ChutneyScenarioBuilder(val id: Int? = null, val title: String = "") {
    var description = title
    private val givens = mutableListOf<ChutneyStep>()
    private var `when`: ChutneyStep? = null
    private val thens = mutableListOf<ChutneyStep>()

    fun Given(description: String = "", block: ChutneyStepBuilder.() -> Unit) {
        givens.add(ChutneyStepBuilder(description).apply(block).build())
    }

    fun Given(description: String = "", strategy: Strategy? = null, block: ChutneyStepBuilder.() -> Unit) {
        givens.add(ChutneyStepBuilder(description, strategy).apply(block).build())
    }

    fun When(description: String = "", block: ChutneyStepBuilder.() -> Unit) {
        `when` = ChutneyStepBuilder(description).apply(block).build()
    }

    fun When(description: String = "", strategy: Strategy? = null, block: ChutneyStepBuilder.() -> Unit) {
        `when` = ChutneyStepBuilder(description, strategy).apply(block).build()
    }

    fun Then(description: String = "", block: ChutneyStepBuilder.() -> Unit) {
        thens.add(ChutneyStepBuilder(description).apply(block).build())
    }

    fun Then(description: String = "", strategy: Strategy?, block: ChutneyStepBuilder.() -> Unit) {
        thens.add(ChutneyStepBuilder(description, strategy).apply(block).build())
    }

    fun And(description: String = "", strategy: Strategy? = null, block: ChutneyStepBuilder.() -> Unit) {
        when {
            `when` != null -> thens.add(ChutneyStepBuilder(description, strategy).apply(block).build())
            else -> givens.add(ChutneyStepBuilder(description, strategy).apply(block).build())
        }
    }

    fun And(description: String = "", block: ChutneyStepBuilder.() -> Unit) {
        when {
            `when` != null -> thens.add(ChutneyStepBuilder(description).apply(block).build())
            else -> givens.add(ChutneyStepBuilder(description).apply(block).build())
        }
    }

    fun build(): ChutneyScenario = ChutneyScenario(id, title, description, givens, `when`, thens)

}

open class Strategy(val type: String, val parameters: Map<String, String> = emptyMap())
open class RetryTimeOutStrategy(timeout: String, retryDelay: String) :
    Strategy(type = "retry-with-timeout", parameters = mapOf("timeOut" to timeout, "retryDelay" to retryDelay))

open class SoftAssertStrategy() :
    Strategy(type = "soft-assert")

@ChutneyScenarioDsl
class ChutneyStepBuilder(var description: String = "", var strategy: Strategy? = null) {

    var subSteps = mutableListOf<ChutneyStep>()
    var implementation: ChutneyStepImpl? = null

    fun Strategy(s: Strategy) {
        strategy = s
    }

    fun Implementation(block: ChutneyStepImplBuilder.() -> Unit) {
        implementation = ChutneyStepImplBuilder().apply(block).build()
    }

    fun Step(description: String = "", strategy: Strategy? = null, block: ChutneyStepBuilder.() -> Unit) {
        subSteps.add(ChutneyStepBuilder(description, strategy).apply(block).build())
    }

    fun Step(description: String = "", block: ChutneyStepBuilder.() -> Unit) {
        subSteps.add(ChutneyStepBuilder(description, strategy).apply(block).build())
    }

    fun ContextPutTask(entries: Map<String, Any>, outputs: Map<String, Any> = mapOf()) {
        implementation =
            ChutneyStepImpl(type = "context-put", target = null, inputs = entries.toEntries(), outputs = outputs)
    }

    fun DebugTask() {
        implementation = ChutneyStepImpl(type = "debug", target = null, inputs = mapOf(), outputs = mapOf())
    }

    fun HttpGetTask(
        target: String,
        uri: String,
        headers: Map<String, Any> = mapOf(),
        timeout: String = "2 sec",
        outputs: Map<String, Any> = mapOf(),
        strategy: Strategy? = null
    ) {
        implementation = ChutneyStepImpl(
            type = "http-get",
            target = target,
            inputs = listOfNotNull("uri" to uri,  ("headers" to headers).takeIf { headers.isNotEmpty() }, "timeout" to timeout).toMap(),
            outputs = outputs
        )
        this.strategy = strategy

    }

    fun HttpDeleteTask(
        target: String,
        uri: String,
        headers: Map<String, Any> = mapOf(),
        timeout: String = "2 sec",
        outputs: Map<String, Any> = mapOf(),
        strategy: Strategy? = null
    ) {
        implementation = ChutneyStepImpl(
            type = "http-delete",
            target = target,
            inputs = listOfNotNull("uri" to uri, ("headers" to headers).takeIf { headers.isNotEmpty() }, "timeout" to timeout).toMap(),
            outputs = outputs
        )
        this.strategy = strategy

    }

    fun HttpPostTask(
        target: String,
        uri: String,
        headers: Map<String, Any> = mapOf(),
        body: Any,
        timeout: String = "2 sec",
        outputs: Map<String, Any> = mapOf("body".toSpelPair()),
        strategy: Strategy? = null
    ) {
        implementation = ChutneyStepImpl(
            type = "http-post",
            target = target,
            inputs = listOfNotNull("uri" to uri,  ("headers" to headers).takeIf { headers.isNotEmpty() }, "body" to body, "timeout" to timeout).toMap(),
            outputs = outputs
        )
        this.strategy = strategy
    }

    fun HttpPutTask(
        target: String,
        uri: String,
        headers: Map<String, Any> = mapOf(),
        body: Map<String, Any> = mapOf(),
        timeout: String = "2 sec",
        outputs: Map<String, Any> = mapOf("body".toSpelPair()),
        strategy: Strategy? = null
    ) {
        implementation = ChutneyStepImpl(
            type = "http-put",
            target = target,
            inputs = listOfNotNull("uri" to uri,  ("headers" to headers).takeIf { headers.isNotEmpty() }, "body" to body, "timeout" to timeout).toMap(),
            outputs = outputs
        )
        this.strategy = strategy
    }

    fun KafkaBasicPublishTask(
        target: String,
        topic: String,
        headers: Map<String, Any> = mapOf(),
        payload: Any
    ) {
        implementation = ChutneyStepImpl(
            type = "kafka-basic-publish",
            target = target,
            inputs = listOfNotNull("topic" to topic, ("headers" to headers).takeIf { headers.isNotEmpty() }, "payload" to payload).toMap(),
            outputs = mapOf()
        )
    }

    fun KafkaBasicConsumeTask(
        target: String,
        topic: String,
        group: String,
        properties: Map<String, String> = mapOf("auto.offset.reset" to "earliest"),
        timeout: String = "60 sec",
        selector: String = "",
        outputs: Map<String, Any> = mapOf()
    ) {
        implementation = ChutneyStepImpl(
            type = "kafka-basic-consume",
            target = target,
            inputs = mapOf(
                "topic" to topic,
                "group" to group,
                "timeout" to timeout,
                "selector" to selector,
                "properties" to properties
            ),
            outputs = outputs
        )
    }

    fun AmqpBasicConsumeTask(
        target: String,
        queueName: String,
        nbMessages: Int = 1,
        timeout: String = "60 sec",
        selector: String = "",
        outputs: Map<String, Any> = mapOf()
    ) {
        implementation = ChutneyStepImpl(
            type = "amqp-basic-consume",
            target = target,
            inputs = mapOf(
                "queue-name" to queueName,
                "nb-messages" to nbMessages,
                "timeout" to timeout,
                "selector" to selector
            ),
            outputs = outputs
        )
    }

    fun AmqpCleanQueuesTask(target: String, queueNames: String) {
        implementation = ChutneyStepImpl(
            type = "amqp-clean-queues",
            target = target,
            inputs = mapOf("queue-names" to queueNames),
            outputs = mapOf()
        )
    }

    fun JmsCleanQueuesTask(target: String, queueName: String) {
        implementation = ChutneyStepImpl(
            type = "jms-clean-queue",
            target = target,
            inputs = mapOf("destination" to queueName),
            outputs = mapOf()
        )
    }

    fun JmsSenderTask(target: String, queueName: String, headers: Map<String, Any> = mapOf(), payload: String) {
        implementation = ChutneyStepImpl(
            type = "jms-sender",
            target = target,
            inputs = listOfNotNull(
                "destination" to queueName,
                "body" to payload,
                ("headers" to headers).takeIf { headers.isNotEmpty() },
            ).toMap(),
            outputs = mapOf()
        )
    }

    fun JsonAssertTask(document: String, expected: Map<String, Any> = mapOf()) {
        implementation = ChutneyStepImpl(
            type = "json-assert",
            target = null,
            inputs = mapOf("document" to document, "expected" to expected),
            outputs = mapOf()
        )
    }

    fun JsonAssertTask(documentVariable: String, expectationsVariable: String) {
        implementation = ChutneyStepImpl(
            type = "json-assert",
            target = null,
            inputs = mapOf("document" to documentVariable.spEL, "expected" to expectationsVariable.spEL),
            outputs = mapOf()
        )
    }

    fun JsonCompareTask(document1: String, document2: String, comparingPaths: Map<String, String> = mapOf()) {
        implementation = ChutneyStepImpl(
            type = "json-compare",
            target = null,
            inputs = mapOf("document1" to document1, "document2" to document2, "comparingPaths" to comparingPaths),
            outputs = mapOf()
        )
    }

    fun StringAssertTask(document: String, expected: String) {
        implementation = ChutneyStepImpl(
            type = "string-assert",
            target = null,
            inputs = mapOf("document" to document, "expected" to expected),
            outputs = mapOf()
        )
    }

    fun AssertTrueTask(asserts: List<Map<String, Any>>) {
        implementation = ChutneyStepImpl(
            type = "assert",
            target = null,
            inputs = mapOf("asserts" to asserts),
            outputs = mapOf()
        )
    }

    fun XmlAssertTask(document: String, expected: Map<String, Any> = mapOf()) {
        implementation = ChutneyStepImpl(
            type = "xml-assert",
            target = null,
            inputs = mapOf("document" to document, "expected" to expected),
            outputs = mapOf()
        )
    }

    fun JsonValidationTask(schema: String, json: String) {
        implementation = ChutneyStepImpl(
            type = "json-validation",
            target = null,
            inputs = mapOf("schema" to schema, "json" to json),
            outputs = mapOf()
        )
    }

    fun SqlTask(target: String, statements: List<String>, outputs: Map<String, Any> = mapOf()) {
        implementation = ChutneyStepImpl(
            type = "sql",
            target = target,
            inputs = mapOf("statements" to statements),
            outputs = outputs
        )
    }

    fun SleepTask(duration: String) {
        implementation = ChutneyStepImpl(
            type = "sleep",
            target = null,
            inputs = mapOf("duration" to duration),
            outputs = mapOf()
        )
    }

    fun MicrometerGaugeTask(
        name: String,
        description: String? = null,
        unit: String? = null,
        strongReference: Boolean = false,
        tags: List<String> = emptyList(),
        gaugeObject: Any? = null, //Number or Object or List or Map
        gaugeFunction: String? = null,
        registry: String? = null,
        outputs: Map<String, Any> = mapOf()
    ) {
        val inputs = HashMap<String, Any?>()
        inputs.putAll(mapOf( "name" to null))
        implementation = ChutneyStepImpl(
            type = "micrometer-gauge",
            target = null,
            inputs = mapOf(
                "name" to name,
                "description" to description,
                "unit" to unit,
                "strongReference" to strongReference,
                "tags" to tags,
                "gaugeObject" to gaugeObject,
                "gaugeFunction" to gaugeFunction,
                "registry" to registry
            ),
            outputs = outputs
        )
    }

    fun MicrometerCounterTask(
        name: String,
        description: String? = null,
        unit: String? = null,
        tags: List<String> = emptyList(),
        counter: String ? = null,
        increment: String? = null,
        registry: String? = null,
        outputs: Map<String, Any> = mapOf()
    ) {
        implementation = ChutneyStepImpl(
            type = "micrometer-counter",
            target = null,
            inputs = mapOf(
                "name" to name,
                "description" to description,
                "unit" to unit,
                "tags" to tags,
                "counter" to counter,
                "increment" to increment,
                "registry" to registry
            ),
            outputs = outputs
        )
    }

    fun MicrometerSummaryTask(
        name: String,
        description: String? = null,
        unit: String? = null,
        tags: List<String> = emptyList(),
        bufferLength: String? = null,
        expiry: String? = null,
        maxValue: String? = null,
        minValue: String? = null,
        percentilePrecision: String? = null,
        publishPercentilesHistogram: Boolean = false,
        percentiles: String? = null,
        scale: String? = null,
        sla: String? = null,
        distributionSummary: String? = null,
        registry: String? = null,
        record: String? = null,
        outputs: Map<String, Any> = mapOf()
    ) {
        implementation = ChutneyStepImpl(
            type = "micrometer-summary",
            target = null,
            inputs = mapOf(
                "name" to name,
                "description" to description,
                "unit" to unit,
                "tags" to tags,
                "bufferLength" to bufferLength,
                "expiry" to expiry,
                "maxValue" to maxValue,
                "minValue" to minValue,
                "percentilePrecision" to percentilePrecision,
                "publishPercentilesHistogram" to publishPercentilesHistogram,
                "percentiles" to percentiles,
                "scale" to scale,
                "sla" to sla,
                "distributionSummary" to distributionSummary,
                "registry" to registry,
                "record" to record
            ),
            outputs = outputs
        )
    }

    fun MicrometerTimerTask(
        name: String,
        description: String? = null,
        tags: List<String> = emptyList(),
        bufferLength: String? = null,
        expiry: String? = null,
        maxValue: String? = null,
        minValue: String? = null,
        percentilePrecision: String? = null,
        publishPercentilesHistogram: Boolean = false,
        percentiles: String? = null,
        sla: String? = null,
        timer: String? = null,
        registry: String? = null,
        timeunit: String? = null,
        record: String? = null,
        outputs: Map<String, Any> = mapOf()
    ) {
        implementation = ChutneyStepImpl(
            type = "micrometer-timer",
            target = null,
            inputs = mapOf(
                "name" to name,
                "description" to description,
                "tags" to tags,
                "bufferLength" to bufferLength,
                "expiry" to expiry,
                "maxValue" to maxValue,
                "minValue" to minValue,
                "percentilePrecision" to percentilePrecision,
                "publishPercentilesHistogram" to publishPercentilesHistogram,
                "percentiles" to percentiles,
                "sla" to sla,
                "timer" to timer,
                "registry" to registry,
                "timeunit" to timeunit,
                "record" to record
            ),
            outputs = outputs
        )
    }

    fun MicrometerTimerStartTask(
        registry: String? = null,
        outputs: Map<String, Any> = mapOf()
    ) {
        implementation = ChutneyStepImpl(
            type = "micrometer-timer-start",
            target = null,
            inputs = mapOf(
                "registry" to registry,
            ),
            outputs = outputs
        )
    }

    fun MicrometerTimerStopTask(
        registry: String? = null,
        outputs: Map<String, Any> = mapOf()
    ) {
        implementation = ChutneyStepImpl(
            type = "micrometer-timer-stop",
            target = null,
            inputs = mapOf(
                "registry" to registry,
            ),
            outputs = outputs
        )
    }

    fun build(): ChutneyStep = ChutneyStep(description, implementation, strategy, subSteps)
}

@ChutneyScenarioDsl
class ChutneyStepImplBuilder {

    var type: String = ""
    var target: String = ""
    var inputs: Map<String, Any> = mapOf()
    var outputs: Map<String, Any> = mapOf()

    fun build(): ChutneyStepImpl = ChutneyStepImpl(type, target, inputs, outputs)

}

object Mapper {

    val pp = object : DefaultPrettyPrinter() {
        init {
            val indenter: Indenter = DefaultIndenter()
            indentObjectsWith(indenter) // Indent JSON objects
            indentArraysWith(indenter) // Indent JSON arrays
        }

        override fun withSeparators(separators: Separators?): DefaultPrettyPrinter {
            _separators = separators
            _objectFieldValueSeparatorWithSpaces = "" + separators!!.objectFieldValueSeparator + " "
            return this
        }
    }

    val mapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(KotlinModule())
        .setDefaultPrettyPrinter(pp)
        .enable(SerializationFeature.INDENT_OUTPUT)
}

class ChutneyStep(
    val description: String,
    @JsonInclude(NON_NULL) val implementation: ChutneyStepImpl? = null,
    @JsonInclude(NON_NULL) val strategy: Strategy? = null,
    @JsonInclude(NON_EMPTY) val subSteps: List<ChutneyStep> = emptyList()
)

class ChutneyStepImpl(
    val type: String,
    @JsonInclude(NON_EMPTY) val target: String?,
    @JsonInclude(NON_EMPTY) val inputs: Map<String, Any?>,
    @JsonInclude(NON_EMPTY) val outputs: Map<String, Any>?
)

class ChutneyScenario(
    @JsonIgnore val id: Int?,
    val title: String = "",
    val description: String = "",
    val givens: List<ChutneyStep> = mutableListOf(),
    val `when`: ChutneyStep? = null,
    val thens: List<ChutneyStep> = mutableListOf()
) {

    override fun toString(): String {
        return Mapper.mapper.writeValueAsString(this) + System.getProperty("line.separator")
    }
}
