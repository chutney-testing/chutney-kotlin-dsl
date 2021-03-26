package com.chutneytesting.kotlin.dsl

const val JSON_PATH_ROOT = "\$"
val String.spELVar: String
    get() = "#$this"
val String.spEL: String
    get() = "\${#$this}"

@Deprecated("Duplicate", ReplaceWith("spEL()"), DeprecationLevel.WARNING)
fun String.spELString(): String = "\${#$this}"
fun String.spEL(): String = "\${#$this}"
fun String.spELVar(): String = "#$this"
@Deprecated("ContextPut task specific", ReplaceWith(""), DeprecationLevel.WARNING)
fun Map<String, Any>.toEntries(): Map<String, Map<String, Any>> = mapOf("entries" to this)
fun String.toSpelPair(): Pair<String, String> = this to this.spEL
@Deprecated("Too specific", ReplaceWith(""), DeprecationLevel.WARNING)
fun retryTimeOutStrategy(timeout: String = "30 sec", retryDelay: String = "5 sec") =
    RetryTimeOutStrategy(timeout, retryDelay)

fun String.elEval(): String = "\${$this}"
fun String.elString(): String = "'${this.replace("'", "''")}'"
fun Map<String, String>.elMap(): String {
    return this.map {
        e -> "${e.key.elString()}: ${e.value.elString()}"
    }.joinToString(prefix = "{", postfix = "}")
}
