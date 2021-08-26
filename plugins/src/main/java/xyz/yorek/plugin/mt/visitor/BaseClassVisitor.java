package xyz.yorek.plugin.mt.visitor;

import org.objectweb.asm.ClassVisitor;

import xyz.yorek.plugin.mt.TransformContext;

/**
 * Created by yorek.liu on 2021/8/4
 *
 * @author yorek.liu
 */
public abstract class BaseClassVisitor extends ClassVisitor {

    protected TransformContext mContext;

    public BaseClassVisitor(int api) {
        super(api);
    }

    public BaseClassVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    public TransformContext getContext() {
        return mContext;
    }

    public void setContext(TransformContext context) {
        mContext = context;
    }
}
