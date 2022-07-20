package xyz.yorek.plugin

open class APMSampleExtension(
    var enable: Boolean = true
) {
    override fun toString(): String {
        return """
            enable=$enable
        """.trimIndent()
    }
}

////////////////////////////////////////////
enum class MinifySoMode(
    val label: String
) {
    NONE("none"),
    ASSET("asset"),
    ZIP("zip"),
}

open class MinifySoExtension(
    var mode: String = "none",
) {
    override fun toString(): String {
        return """
            mode=$mode
        """.trimIndent()
    }
}
////////////////////////////////////////////