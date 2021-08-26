package xyz.yorek.plugin.mt.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.yorek.plugin.mt.model.ClassGraph;
import xyz.yorek.plugin.mt.model.Configuration;
import xyz.yorek.plugin.mt.model.MethodCallerRecord;

/**
 * Created by yorek.liu on 2021/8/3
 *
 * @author yorek.liu
 */
public class CodeScanVisitor extends BaseClassVisitor {

    private String className;

    public CodeScanVisitor(int i, ClassVisitor classVisitor) {
        super(i, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
        return new TraceMethodAdapter(api, methodVisitor, access, name, desc, this.className, getContext().getConfiguration(), getContext().getClassGraph());
    }

    public static class TraceMethodAdapter extends AdviceAdapter {

        private final Configuration configuration;
        private final ClassGraph classGraph;
        private final String methodName;
        private final String className;

        protected TraceMethodAdapter(int api, MethodVisitor mv, int access, String name, String desc, String className, Configuration configuration, ClassGraph classGraph) {
            super(api, mv, access, name, desc);
            this.className = className;
            this.methodName = name;
            this.configuration = configuration;
            this.classGraph = classGraph;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            traceMethodCall(opcode, owner, name, desc, itf);
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }

        private void traceMethodCall(int opcode, String owner, String name, String desc, boolean itf) {
            if (configuration.apiList.containsKey(owner)) {
                List<String> checkApi = configuration.apiList.get(owner);
                if (checkApi.contains("*") || checkApi.contains(name)) {
                    Map<String, List<MethodCallerRecord>> method2CallersMap = configuration.classMethodAndCallersMap.get(owner);
                    if (method2CallersMap == null) {
                        method2CallersMap = new HashMap<>();
                        configuration.classMethodAndCallersMap.put(owner, method2CallersMap);
                    }
                    List<MethodCallerRecord> callers = method2CallersMap.get(name);
                    if (callers == null) {
                        callers = new ArrayList<>();
                        method2CallersMap.put(name, callers);
                    }
                    callers.add(new MethodCallerRecord(opcode, desc, className, methodName));
//                    Log.v("CodeScanner", "%d %s %s %s, called by %s.%s", opcode, owner, name, desc, className, methodName);
                }
            }
        }
    }
}
