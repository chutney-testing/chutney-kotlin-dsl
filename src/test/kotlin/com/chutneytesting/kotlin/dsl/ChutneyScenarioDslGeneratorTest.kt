package com.chutneytesting.kotlin.dsl

import com.gregwoodfill.assert.`should equal json`
import org.junit.Test
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import kotlin.test.assertEquals

class ChutneyScenarioDslGeneratorTest {

    @Test
    fun `abe to create kotlin chutney scenario from json`() {

        val generateDsl = ChutneyScenarioDslGenerator().generateDsl("/get-people.chutney.json".asResource())

        assertEquals(
            generateDsl,
            """
            >val `swapi GET people record` = Scenario(title = "swapi GET people record") {
            >    Given("I set get people service api endpoint") {
            >        ContextPutTask(entries = mapOf("uri" to "api/people/1"))
            >    }
            >    When("I send GET HTTP request") {
            >        HttpGetTask(target = "swapi.dev", uri = "\${'$'}{#uri}", timeout = "2 sec")
            >    }
            >    Then("I receive valid HTTP response") {
            >        JsonAssertTask(document = "\${'$'}{#body}", expected = mapOf("${'$'}.name" to "Luke Skywalker"))
            >    }
            >}
            """.trimMargin('>'.toString())
        )

    }

    @Test
    fun `abe to compile kotlin dsl chutney scenario to json`() {
        val scriptContent = "/get-people.chutney.kts".asResource()
        val fromScript: ChutneyScenario = load(scriptContent)

        "$fromScript" `should equal json` "/get-people.chutney.json".asResource()
    }

    inline fun <reified T> load(script: String) : T {
        val engine: ScriptEngine = ScriptEngineManager().getEngineByExtension("kts")
        return engine.eval(script) as T
    }
}
