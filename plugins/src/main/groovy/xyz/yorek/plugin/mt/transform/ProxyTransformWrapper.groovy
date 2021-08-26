package xyz.yorek.plugin.mt.transform

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.android.build.api.transform.Transform
import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import xyz.yorek.plugin.mt.util.ReflectUtil

import java.lang.reflect.Field

abstract class ProxyTransformWrapper extends Transform {
    protected final Transform origTransform

    ProxyTransformWrapper(Transform origTransform) {
        this.origTransform = origTransform
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return origTransform.getInputTypes()
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return origTransform.getScopes()
    }

    @Override
    boolean isIncremental() {
        return origTransform.isIncremental()
    }

    protected static String getUniqueJarName(File jarFile) {
        final String origJarName = jarFile.getName()
        final String hashing = Hashing.sha1().hashString(jarFile.getPath(), Charsets.UTF_16LE).toString()
        final int dotPos = origJarName.lastIndexOf('.')
        if (dotPos < 0) {
            return "${origJarName}_${hashing}"
        } else {
            final String nameWithoutDotExt = origJarName.substring(0, dotPos)
            final String dotExt = origJarName.substring(dotPos)
            return "${nameWithoutDotExt}_${hashing}${dotExt}"
        }
    }

    protected static void replaceFile(QualifiedContent input, File newFile) {
        final Field fileField = ReflectUtil.getDeclaredFieldRecursive(input.getClass(), 'file')
        fileField.set(input, newFile)
    }

    protected static void replaceChangedFile(DirectoryInput dirInput, Map<File, Status> changedFiles) {
        final Field changedFilesField = ReflectUtil.getDeclaredFieldRecursive(dirInput.getClass(), 'changedFiles')
        changedFilesField.set(dirInput, changedFiles)
    }
}
