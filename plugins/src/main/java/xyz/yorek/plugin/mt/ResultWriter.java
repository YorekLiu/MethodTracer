package xyz.yorek.plugin.mt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.gradle.internal.impldep.com.google.api.client.json.Json;
import org.objectweb.asm.Type;

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
    private static boolean JSON = true;

    public static void write(Configuration configuration) throws IOException {
        if (JSON) {
            write2Json(configuration);
            return;
        }

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

    public static void write2Json(Configuration configuration) throws IOException {
        // [{"class":"xxxx", "method":[{"name":"pid", "call":["", ""]}]}]
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(configuration.output))) {
            Map<String, List<String>> apiList = configuration.apiList;
            Map<String, Map<String, List<MethodCallerRecord>>> classMethodAndCallersMap = configuration.classMethodAndCallersMap;

            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();
            JsonArray jsonArray = new JsonArray();
            for (Map.Entry<String, List<String>> entry : apiList.entrySet()) {
                // 待检测的类名
                String internalClassName = entry.getKey();
                String className = Type.getObjectType(internalClassName).getClassName();

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("class", className);

                JsonArray apiMethods = new JsonArray();
                for (String method : entry.getValue()) {
                    JsonObject methodAndCaller = new JsonObject();

                    // 待检测的方法名
                    methodAndCaller.addProperty("methodName", method);

                    // 调用者
                    JsonArray callers = new JsonArray();
                    if (classMethodAndCallersMap.get(internalClassName) != null) {
                        if (classMethodAndCallersMap.get(internalClassName).get(method) != null) {
                            List<MethodCallerRecord> callerRecordList = classMethodAndCallersMap.get(internalClassName).get(method);
                            for (MethodCallerRecord callerRecord : callerRecordList) {
                                callers.add(callerRecord.toSimpleString());
                            }
                            methodAndCaller.add("callers", callers);
                        }
                    }
                    // 过滤掉没有调用的检测项
                    if (callers.size() != 0) {
                        apiMethods.add(methodAndCaller);
                    }
                }
                jsonObject.add("method", apiMethods);

                if (apiMethods.size() != 0) {
                    jsonArray.add(jsonObject);
                }
            }
            bufferedWriter.write(gson.toJson(jsonArray));
            bufferedWriter.flush();
        }
    }
}
