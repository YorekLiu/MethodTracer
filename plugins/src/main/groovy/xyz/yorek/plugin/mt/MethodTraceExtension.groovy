package xyz.yorek.plugin.mt

/**
 * Created by yorek.liu on 2021/8/12
 *
 * @author yorek.liu
 */
class MethodTraceExtension {
    public boolean enable
    public String output
    /**
     * key:owner, value:List<name>
     * name支持通配符*
     */
    public Map<String, List<String>> apiList

    MethodTraceExtension() {
        enable = true
        output = ""
        apiList = new HashMap<>()
    }

    @Override
    String toString() {
        """| enable = ${enable}
           | output = ${output}
           | apiList = ${apiList}
        """.stripMargin()
    }
}