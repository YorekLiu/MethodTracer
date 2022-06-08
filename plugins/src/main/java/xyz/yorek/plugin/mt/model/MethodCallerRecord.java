package xyz.yorek.plugin.mt.model;

import org.objectweb.asm.Type;

import java.util.Locale;

/**
 * Created by yorek.liu on 2021/8/2
 *
 * @author yorek.liu
 */
public class MethodCallerRecord {
    public int opCode;
    public String desc;
    public String className;
    public String methodName;

    public MethodCallerRecord(int opCode, String desc, String className, String methodName) {
        this.opCode = opCode;
        this.desc = desc;
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "%s#%s (opcode=%d signature=%s)", className, methodName, opCode, desc);
    }

    public String toSimpleString() {
        return String.format(Locale.getDefault(), "%s#%s %s", Type.getObjectType(className).getClassName(), methodName, desc);
    }
}
