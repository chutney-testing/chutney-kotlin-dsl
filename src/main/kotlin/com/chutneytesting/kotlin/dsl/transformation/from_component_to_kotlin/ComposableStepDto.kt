package com.chutneytesting.kotlin.dsl.transformation.from_component_to_kotlin

public final data class ComposableStepDto(
    val id: String?,
    val name: String?,
    val strategy: Strategy?,
    val usage: String?,
    val task: StepImplementation?,
    val steps: List<ComposableStepDto>?,
    val parameters: List<KeyValue>?,
    val computedParameters: List<KeyValue>?,
    val tags: List<String>?
)

data class KeyValue(
    val key: String?,
    val value: String?
)

data class Strategy(
    val type: String?,
    val parameters: Map<String, String>?
)
