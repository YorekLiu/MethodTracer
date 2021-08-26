package xyz.yorek.plugin.mt.graph;

import org.objectweb.asm.ClassVisitor;

import xyz.yorek.plugin.mt.model.ClassGraph;
import xyz.yorek.plugin.mt.model.ClassRecord;
import xyz.yorek.plugin.mt.util.Util;

/**
 * Created by yorek.liu on 2021/8/3
 *
 * @author yorek.liu
 */
public class ClassGraphVisitor extends ClassVisitor {
    private final ClassGraph mClassGraph;

    public ClassGraphVisitor(int api, ClassVisitor cv, ClassGraph classGraph) {
        super(api, cv);
        mClassGraph = classGraph;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (!Util.isNullOrNil(name) && !Util.isNullOrNil(superName) ) {
            mClassGraph.add(new ClassRecord(version, access, name, signature, superName, interfaces));
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }
}
