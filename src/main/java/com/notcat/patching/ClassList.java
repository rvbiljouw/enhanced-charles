package com.notcat.patching;

import javassist.ClassPool;
import javassist.NotFoundException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassList {
    private final List<String> classes = new ArrayList<>();
    private final Patcher patcher;

    public ClassList(Patcher patcher) {
        this.patcher = patcher;
    }

    public void load() throws Exception {
        classes.clear();

        patcher.classPool.insertClassPath(patcher.inputPath.toString());
        try (final JarFile file = new JarFile(this.patcher.inputPath.toFile())) {
            final Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    classes.add(entry.getName().replaceAll("/", ".").replace(".class", ""));
                }
            }
        }
    }

    public ClassPool getPool() {
        return patcher.classPool;
    }

    public List<String> getClasses() {
        return classes;
    }

}
