package xyz.yorek.plugin.mt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import xyz.yorek.plugin.mt.model.Configuration;
import xyz.yorek.plugin.mt.model.MethodCallerRecord;

/**
 * Created by yorek.liu on 2021/8/2
 *
 * @author yorek.liu
 */
public class ResultWriter {
    public static void write(Configuration configuration) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(configuration.output))) {
            Map<String, List<String>> apiList = configuration.apiList;
            Map<String, Map<String, List<MethodCallerRecord>>> classMethodAndCallersMap = configuration.classMethodAndCallersMap;
            for (Map.Entry<String, List<String>> entry : apiList.entrySet()) {
                // 待检测的类名
                String className = entry.getKey();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(className).append("\n");

                for (String method : entry.getValue()) {
                    // 待检测的方法名
                    stringBuilder.append("  ").append(method).append("\n");

                    // 调用者
                    if (classMethodAndCallersMap.get(className) != null) {
                        if (classMethodAndCallersMap.get(className).get(method) != null) {
                            List<MethodCallerRecord> callerRecordList = classMethodAndCallersMap.get(className).get(method);
                            for (MethodCallerRecord callerRecord : callerRecordList) {
                                stringBuilder.append("    -> ").append(callerRecord).append("\n");
                            }
                        }
                    }
                }
                bufferedWriter.write(stringBuilder.toString());
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
        }
    }
}
