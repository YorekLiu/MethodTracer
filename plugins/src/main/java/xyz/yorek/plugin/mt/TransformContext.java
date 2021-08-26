package xyz.yorek.plugin.mt;

import java.util.List;

import xyz.yorek.plugin.mt.model.ClassGraph;
import xyz.yorek.plugin.mt.model.Configuration;
import xyz.yorek.plugin.mt.model.FirstTraceContext;
import xyz.yorek.plugin.mt.model.MethodProxyContext;
import xyz.yorek.plugin.mt.visitor.BaseClassVisitor;

/**
 * Created by yorek.liu on 2021/8/4
 *
 * @author yorek.liu
 */
public class TransformContext {
    Configuration configuration;
    FirstTraceContext firstTraceContext;
    List<Class<BaseClassVisitor>> visitorList;

    public TransformContext(Configuration configuration, FirstTraceContext firstTraceContext, List<Class<BaseClassVisitor>> visitorList) {
        this.configuration = configuration;
        this.firstTraceContext = firstTraceContext;
        this.visitorList = visitorList;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ClassGraph getClassGraph() {
        return firstTraceContext.classGraph;
    }

    public List<Class<BaseClassVisitor>> getVisitorList() {
        return visitorList;
    }

    public MethodProxyContext getMethodProxyContext() {
        return firstTraceContext.methodProxyContent;
    }
}
