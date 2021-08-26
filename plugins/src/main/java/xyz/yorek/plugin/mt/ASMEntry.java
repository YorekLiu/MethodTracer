package xyz.yorek.plugin.mt;

import org.gradle.api.GradleException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import xyz.yorek.plugin.mt.visitor.BaseClassVisitor;

public class ASMEntry {

    public static final int ASM_VERSION = Opcodes.ASM6;

    public static ClassWriter run(InputStream is, TransformContext context) throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        ClassVisitor mPrevClassVisitor;
        // build visitorList
        mPrevClassVisitor = classWriter;
        List<Class<BaseClassVisitor>> baseVisitorClassList = context.getVisitorList();
        for (Class<BaseClassVisitor> clazz : baseVisitorClassList) {
            try {
                Constructor<BaseClassVisitor> constructor = clazz.getDeclaredConstructor(int.class, ClassVisitor.class);
                BaseClassVisitor baseClassVisitor = constructor.newInstance(ASM_VERSION, mPrevClassVisitor);
                baseClassVisitor.setContext(context);
                mPrevClassVisitor = baseClassVisitor;
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                Log.w("ASMCode", "newInstance %s error: %s", clazz.getSimpleName(), e.getMessage());
                throw new GradleException(e.getMessage());
            }
        }

        ClassReader classReader = new ClassReader(is);
        classReader.accept(mPrevClassVisitor, ClassReader.EXPAND_FRAMES);
        return classWriter;
    }
}
