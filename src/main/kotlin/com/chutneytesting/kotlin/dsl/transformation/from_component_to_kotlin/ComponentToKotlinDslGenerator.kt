package com.chutneytesting.kotlin.dsl.transformation.from_component_to_kotlin

import com.chutneytesting.kotlin.util.http.ChutneyServerInfo
import com.chutneytesting.kotlin.util.http.HttpClient

fun generateDsl(serverInfo: ChutneyServerInfo) {
    val get: List<ComposableStepDto> = HttpClient.get(serverInfo, "/api/steps/v1/all")

    val pouet = ""
}

fun main() {
    val serverInfo = ChutneyServerInfo("https://url server", "user", "user");
    generateDsl(serverInfo);
}
