package com.chutneytesting.kotlin.dsl.transformation.from_component_to_kotlin

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.IOException
import java.util.*
import java.util.function.Consumer

class RawImplementationMapper(vc: Class<*>?) : StdDeserializer<StepImplementation>(vc) {

    private val objectMapper = jacksonObjectMapper()

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): StepImplementation? {
        val implementation = jp.codec.readTree<TextNode>(jp)
        val readTree = objectMapper.readTree(implementation.textValue());
        return StepImplementation(
            type(readTree),
            target(readTree),
            inputs(readTree),
            outputs(readTree),
            validations(readTree)
        )
    }

    private fun type(implementation: JsonNode): String? {
        return if (implementation.hasNonNull("identifier")) {
            implementation["identifier"].textValue()
        } else null
    }

    private fun target(implementation: JsonNode): String? {
        return Optional.ofNullable(implementation["target"]).orElse(TextNode.valueOf("")).textValue()
    }

    private fun outputs(implementation: JsonNode): Map<String, Any>? {
        val outputs: MutableMap<String, Any> = LinkedHashMap()
        if (implementation.hasNonNull("outputs")) {
            val outputsNode = implementation["outputs"]
            outputsNode.forEach(Consumer { `in`: JsonNode ->
                val name = `in`["key"].asText()
                outputs[name] = `in`["value"].asText()
            })
        }
        return outputs
    }

    private fun inputs(implementation: JsonNode): Map<String, Any?>? {
        val inputs: MutableMap<String, Any?> = LinkedHashMap()
        // Simple inputs
        if (implementation.hasNonNull("inputs")) {
            val simpleInputs = implementation["inputs"]
            simpleInputs.forEach(Consumer { `in`: JsonNode ->
                val inputName = `in`["name"].asText()
                inputs[inputName] = transformSimpleInputValue(`in`)
            })
        }
        // List inputs
        if (implementation.hasNonNull("listInputs")) {
            val listInputs = implementation["listInputs"]
            listInputs.forEach(Consumer { `in`: JsonNode ->
                val values: MutableList<Any> = ArrayList()
                `in`["values"]
                    .forEach(Consumer { v: JsonNode ->
                        values.add(
                            transformListInputValue(v)
                        )
                    })
                inputs[`in`["name"].asText()] = values
            })
        }
        // Map inputs
        if (implementation.hasNonNull("mapInputs")) {
            val mapInputs = implementation["mapInputs"]
            mapInputs.forEach(Consumer { `in`: JsonNode ->
                val values =
                    LinkedHashMap<String, String>()
                for (next in `in`["values"]) {
                    values[next["key"].asText()] = next["value"].asText()
                }
                inputs[`in`["name"].asText()] = values
            })
        }
        return inputs
    }

    private fun validations(implementation: JsonNode): Map<String, Any>? {
        val validations: MutableMap<String, Any> = LinkedHashMap()
        if (implementation.hasNonNull("validations")) {
            val validationsNode = implementation["validations"]
            validationsNode.forEach(Consumer { `in`: JsonNode ->
                val name = `in`["key"].asText()
                validations[name] = `in`["value"].asText()
            })
        }
        return validations
    }

    private fun transformSimpleInputValue(`in`: JsonNode): Any? {
        val value = `in`["value"].asText()
        return if (!value.isEmpty()) value else null
    }

    private fun transformListInputValue(`in`: JsonNode): Any {
        return if (`in`.isObject) {
            try {
                objectMapper.readValue(`in`.toString(), HashMap::class.java)
            } catch (e: Exception) {
                `in`.toString()
            }
        } else `in`.asText()
    }

}
