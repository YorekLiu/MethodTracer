package xyz.yorek.plugin.mt.visitor;

import org.gradle.api.GradleException;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import xyz.yorek.plugin.inst.anno.AsmConstants;
import xyz.yorek.plugin.mt.Log;
import xyz.yorek.plugin.mt.model.FirstTraceContext;
import xyz.yorek.plugin.mt.model.MethodProxyContext;
import xyz.yorek.plugin.mt.model.MethodProxyRecord;

/**
 * Created by yorek.liu on 2021/8/3
 *
 * @author yorek.liu
 */
public class MethodTraceAnnotationVisitor extends ClassVisitor {

    private static final String TAG = "MethodTraceAnnotationVisitor";

    private final MethodProxyContext mMethodProxyContext;

    private boolean mVisitedMethodTraceClass = false;
    private String mClassName;

    public MethodTraceAnnotationVisitor(int api, ClassVisitor cv, FirstTraceContext context) {
        super(api, cv);
        mMethodProxyContext = context.methodProxyContent;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        mClassName = name;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (AsmConstants.METHOD_PROXY_CLASS_ANNOTATION.equals(desc)) {
            mVisitedMethodTraceClass = true;
            Log.v(TAG, "found annotation MethodProxyEntry with class: %s", mClassName);
            mMethodProxyContext.addEntryClass(mClassName);
        } else {
            mVisitedMethodTraceClass = false;
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        if (mVisitedMethodTraceClass) {
            return new TraceMethodAdapter(api, methodVisitor, access, name, desc, mClassName, mMethodProxyContext);
        }
        return methodVisitor;
    }

    private static class TraceMethodAdapter extends AdviceAdapter {

        private final MethodProxyContext mMethodProxyContext;
        private final String mMethodName;
        private final String mClassName;

        protected TraceMethodAdapter(int api, MethodVisitor mv, int access, String name, String desc, String className, MethodProxyContext methodProxyContext) {
            super(api, mv, access, name, desc);
            this.mMethodProxyContext = methodProxyContext;
            this.mClassName = className;
            this.mMethodName = name;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(desc, visible);
            if (AsmConstants.METHOD_PROXY_METHOD_ANNOTATION.equals(desc)) {
                MethodProxyRecord methodProxyRecord = new MethodProxyRecord(mClassName, mMethodName, methodDesc);
                mMethodProxyContext.addMethodProxyRecord(methodProxyRecord);
                return new MethodAnnotationAdapter(api, av, methodProxyRecord);
            }
            return av;
        }
    }

    private static class MethodAnnotationAdapter extends AnnotationVisitor {
        private final MethodProxyRecord mMethodProxyRecord;

        public MethodAnnotationAdapter(int api, AnnotationVisitor av, MethodProxyRecord methodProxyRecord) {
            super(api, av);
            this.mMethodProxyRecord = methodProxyRecord;
        }

        @Override
        public void visit(String name, Object value) {
            super.visit(name, value);
            switch (name) {
                case AsmConstants.METHOD_PROXY_METHOD_ANNOTATION_P_CLASS_NAME: {
                    mMethodProxyRecord.proxyClass = value.toString().replace('.', '/');
                    break;
                }
                case AsmConstants.METHOD_PROXY_METHOD_ANNOTATION_P_METHOD: {
                    mMethodProxyRecord.proxyMethod = value.toString();
                    break;
                }
                case AsmConstants.METHOD_PROXY_METHOD_ANNOTATION_P_EXTEND: {
                    mMethodProxyRecord.proxyExtend = Boolean.parseBoolean(value.toString());
                    break;
                }
            }
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            AnnotationVisitor ret = super.visitArray(name);
            if (AsmConstants.METHOD_PROXY_METHOD_ANNOTATION_P_CLAZZ.equals(name)) {
                   return new MethodClassAnnotationVisitor(api, ret, mMethodProxyRecord);
            }
            return ret;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            Log.v(TAG, "found annotation MethodProxy with method: %s", mMethodProxyRecord.toString());
            if (!mMethodProxyRecord.isValid()) {
                throw new GradleException("@MethodProxy is invalid, " + mMethodProxyRecord);
            }
        }
    }

    private static class MethodClassAnnotationVisitor extends AnnotationVisitor {

        private final MethodProxyRecord mMethodProxyRecord;

        public MethodClassAnnotationVisitor(int api, AnnotationVisitor annotationVisitor, MethodProxyRecord methodProxyRecord) {
            super(api, annotationVisitor);
            this.mMethodProxyRecord = methodProxyRecord;
        }

        @Override
        public void visit(String name, Object value) {
            super.visit(name, value);
            if (value instanceof Type) {
                mMethodProxyRecord.proxyClass = ((Type) value).getInternalName();
            }
        }
    }
}
