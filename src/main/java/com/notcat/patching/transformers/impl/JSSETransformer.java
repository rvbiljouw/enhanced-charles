package com.notcat.patching.transformers.impl;

import com.notcat.patching.ClassList;
import com.notcat.patching.TransformedClass;
import com.notcat.patching.transformers.ITransformer;
import javassist.*;
import javassist.bytecode.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class JSSETransformer implements ITransformer {


    private boolean processCode(ConstPool cp, CtMethod method, CodeAttribute code) {
        final CodeIterator iterator = code.iterator();
        while (iterator.hasNext()) {
            try {
                final int idx = iterator.next();
                final int opcode = iterator.byteAt(idx);
                if (opcode != Opcode.INVOKESTATIC) {
                    continue;
                }

                final int methodRefIdx = iterator.byteAt(idx + 2);
                if (cp.getTag(methodRefIdx) == ConstPool.CONST_Methodref) {
                    final int targetClassIdx = cp.getMethodrefClass(methodRefIdx);
                    final String targetClass = cp.getClassInfo(targetClassIdx);
                    final String methodName = cp.getMethodrefName(methodRefIdx);
                    if (targetClass.equals("java.security.Security") && methodName.equals("addProvider")) {
                        final int conscryptClassRef = cp.addClassInfo("org/conscrypt/Conscrypt");

                        final Bytecode bytecode = new Bytecode(cp);
                        bytecode.addInvokestatic(conscryptClassRef, "newProvider", "()Ljava/Security/Provider;");
                        bytecode.addInvokestatic(targetClassIdx, "setProvider", "(Ljava/Security/Provider;)V");
                        bytecode.add(Opcode.NOP);
                        bytecode.add(Opcode.NOP);
                        bytecode.add(Opcode.NOP);
                        bytecode.add(Opcode.NOP);
                        bytecode.add(Opcode.NOP);
                        iterator.write( bytecode.get(), 0);
                        System.out.println("Switched security provider in " + method.getName() + " " + method.getDeclaringClass().getName());
                        return true;
                    }
                }
            } catch (BadBytecode e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    private boolean processClass(CtClass clazz) {
        try {
            clazz.defrost();

            final ConstPool cp = clazz.getClassFile().getConstPool();
            if (!cp.getClassNames().contains("java/security/Security")) {
                return false;
            }

            boolean modificationsMade = false;
            for (CtMethod method : clazz.getMethods()) {
                final CodeAttribute code = method.getMethodInfo().getCodeAttribute();
                if (code == null) {
                    continue;
                }

                if (processCode(cp, method, code)) {
                    modificationsMade = true;
                }
            }
            return modificationsMade;
        } finally {
            clazz.freeze();
        }
    }

    @Override
    public Iterable<TransformedClass> transform(ClassList classList) {
        final List<TransformedClass> transformed = new ArrayList<>();
        final ClassPool classPool = classList.getPool();
        for (final String className : classList.getClasses()) {
            try {
                final CtClass clazz = classPool.get(className);
                if (processClass(clazz)) {
                    transformed.add(new TransformedClass(className, clazz.toBytecode()));
                }
            } catch (NotFoundException | CannotCompileException | IOException e) {
                e.printStackTrace();
            }
        }
        return transformed;
    }

}
