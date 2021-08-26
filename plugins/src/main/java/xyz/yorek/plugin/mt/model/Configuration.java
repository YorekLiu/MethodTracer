package xyz.yorek.plugin.mt.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yorek.liu on 2021/8/2
 *
 * @author yorek.liu
 */
public class Configuration {
    public String output;
    /**
     * key:owner, value:List<name>
     * name支持通配符*
     */
    public Map<String, List<String>> apiList;

    public Map<String, Map<String, List<MethodCallerRecord>>> classMethodAndCallersMap = new HashMap<>();
}
