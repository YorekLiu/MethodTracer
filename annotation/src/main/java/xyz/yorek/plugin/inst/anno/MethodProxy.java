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
@Target(ElementType.METHOD)
public @interface MethodProxy {
    /**
     * 要代理的class
     */
    Class<?>[] clazz() default {};

    /**
     * 要代理的className，适用于由于可见性不能直接取到class的情况
     */
    String className() default "";

    /**
     * 要代理的方法名
     * 若被代理的方法是静态方法，则该注解修饰的方法的签名一定要与被代理的保持一致
     * 若不是静态方法，方法参数第一个为该对象，其他参数从第二个开始摆放
     */
    String method();

    /**
     * 是否扩展匹配类
     */
    boolean extend() default false;
}
