package xyz.yorek.plugin.sample

import android.app.Application
import android.content.Context
import android.util.Log
import com.facebook.soloader.DirectorySoSource
import com.facebook.soloader.SoLoader
import com.facebook.soloader.SoSource
import dalvik.system.PathClassLoader
import java.io.BufferedInputStream
import java.io.File
import java.lang.Exception
import java.lang.reflect.Proxy
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.ArrayList
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Created by yorek.liu on 2021/9/2
 *
 * @author yorek.liu
 */
object PackedAppLibsExtract {
    private const val TAG = "PackedAppLibsExtract"
    private const val NATIVE_DIR = "palibs"
    private const val PACKED_APP_LIBS_NAME = "applibs.zip"

    fun extract(context: Context) {
        val assetManager = context.assets
        val inputStream = assetManager.open(PACKED_APP_LIBS_NAME)
        val zipInputStream = ZipInputStream(BufferedInputStream(inputStream))
//        val nativeDir = context.applicationInfo.nativeLibraryDir
        val outputDir = getDir(context)
        var zipEntry: ZipEntry?
        while ((zipInputStream.nextEntry.also { zipEntry = it }) != null) {
            val entry = zipEntry!!
            val zipEntryFile = File(outputDir, entry.name)
            if (entry.isDirectory) {
                zipEntryFile.mkdirs()
            } else {
                Util.copyFileUsingStream(zipInputStream, zipEntryFile)
            }
            zipInputStream.closeEntry()
        }
        zipInputStream.close()

        val file = File(outputDir.absolutePath + File.separator + "lib" + File.separator + "arm64-v8a")
        val directorySoSource = DirectorySoSource(file, SoSource.LOAD_FLAG_ALLOW_IMPLICIT_PROVISION)
        SoLoader.prependSoSource(directorySoSource)
//        try {
//            registerNativeLibPath(context, file)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }

    @Throws(Exception::class)
    private fun registerNativeLibPath(context: Context, file: File){
        val classLoader = this.javaClass.classLoader
        val pathListField = classLoader.javaClass.superclass.getDeclaredField("pathList")
        pathListField.isAccessible = true
        val pathList = pathListField.get(classLoader)
        val nativeLibField = pathList.javaClass.getDeclaredField("nativeLibraryDirectories")
        nativeLibField.isAccessible = true
        val nativeLibraryDirectories: ArrayList<File> = nativeLibField.get(pathList) as ArrayList<File>
        nativeLibraryDirectories.add(0, file)
        Log.e(TAG, nativeLibraryDirectories.toString())

        val makePathElementsMethod = pathList.javaClass.getDeclaredMethod("makePathElements", java.util.List::class.java)
        makePathElementsMethod.isAccessible = true

        val systemNativeLibraryDirectoriesField = pathList.javaClass.getDeclaredField("systemNativeLibraryDirectories")
        systemNativeLibraryDirectoriesField.isAccessible = true
        val systemNativeLibraryDirectories: ArrayList<File> = systemNativeLibraryDirectoriesField.get(pathList) as ArrayList<File>
        val getAllNativeLibraryDirectories = ArrayList<File>(nativeLibraryDirectories)
        getAllNativeLibraryDirectories.addAll(systemNativeLibraryDirectories)
        val makePathElements = makePathElementsMethod.invoke(null, getAllNativeLibraryDirectories)

        val nativeLibraryPathElementsField = pathList.javaClass.getDeclaredField("nativeLibraryPathElements")
        nativeLibraryPathElementsField.isAccessible = true
        nativeLibraryPathElementsField.set(pathList, makePathElements)
    }

    fun getDir(context: Context): File {
        return context.getDir(NATIVE_DIR, Context.MODE_PRIVATE)
    }
}