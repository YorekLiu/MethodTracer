package xyz.yorek.plugin.mt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.yorek.plugin.mt.model.Configuration;
import xyz.yorek.plugin.mt.model.MethodProxyContext;
import xyz.yorek.plugin.mt.model.MethodProxyRecord;
import xyz.yorek.plugin.mt.util.Util;

/**
 * Created by yorek.liu on 2021/8/13
 *
 * @author yorek.liu
 */
public class PreMethodTracer {
    private final TransformContext mContext;

    public PreMethodTracer(TransformContext context) {
        mContext = context;
    }

    public void execute() {
        handleCodeScannerRegexClass();
        handleMethodProxyRegexClass();
    }

    private void handleCodeScannerRegexClass() {
        List<String> removeKey = new ArrayList<>();
        Map<String, List<String>> addApi = new HashMap<>();

        Configuration configuration = mContext.configuration;

        for (Map.Entry<String, List<String>> api: configuration.apiList.entrySet()) {
            String className = api.getKey();
            if (Util.isNullOrNil(className) || !className.endsWith("+")) continue;
            String baseClassName = className.substring(0, className.length() - 1);

            List<String> apiList = api.getValue();
            List<String> extendedClasses = mContext.getClassGraph().getExtendedClass(baseClassName);
            for (String extendedClass : extendedClasses) {
                addApi.put(extendedClass, apiList);
            }
            removeKey.add(className);
        }

        for (String remove : removeKey) {
            configuration.apiList.remove(remove);
        }
        for (Map.Entry<String, List<String>> api: addApi.entrySet()) {
            configuration.apiList.put(api.getKey(), api.getValue());
        }

//        Log.v("MethodTracer", configuration.apiList.toString());
    }

    private void handleMethodProxyRegexClass() {
        MethodProxyContext methodProxyContext = mContext.getMethodProxyContext();
        List<MethodProxyRecord> proxyRecordList = new ArrayList<>(methodProxyContext.getProxyRecordList());

        List<MethodProxyRecord> extendedClassList = new ArrayList<>();
        for (MethodProxyRecord record : proxyRecordList) {
            if (record.proxyExtend) {
                List<String> extendedClasses = mContext.getClassGraph().getExtendedClass(record.proxyClass);
                for (String extendedClass : extendedClasses) {
                    MethodProxyRecord extendedRecord = new MethodProxyRecord(record.implClass, record.implMethod, record.implMethodDesc);
                    extendedRecord.proxyClass = extendedClass;
                    extendedRecord.proxyMethod = record.proxyMethod;
                    extendedRecord.proxyExtend = false;
                    extendedClassList.add(extendedRecord);
                }
            }
        }
        proxyRecordList.addAll(extendedClassList);
        methodProxyContext.setProxyRecordList(proxyRecordList);
    }
}
