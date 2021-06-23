package com.chutneytesting.kotlin.dsl.transformation.from_component_to_kotlin

data class StepImplementation (
    val type: String? = null
    val target: String? = null
    val inputs: Map<String, Any>? = null
    val outputs: Map<String, Any>? = null
    val validations: Map<String, Any>? = null
)
