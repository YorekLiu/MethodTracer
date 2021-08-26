package xyz.yorek.plugin.mt.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yorek.liu on 2021/8/2
 *
 * @author yorek.liu
 */
public class MethodProxyContext {
    private final List<MethodProxyRecord> proxyRecordList = new ArrayList<>();
    private final List<String> entryClassList = new ArrayList<>();

    public void addEntryClass(String entryClass) {
        entryClassList.add(entryClass);
    }

    public boolean isEntryClass(String clazz) {
        return entryClassList.contains(clazz);
    }

    public void addMethodProxyRecord(MethodProxyRecord methodProxyRecord) {
        proxyRecordList.add(methodProxyRecord);
    }

    public boolean skipTrace() {
        return proxyRecordList.isEmpty() || entryClassList.isEmpty();
    }

    public MethodProxyRecord findRecord(String owner, String name) {
        for (MethodProxyRecord proxyRecord: proxyRecordList) {
            if (proxyRecord.proxyClass.equals(owner) && proxyRecord.proxyMethod.equals(name)) {
                return proxyRecord;
            }
        }
        return null;
    }

    public List<MethodProxyRecord> getProxyRecordList() {
        return proxyRecordList;
    }

    public void setProxyRecordList(List<MethodProxyRecord> list) {
        proxyRecordList.clear();
        proxyRecordList.addAll(list);
    }
}
