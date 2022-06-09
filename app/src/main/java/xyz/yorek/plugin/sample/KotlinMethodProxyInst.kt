package xyz.yorek.plugin.sample

import xyz.yorek.plugin.inst.anno.MethodProxy
import xyz.yorek.plugin.inst.anno.MethodProxyEntry

@MethodProxyEntry
object KotlinMethodProxyInst {

    @JvmStatic
    @MethodProxy(clazz = [ MainActivity::class ], method = "plus")
    fun d(mainActivity: MainActivity): String {
        return "D"
    }
}