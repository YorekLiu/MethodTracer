package xyz.yorek.plugin.mt.model;

import xyz.yorek.plugin.mt.util.Util;

/**
 * Created by yorek.liu on 2021/8/2
 *
 * @author yorek.liu
 */
public class MethodProxyRecord {
    public String implClass;
    public String implMethod;
    public String implMethodDesc;

    public String proxyClass;
    public String proxyMethod;
    public boolean proxyExtend;

    public MethodProxyRecord(String implClass, String implMethod, String implMethodDesc) {
        this.implClass = implClass;
        this.implMethod = implMethod;
        this.implMethodDesc = implMethodDesc;
    }

    public boolean isValid() {
        return !Util.isNullOrNil(implClass)
                && !Util.isNullOrNil(implMethod)
                && !Util.isNullOrNil(implMethodDesc)
                && !Util.isNullOrNil(proxyClass)
                && !Util.isNullOrNil(proxyMethod);
    }

    @Override
    public String toString() {
        return "MethodProxyRecord{" +
                "implClass='" + implClass + '\'' +
                ", implMethod='" + implMethod + '\'' +
                ", implMethodDesc='" + implMethodDesc + '\'' +
                ", proxyClass='" + proxyClass + '\'' +
                ", proxyMethod='" + proxyMethod + '\'' +
                ", proxyExtend='" + proxyExtend + '\'' +
                '}';
    }
}
