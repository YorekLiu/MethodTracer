package xyz.yorek.plugin.mt.model;

/**
 * Created by yorek.liu on 2021/8/3
 *
 * @author yorek.liu
 */
public class ClassRecord {
//    public int version;
//    public int access;
    public String name;
//    public String signature;
    public String superName;
//    public String[] interfaces;

    public ClassRecord(String name) {
        this.name = name;
    }

    public ClassRecord(int version, int access, String name, String signature, String superName, String[] interfaces) {
//        this.version = version;
//        this.access = access;
        this.name = name;
//        this.signature = signature;
        this.superName = superName;
//        this.interfaces = interfaces;
    }

    @Override
    public String toString() {
        return "ClassRecord{" +
                "name='" + name + '\'' +
                ", superName='" + superName + '\'' +
                '}';
    }
}
