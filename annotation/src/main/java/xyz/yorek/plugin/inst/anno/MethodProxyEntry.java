package xyz.yorek.plugin.inst.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by yorek.liu on 2021/8/12
 *
 * @author yorek.liu
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface MethodProxyEntry {
}
