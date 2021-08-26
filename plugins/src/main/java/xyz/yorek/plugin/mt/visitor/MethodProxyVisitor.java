package xyz.yorek.plugin.mt.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import xyz.yorek.plugin.mt.model.MethodProxyContext;
import xyz.yorek.plugin.mt.model.MethodProxyRecord;

/**
 * Created by yorek.liu on 2021/8/3
 *
 * @author yorek.liu
 */
public class MethodProxyVisitor extends BaseClassVisitor {
    private static final String TAG = "MethodProxyVisitor";

    private String className;
    private boolean ignore;

    public MethodProxyVisitor(int i, ClassVisitor classVisitor) {
        super(i, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        ignore = getContext().getMethodProxyContext().isEntryClass(className);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        if (ignore || getContext().getMethodProxyContext().skipTrace()) {
            return methodVisitor;
        }
        return new MethodProxyAdapter(api, methodVisitor, access, name, desc, getContext().getMethodProxyContext());
    }

    public static class MethodProxyAdapter extends AdviceAdapter {

        private final MethodProxyContext mMethodProxyContext;

        protected MethodProxyAdapter(int api, MethodVisitor mv, int access, String name, String desc, MethodProxyContext methodProxyContext) {
            super(api, mv, access, name, desc);
            this.mMethodProxyContext = methodProxyContext;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (!handleMethod(opcode, owner, name, desc, itf)) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }

        private boolean handleMethod(int opcode, String owner, String name, String desc, boolean itf) {
            MethodProxyRecord proxyRecord = mMethodProxyContext.findRecord(owner, name);
            if (proxyRecord != null) {
                if (Opcodes.INVOKESTATIC == opcode) {
                    if (desc.equals(proxyRecord.implMethodDesc)) {
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, proxyRecord.implClass, proxyRecord.implMethod, desc, false);
                        return true;
                    }
                } else {
                    Type ownerType = Type.getObjectType(owner);
                    Type returnType = Type.getReturnType(desc);
                    Type[] descType = Type.getArgumentTypes(desc);
                    Type[] packedType = new Type[descType.length + 1];
                    packedType[0] = ownerType;
                    System.arraycopy(descType, 0, packedType, 1, descType.length);
                    String packedDesc = Type.getMethodType(returnType, packedType).toString();
                    if (packedDesc.equals(proxyRecord.implMethodDesc)) {
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, proxyRecord.implClass, proxyRecord.implMethod, proxyRecord.implMethodDesc, false);
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
