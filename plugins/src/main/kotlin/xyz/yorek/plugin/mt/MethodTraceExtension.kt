package xyz.yorek.plugin.mt

open class MethodTraceExtension(
    var enable: Boolean = true,
    var output: String = "",
    /**
     * key:owner, value:List<name>
     * name支持通配符*
     */
    var apiList: Map<String, List<String>> = emptyMap()
) {
    override fun toString(): String {
        return """| enable = $enable
           | output = $output
           | apiList = $apiList
        """.trimMargin()
    }
}