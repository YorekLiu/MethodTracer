package xyz.yorek.plugin.mt.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import xyz.yorek.plugin.mt.util.Util;

/**
 * Created by yorek.liu on 2021/8/2
 *
 * @author yorek.liu
 */
public class ClassGraph {
    private final Node root;
    private final HashMap<String, Node> class2NodeMap = new HashMap<>(1 << 24);

    public ClassGraph() {
        ClassRecord object = new ClassRecord("java/lang/Object");
        root = new Node(object);
        class2NodeMap.put("java/lang/Object", root);
    }

    public void add(ClassRecord record) {
        String superName = record.superName;
        if (Util.isNullOrNil(superName)) return;

        Node parent = class2NodeMap.get(superName);
        // parent不存在，则创建一个并添加到树中，并设置parent为object
        if (parent == null) {
            ClassRecord parentClassRecord = new ClassRecord(superName);
            parent = new Node(parentClassRecord);
            parent.parent = root;
            class2NodeMap.put(superName, parent);
        }

        // 检查child是否存在
        // 若child存在，则是作为别的Node的parent添加的，此时需要更新下信息
        Node child;
        if (class2NodeMap.get(record.name) == null) {
            child = new Node(record);
            child.parent = parent;
            parent.children.add(child);
            class2NodeMap.put(record.name, child);
        } else {
            child = class2NodeMap.get(record.name);
            child.val = record;
            child.parent = parent;
            parent.children.add(child);
        }
    }

    public boolean isAssignableFrom(String className, String superName) {
        if (className.equals(superName))
            return true;

        Node node = class2NodeMap.get(className);
        if (node == null) return false;

        Node p = node;
        while (p.parent != null) {
            if (p.val.name.equals(superName))
                return true;
            p = p.parent;
        }

        return false;
    }

    /**
     * 获取所有继承了baseClassName的类，包括自己
     */
    public List<String> getExtendedClass(String baseClassName) {
        List<String> classList = new ArrayList<>();
        classList.add(baseClassName);

        Node node = class2NodeMap.get(baseClassName);
        if (node == null) return classList;

        Queue<Node> queue = new LinkedList<>();
        queue.offer(node);

        while (!queue.isEmpty()) {
            Node p = queue.poll();
            for (Node child : p.children) {
                classList.add(child.val.name);
                if (!child.children.isEmpty()) {
                    queue.offer(child);
                }
            }
        }

        return classList;
    }

    private static class Node {
        public ClassRecord val;
        public List<Node> children;
        public Node parent;

        public Node(ClassRecord val) {
            this.val = val;
            children = new ArrayList<>();
        }
    }
}
