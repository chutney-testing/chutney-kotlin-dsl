package com.chutneytesting.kotlin.dsl.transformation.from_component_to_kotlin

data class StepImplementation (
    val type: String?,
    val target: String?,
    val inputs: Map<String, Any?>?,
    val outputs: Map<String, Any?>?,
    val validations: Map<String, Any?>?
)
