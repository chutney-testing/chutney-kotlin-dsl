# Chutney Testing Kotlin DSL

[![Build Status](https://travis-ci.org/chutney-testing/chutney-kotlin-dsl.svg?branch=master)](https://travis-ci.org/chutney-testing/chutney-kotlin-dsl)
[![Download](https://api.bintray.com/packages/chutney-testing/maven/chutney-kotlin-dsl/images/download.svg) ](https://bintray.com/chutney-testing/maven/chutney-kotlin-dsl/_latestVersion)

## DO IT IN CODE {"NOT": "JSON"}

This repository aims to add a kotlin flavor for writing chutney scenarios.

## Why?

- Avoid text copy pasting
- Provide better code assistance using IDE 
- Allow customization for teams

A chutney json scenario:
```json
{
  "title": "SWAPI GET people record",
  "description": "SWAPI GET people record",
  "givens": [
    {
      "description": "I set get people service api endpoint",
      "implementation": {
        "type": "context-put",
        "inputs": {
          "entries": {
            "uri": "api/people/1"
          }
        }
      }
    }
  ],
  "when": {
    "description": "I send GET HTTP request",
    "implementation": {
      "type": "http-get",
      "target": "swapi.dev",
      "inputs": {
        "uri": "${#uri}"
      }
    }
  },
  "thens": [
    {
      "description": "I receive valid HTTP response",
      "implementation": {
        "type": "json-assert",
        "inputs": {
          "document": "${#body}",
          "expected": {
            "$.name": "Luke Skywalker"
          }
        }
      }
    }
  ]
}
```

Writing the same scenario with a kotlin DSL:
```kotlin
Scenario(title = "SWAPI GET people record") {
    Given("I set get people service api endpoint") {
        ContextPutTask(entries = mapOf("uri" to "api/people/1"))
    }
    When("I send GET HTTP request") {
        HttpGetTask(target = "swapi.dev", uri = "uri".spEL())
    }
    Then("I receive valid HTTP response") {
        JsonAssertTask(document = "body".spEL(), expected = mapOf("$.name" to "Luke Skywalker"))
    }
}
```

How (k)ool is Kotlin? super (k)ool! 
