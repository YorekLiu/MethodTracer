package xyz.yorek.plugin.mt.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import xyz.yorek.plugin.mt.model.ClassGraph;
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
        return new MethodProxyAdapter(api, methodVisitor, access, name, desc, className, getContext().getClassGraph(), getContext().getMethodProxyContext());
    }

    public static class MethodProxyAdapter extends AdviceAdapter {

        private final String className;
        private final String methodName;
        private final ClassGraph classGraph;
        private final MethodProxyContext mMethodProxyContext;

        protected MethodProxyAdapter(int api, MethodVisitor mv, int access, String name, String desc, String className, ClassGraph classGraph, MethodProxyContext methodProxyContext) {
            super(api, mv, access, name, desc);
            this.className = className;
            this.methodName = name;
            this.classGraph = classGraph;
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
                // skip super call
                if (name.equals(this.methodName)
                        && desc.equals(methodDesc)
                        && classGraph.isAssignableFrom(className, owner)) {
                    return false;
                }

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
//                        if (true /* replace */) {
//                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, proxyRecord.implClass, proxyRecord.implMethod, proxyRecord.implMethodDesc, false);
//                            return true;
//                        } else { /* append */
//                            // this p1 p2 p3 -> ret
//                            int[] localIndex = new int[packedType.length];
//                            for (int i = 0; i < packedType.length; i++) {
//                                Type type = packedType[i];
//                                localIndex[i] = newLocal(type);
//                                int storeOpcode;
//                                switch (type.getSort()) {
//                                    case Type.BOOLEAN:
//                                    case Type.CHAR:
//                                    case Type.BYTE:
//                                    case Type.SHORT:
//                                    case Type.INT:
//                                        storeOpcode = Opcodes.ISTORE;
//                                        break;
//                                    case Type.FLOAT:
//                                        storeOpcode = Opcodes.FSTORE;
//                                        break;
//                                    case Type.LONG:
//                                        storeOpcode = Opcodes.LSTORE;
//                                        break;
//                                    case Type.DOUBLE:
//                                        storeOpcode = Opcodes.DSTORE;
//                                        break;
//                                    default:
//                                        storeOpcode = Opcodes.ASTORE;
//                                }
//                                mv.visitVarInsn(storeOpcode, localIndex[i]);
//                            }
//
//                            super.visitMethodInsn(opcode, owner, name, desc, itf);
//
//                            // ret
//
//                            return true;
//                        }
                    }
                }
            }
            return false;
        }
    }
}
