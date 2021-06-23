package com.chutneytesting.kotlin.dsl.synchronize

import com.chutneytesting.kotlin.util.http.ChutneyServerInfo
import com.chutneytesting.kotlin.util.http.HttpClient
import com.chutneytesting.kotlin.dsl.ChutneyScenario
import org.apache.commons.lang3.StringUtils
import java.io.File

const val chutneyDatabaseUrl = "/api/v1/admin/database/execute/jdbc"

/**
 * Synchronise scenario locally and/or remotely and returns elapsed time in milliseconds.
 */
inline fun synchronizeScenarios(serverInfo: ChutneyServerInfo, remoteUpdate: Boolean = false, block: SynchronizeScenariosBuilder.() -> Unit): Long {
    val start = System.currentTimeMillis()
    val builder = SynchronizeScenariosBuilder()
    builder.block()
    if (!remoteUpdate) {
        builder.scenarios.forEach { it.synchronise(serverInfo, remoteUpdate) }
    }
    return System.currentTimeMillis() - start
}

/**
 * Cosmetic to create a list of scenarios
 */
class SynchronizeScenariosBuilder {
    var scenarios: List<ChutneyScenario> = mutableListOf()

    operator fun ChutneyScenario.unaryPlus() {
        scenarios = scenarios + this
    }

    operator fun List<ChutneyScenario>.unaryPlus() {
        scenarios = scenarios + this
    }

    operator fun ChutneyScenario.unaryMinus() {
        // scenarios = scenarios - this
        // cosmetic to ignore scenario
    }
}

fun ChutneyScenario.synchronise(serverInfo: ChutneyServerInfo, updateRemote: Boolean = false, path: String = "src/main/resources/chutney/") {
    val json = this.toString()
    val fileName = (this.id?.let { this.id.toString() + "-" } ?: "") + title + ".chutney.json"
    File(path).walkTopDown().filter { it.isFile }.firstOrNull {
        val chutneyScenarioIdFromFileName = getChutneyScenarioIdFromFileName(it.name)
        it.name.equals(fileName, ignoreCase = true) || (this.id != null && chutneyScenarioIdFromFileName != null && this.id == chutneyScenarioIdFromFileName)
    }?.apply {
        this.writeText(json)
    }?.also {
        println("| AT json synchronized:: ${it.absolutePath}")
    } ?: File("src/main/resources/chutney/in_progress/$fileName").apply { writeText(json) }
        .also { println("| AT json created:: ${it.absolutePath}") }

    if (updateRemote && this.id != null) {
        updateJsonRemoteScenario(serverInfo, this.id!!, json)
    }
}

fun List<ChutneyScenario>.synchronise(serverInfo: ChutneyServerInfo) {
    this.forEach { it.synchronise(serverInfo) }
}

fun updateJsonRemoteScenario(serverInfo: ChutneyServerInfo, id: Int, content: String) {
    val generatedTag = "GENERATED"
    val escapeSql = escapeSql(content)
    val body = "update scenario set content='$escapeSql', version=version+1, tags=CASE WHEN tags like '%$generatedTag%' THEN tags ELSE CONCAT_WS(',',tags,'$generatedTag') end where id = '$id'"
    try {
        HttpClient.post<Any>(serverInfo, chutneyDatabaseUrl, body)
        println("| remote AT json synchronized:: ${serverInfo.remoteServerUrl}/#/scenario/$id/execution/last")
    } catch (e: Exception) {
        println("| remote AT json cannot be synchronized:: $id")
    }
}

fun escapeSql(str: String?): String? = if (str == null) null else StringUtils.replace(str, "'", "''")

fun getChutneyScenarioIdFromFileName(fileName: String): Int? {
    val dashIndex = fileName.indexOf("-")
    return try {
        if (dashIndex > 0) Integer.valueOf(fileName.substring(0, dashIndex)) else null
    } catch (e: Exception) {
        null
    }
}
