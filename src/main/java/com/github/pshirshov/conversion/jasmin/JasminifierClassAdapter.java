package com.github.pshirshov.conversion.jasmin; /***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.github.pshirshov.org.objectweb.asm.*;
import com.github.pshirshov.org.objectweb.asm.tree.*;
import com.github.pshirshov.org.objectweb.asm.util.Printer;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JasminifierClassAdapter extends ClassVisitor {

    /**
     * The print writer to be used to print the class.
     */
    protected PrintWriter pw;

    /**
     * The label names. This map associate String values to Label keys.
     */
    protected final Map<Label, String> labelNames;

    /**
     * Constructs a new {@link JasminifierClassAdapter}.
     *
     * @param pw the print writer to be used to print the class.
     */
    public JasminifierClassAdapter(final PrintWriter pw) {
        super(Opcodes.ASM5, new ClassNode());
        this.pw = pw;
        labelNames = new HashMap<Label, String>();
    }


    @Override
    public void visitEnd() {
        ClassNode cn = (ClassNode) cv;
        pwprint(".bytecode ");
        pwprint(cn.version & 0xFFFF);
        pwprint('.');
        pwprintln(cn.version >>> 16);
        println(".source ", cn.sourceFile);
        pwprint(".class");
        pwprint(access(cn.access));
        pwprint(' ');
        pwprintln(cn.name);
        if (cn.superName == null) { // TODO Jasmin bug workaround
            println(".super ", "java/lang/Object");
        } else {
            println(".super ", cn.superName);
        }
        for (int i = 0; i < cn.interfaces.size(); ++i) {
            println(".implements ", cn.interfaces.get(i).toString());
        }
        if (cn.signature != null)
            println(".signature ", '"' + cn.signature + '"');
        if (cn.outerClass != null) {
            pwprint(".enclosing method ");
            pwprint(cn.outerClass);
            if (cn.outerMethod != null) {
                pwprint('/');
                pwprint(cn.outerMethod);
                pwprintln(cn.outerMethodDesc);
            } else {
                pwprintln();
            }
        }
        if ((cn.access & Opcodes.ACC_DEPRECATED) != 0) {
            pwprintln(".deprecated");
        }
        printAnnotations(cn.visibleAnnotations, 1);
        printAnnotations(cn.invisibleAnnotations, 2);
        println(".debug ", cn.sourceDebug == null ? null
                : '"' + cn.sourceDebug + '"');

        for (int i = 0; i < cn.innerClasses.size(); ++i) {
            InnerClassNode in = (InnerClassNode) cn.innerClasses.get(i);
            pwprint(".inner class");
            pwprint(access(in.access));
            if (in.innerName != null) {
                pwprint(' ');
                pwprint(in.innerName);
            }
            if (in.name != null) {
                pwprint(" inner ");
                pwprint(in.name);
            }
            if (in.outerName != null) {
                pwprint(" outer ");
                pwprint(in.outerName);
            }
            pwprintln();
        }

        for (int i = 0; i < cn.fields.size(); ++i) {
            FieldNode fn = (FieldNode) cn.fields.get(i);
            boolean annotations = false;
            if (fn.visibleAnnotations != null
                    && fn.visibleAnnotations.size() > 0) {
                annotations = true;
            }
            if (fn.invisibleAnnotations != null
                    && fn.invisibleAnnotations.size() > 0) {
                annotations = true;
            }
            boolean deprecated = (fn.access & Opcodes.ACC_DEPRECATED) != 0;
            pwprint("\n.field");
            pwprint(access(fn.access));
            pwprint(" '");
            pwprint(fn.name);
            pwprint("' ");
            pwprint(fn.desc);
            if (fn.signature != null && (!deprecated && !annotations)) {
                pwprint(" signature \"");
                pwprint(fn.signature);
                pwprint("\"");
            }
            if (fn.value instanceof String) {
                StringBuffer buf = new StringBuffer();
                Printer.appendString(buf, (String) fn.value);
                pwprint(" = ");
                pwprint(buf.toString());
            } else if (fn.value != null) {
                pwprint(" = ");
                print(fn.value);
                pwprintln();
            }
            pwprintln();
            if (fn.signature != null && (deprecated || annotations)) {
                pwprint(".signature \"");
                pwprint(fn.signature);
                pwprintln("\"");
            }
            if (deprecated) {
                pwprintln(".deprecated");
            }
            printAnnotations(fn.visibleAnnotations, 1);
            printAnnotations(fn.invisibleAnnotations, 2);
            if (deprecated || annotations) {
                pwprintln(".end field");
            }
        }

        for (int i = 0; i < cn.methods.size(); ++i) {
            MethodNode mn = (MethodNode) cn.methods.get(i);
            pwprint("\n.method");
            pwprint(access(mn.access));
            pwprint(' ');
            pwprint(mn.name);
            pwprintln(mn.desc);
            setShift(2);
            if (mn.signature != null) {
                pwprint(".signature \"");
                pwprint(mn.signature);
                pwprintln("\"");
            }
            if (mn.annotationDefault != null) {
                pwprintln(".annotation default");
                printAnnotationValue(mn.annotationDefault);
                pwprintln(".end annotation");
            }
            printAnnotations(mn.visibleAnnotations, 1);
            printAnnotations(mn.invisibleAnnotations, 2);
            if (mn.visibleParameterAnnotations != null) {
                for (int j = 0; j < mn.visibleParameterAnnotations.length; ++j) {
                    printAnnotations(mn.visibleParameterAnnotations[j], 1);
                }
            }
            if (mn.invisibleParameterAnnotations != null) {
                for (int j = 0; j < mn.invisibleParameterAnnotations.length; ++j) {
                    printAnnotations(mn.invisibleParameterAnnotations[j], 2);
                }
            }
            for (int j = 0; j < mn.exceptions.size(); ++j) {
                println(".throws ", mn.exceptions.get(j).toString());
            }
            if ((mn.access & Opcodes.ACC_DEPRECATED) != 0) {
                pwprintln(".deprecated");
            }
            if (mn.instructions.size() > 0) {
                labelNames.clear();
                for (int j = 0; j < mn.tryCatchBlocks.size(); ++j) {
                    TryCatchBlockNode tcb = (TryCatchBlockNode) mn.tryCatchBlocks.get(j);
                    pwprint(".catch ");
                    pwprint(tcb.type);
                    pwprint(" from ");
                    print(tcb.start);
                    pwprint(" to ");
                    print(tcb.end);
                    pwprint(" using ");
                    print(tcb.handler);
                    pwprintln();
                }
                setShift(2);
                for (int j = 0; j < mn.instructions.size(); ++j) {
                    AbstractInsnNode in = mn.instructions.get(j);
                    in.accept(new MethodVisitor(Opcodes.ASM5) {

                        @Override
                        public void visitFrame(int type, int local,
                                               Object[] locals, int stack, Object[] stacks) {
                            if (type != Opcodes.F_FULL && type != Opcodes.F_NEW) {
                                throw new RuntimeException(
                                        "Compressed frames unsupported, use EXPAND_FRAMES option");
                            }
                            printShift();
                            pwprintln(".stack");
                            setShift(6);
                            for (int i = 0; i < local; ++i) {
                                pwprint("locals ");
                                printFrameType(locals[i]);
                                pwprintln();
                            }
                            for (int i = 0; i < stack; ++i) {
                                pwprint("stack ");
                                printFrameType(stacks[i]);
                                pwprintln();
                            }
                            setShift(4);
                            printShift();
                            pwprintln(".end stack");
                        }

                        @Override
                        public void visitInsn(int opcode) {
                            print(opcode);
                            pwprintln();
                        }

                        @Override
                        public void visitIntInsn(int opcode, int operand) {
                            print(opcode);
                            if (opcode == Opcodes.NEWARRAY) {
                                switch (operand) {
                                    case Opcodes.T_BOOLEAN:
                                        pwprintln(" boolean");
                                        break;
                                    case Opcodes.T_CHAR:
                                        pwprintln(" char");
                                        break;
                                    case Opcodes.T_FLOAT:
                                        pwprintln(" float");
                                        break;
                                    case Opcodes.T_DOUBLE:
                                        pwprintln(" double");
                                        break;
                                    case Opcodes.T_BYTE:
                                        pwprintln(" byte");
                                        break;
                                    case Opcodes.T_SHORT:
                                        pwprintln(" short");
                                        break;
                                    case Opcodes.T_INT:
                                        pwprintln(" int");
                                        break;
                                    case Opcodes.T_LONG:
                                    default:
                                        pwprintln(" long");
                                        break;
                                }
                            } else {
                                pwprint(' ');
                                pwprintln(operand);
                            }
                        }

                        @Override
                        public void visitVarInsn(int opcode, int var) {
                            print(opcode);
                            pwprint(' ');
                            pwprintln(var);
                        }

                        @Override
                        public void visitTypeInsn(int opcode, String type) {
                            print(opcode);
                            pwprint(' ');
                            pwprintln(type);
                        }

                        @Override
                        public void visitFieldInsn(int opcode, String owner,
                                                   String name, String desc) {
                            print(opcode);
                            pwprint(' ');
                            pwprint(owner);
                            pwprint('/');
                            pwprint(name);
                            pwprint(' ');
                            pwprintln(desc);
                        }

                        @Override
                        public void visitMethodInsn(int opcode, String owner,
                                                    String name, String desc, boolean itf) {
                            print(opcode);
                            pwprint(' ');
                            pwprint(owner);
                            pwprint('/');
                            pwprint(name);
                            pwprint(desc);
                            if (opcode == Opcodes.INVOKEINTERFACE) {
                                pwprint(' ');
                                pwprint((Type.getArgumentsAndReturnSizes(desc) >> 2) - 1);
                            }
                            pwprintln();
                        }

                        @Override
                        public void visitJumpInsn(int opcode, Label label) {
                            print(opcode);
                            pwprint(' ');
                            print(label);
                            pwprintln();
                        }

                        @Override
                        public void visitLabel(Label label) {
                            setShift(2);
                            print(label);
                            pwprintln(':');
                            setShift(4);
                        }

                        @Override
                        public void visitLdcInsn(Object cst) {
                            pwprint("ldc ");
                            if (cst instanceof Type) {
                                pwprint(((Type) cst).getInternalName());
                            } else {
                                print(cst);
                            }
                            pwprintln();
                        }

                        @Override
                        public void visitIincInsn(int var, int increment) {
                            pwprint("iinc ");
                            pwprint(var);
                            pwprint(' ');
                            pwprintln(increment);
                        }

                        @Override
                        public void visitTableSwitchInsn(int min, int max,
                                                         Label dflt, Label... labels) {
                            pwprint("tableswitch ");
                            pwprintln(min);
                            for (int i = 0; i < labels.length; ++i) {
                                print(labels[i]);
                                pwprintln();
                            }
                            pwprint("default : ");
                            print(dflt);
                            pwprintln();
                        }

                        @Override
                        public void visitLookupSwitchInsn(Label dflt,
                                                          int[] keys, Label[] labels) {
                            if (keys.length == 0) {
                                pwprint("goto "); // TODO Jasmin bug
                                // workaround
                                print(dflt);
                                pwprintln();
                                return;
                            }
                            pwprintln("lookupswitch");
                            for (int i = 0; i < keys.length; ++i) {
                                pwprint(keys[i]);
                                pwprint(" : ");
                                print(labels[i]);
                                pwprintln();
                            }
                            pwprint("default : ");
                            print(dflt);
                            pwprintln();
                        }

                        @Override
                        public void visitMultiANewArrayInsn(String desc,
                                                            int dims) {
                            pwprint("multianewarray ");
                            pwprint(desc);
                            pwprint(' ');
                            pwprintln(dims);
                        }

                        @Override
                        public void visitLineNumber(int line, Label start) {
                            pwprint(".line ");
                            pwprintln(line);
                        }
                    });
                }
                setShift(2);
                for (int j = 0; j < mn.localVariables.size(); ++j) {
                    LocalVariableNode lv = (LocalVariableNode) mn.localVariables.get(j);
                    pwprint(".var ");
                    pwprint(lv.index);
                    pwprint(" is '");
                    pwprint(lv.name);
                    pwprint("' ");
                    pwprint(lv.desc);
                    if (lv.signature != null) {
                        pwprint(" signature \"");
                        pwprint(lv.signature);
                        pwprint("\"");
                    }
                    pwprint(" from ");
                    print(lv.start);
                    pwprint(" to ");
                    print(lv.end);
                    pwprintln();
                }
                println(".limit locals ", Integer.toString(mn.maxLocals));
                println(".limit stack ", Integer.toString(mn.maxStack));
            }
            pwprintln(".end method");
        }
        super.visitEnd();
    }

    protected void println(final String directive, final String arg) {
        if (arg != null) {
            pwprint(directive);
            pwprintln(arg);
        }
    }

    protected void pwprint(final Object arg) {
        if (doShift) {
            printShift();
            doShift = false;
        }
        pw.print(arg);
    }

    protected Integer shift = 0;
    protected Boolean doShift = false;

    public Integer getShift() {
        return shift;
    }


    public void setShift(Integer shift) {
        this.shift = shift;
    }

    protected void pwprintln(final Object arg) {
        pw.println(arg);
        doShift = true;
    }


    private void printShift() {
        for (int i = 0; i < shift; ++i) {
            pw.print(' ');
        }
    }


    protected void pwprintln() {
        pw.println();
        doShift = true;
    }


    protected String access(final int access) {
        StringBuilder b = new StringBuilder();
        if ((access & Opcodes.ACC_PUBLIC) != 0) {
            b.append(" public");
        }
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            b.append(" private");
        }
        if ((access & Opcodes.ACC_PROTECTED) != 0) {
            b.append(" protected");
        }
        if ((access & Opcodes.ACC_STATIC) != 0) {
            b.append(" static");
        }
        if ((access & Opcodes.ACC_FINAL) != 0) {
            b.append(" final");
        }
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
            b.append(" synchronized");
        }
        if ((access & Opcodes.ACC_VOLATILE) != 0) {
            b.append(" volatile");
        }
        if ((access & Opcodes.ACC_TRANSIENT) != 0) {
            b.append(" transient");
        }
        if ((access & Opcodes.ACC_NATIVE) != 0) {
            b.append(" native");
        }
        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            b.append(" abstract");
        }
        if ((access & Opcodes.ACC_STRICT) != 0) {
            b.append(" fpstrict");
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            b.append(" synthetic");
        }
        if ((access & Opcodes.ACC_INTERFACE) != 0) {
            b.append(" interface");
        }
        if ((access & Opcodes.ACC_ANNOTATION) != 0) {
            b.append(" annotation");
        }
        if ((access & Opcodes.ACC_ENUM) != 0) {
            b.append(" enum");
        }
        if ((access & Opcodes.ACC_MANDATED) != 0) {
            b.append(" mandated");
        }
        return b.toString();
    }

    protected void print(final int opcode) {
        pwprint(Printer.OPCODES[opcode].toLowerCase());
    }

    protected void print(final Object cst) {
        if (cst instanceof String) {
            StringBuffer buf = new StringBuffer();
            Printer.appendString(buf, (String) cst);
            pwprint(buf.toString());
        } else if (cst instanceof Float) {
            Float f = (Float) cst;
            if (f.isNaN() || f.isInfinite()) {
                pwprint("0.0"); // TODO Jasmin bug workaround
            } else {
                pwprint(f);
            }
        } else if (cst instanceof Double) {
            Double d = (Double) cst;
            if (d.isNaN() || d.isInfinite()) {
                pwprint("0.0"); // TODO Jasmin bug workaround
            } else {
                pwprint(d);
            }
        } else {
            pwprint(cst);
        }
    }

    protected void print(final Label l) {
        String name = labelNames.get(l);
        if (name == null) {
            name = "L" + labelNames.size();
            labelNames.put(l, name);
        }
        pwprint(name);

    }

    protected void print(final LabelNode l) {
        print(l.getLabel());
    }

    protected void printAnnotations(final List<AnnotationNode> annotations,
                                    int visible) {
        if (annotations != null) {
            for (int j = 0; j < annotations.size(); ++j) {
                printAnnotation(annotations.get(j), visible, -1);
            }
        }
    }

    protected void printAnnotation(final AnnotationNode n, final int visible,
                                   final int param) {
        pwprint(".annotation ");
        if (visible > 0) {
            if (param == -1) {
                pwprint(visible == 1 ? "visible " : "invisible ");
            } else {
                pwprint(visible == 1 ? "visibleparam " : "invisibleparam ");
                pwprint(param);
                pwprint(' ');
            }
            pwprint(n.desc);
        }
        pwprintln();
        if (n.values != null) {
            for (int i = 0; i < n.values.size(); i += 2) {
                pwprint(n.values.get(i));
                pwprint(' ');
                printAnnotationValue(n.values.get(i + 1));
            }
        }
        pwprintln(".end annotation");
    }

    protected void printAnnotationValue(final Object value) {
        if (value instanceof String[]) {
            pwprint("e ");
            pwprint(((String[]) value)[0]);
            pwprint(" = ");
            print(((String[]) value)[1]);
            pwprintln();
        } else if (value instanceof AnnotationNode) {
            pwprint("@ ");
            pwprint(((AnnotationNode) value).desc);
            pwprint(" = ");
            printAnnotation((AnnotationNode) value, 0, -1);
        } else if (value instanceof byte[]) {
            pwprint("[B = ");
            byte[] v = (byte[]) value;
            for (int i = 0; i < v.length; i++) {
                pwprint(v[i]);
                pwprint(' ');
            }
            pwprintln();
        } else if (value instanceof boolean[]) {
            pwprint("[Z = ");
            boolean[] v = (boolean[]) value;
            for (int i = 0; i < v.length; i++) {
                pwprint(v[i] ? '1' : '0');
                pwprint(' ');
            }
            pwprintln();
        } else if (value instanceof short[]) {
            pwprint("[S = ");
            short[] v = (short[]) value;
            for (int i = 0; i < v.length; i++) {
                pwprint(v[i]);
                pwprint(' ');
            }
            pwprintln();
        } else if (value instanceof char[]) {
            pwprint("[C = ");
            char[] v = (char[]) value;
            for (int i = 0; i < v.length; i++) {
                pwprint((int) v[i]);
                pwprint(' ');
            }
            pwprintln();
        } else if (value instanceof int[]) {
            pwprint("[I = ");
            int[] v = (int[]) value;
            for (int i = 0; i < v.length; i++) {
                pwprint(v[i]);
                pwprint(' ');
            }
            pwprintln();
        } else if (value instanceof long[]) {
            pwprint("[J = ");
            long[] v = (long[]) value;
            for (int i = 0; i < v.length; i++) {
                pwprint(v[i]);
                pwprint(' ');
            }
            pwprintln();
        } else if (value instanceof float[]) {
            pwprint("[F = ");
            float[] v = (float[]) value;
            for (int i = 0; i < v.length; i++) {
                print(new Float(v[i]));
                pwprint(' ');
            }
            pwprintln();
        } else if (value instanceof double[]) {
            pwprint("[D = ");
            double[] v = (double[]) value;
            for (int i = 0; i < v.length; i++) {
                print(new Double(v[i]));
                pwprint(' ');
            }
            pwprintln();
        } else if (value instanceof List) {
            List<?> l = (List<?>) value;
            if (l.size() > 0) {
                Object o = l.get(0);
                if (o instanceof String[]) {
                    pwprint("[e ");
                    pwprint(((String[]) o)[0]);
                    pwprint(" = ");
                } else if (o instanceof AnnotationNode) {
                    pwprint("[& ");
                    pwprint(((AnnotationNode) o).desc);
                    pwprint(" = ");
                    pwprint("[@ = ");
                } else if (o instanceof String) {
                    pwprint("[s = ");
                } else if (o instanceof Byte) {
                    pwprint("[B = ");
                } else if (o instanceof Boolean) {
                    pwprint("[Z = ");
                } else if (o instanceof Character) {
                    pwprint("[C = ");
                } else if (o instanceof Short) {
                    pwprint("[S = ");
                } else if (o instanceof Type) {
                    pwprint("[c = ");
                } else if (o instanceof Integer) {
                    pwprint("[I = ");
                } else if (o instanceof Float) {
                    pwprint("[F = ");
                } else if (o instanceof Long) {
                    pwprint("[J = ");
                } else if (o instanceof Double) {
                    pwprint("[D = ");
                }
                for (int j = 0; j < l.size(); ++j) {
                    printAnnotationArrayValue(l.get(j));
                    pwprint(' ');
                }
            } else {
                pwprint("; empty array annotation value");
            }
            pwprintln();
        } else if (value instanceof String) {
            pwprint("s = ");
            print(value);
            pwprintln();
        } else if (value instanceof Byte) {
            pwprint("B = ");
            pwprintln(((Byte) value).intValue());
        } else if (value instanceof Boolean) {
            pwprint("Z = ");
            pwprintln(((Boolean) value).booleanValue() ? 1 : 0);
        } else if (value instanceof Character) {
            pwprint("C = ");
            pwprintln(new Integer(((Character) value).charValue()));
        } else if (value instanceof Short) {
            pwprint("S = ");
            pwprintln(((Short) value).intValue());
        } else if (value instanceof Type) {
            pwprint("c = ");
            pwprintln(((Type) value).getDescriptor());
        } else if (value instanceof Integer) {
            pwprint("I = ");
            print(value);
            pwprintln();
        } else if (value instanceof Float) {
            pwprint("F = ");
            print(value);
            pwprintln();
        } else if (value instanceof Long) {
            pwprint("J = ");
            print(value);
            pwprintln();
        } else if (value instanceof Double) {
            pwprint("D = ");
            print(value);
            pwprintln();
        } else {
            throw new RuntimeException();
        }
    }

    protected void printAnnotationArrayValue(final Object value) {
        if (value instanceof String[]) {
            print(((String[]) value)[1]);
        } else if (value instanceof AnnotationNode) {
            printAnnotation((AnnotationNode) value, 0, -1);
        } else if (value instanceof String) {
            print(value);
        } else if (value instanceof Byte) {
            pwprint(((Byte) value).intValue());
        } else if (value instanceof Boolean) {
            pwprint(((Boolean) value).booleanValue() ? 1 : 0);
        } else if (value instanceof Character) {
            pwprint(new Integer(((Character) value).charValue()));
        } else if (value instanceof Short) {
            pwprint(((Short) value).intValue());
        } else if (value instanceof Type) {
            pwprint(((Type) value).getDescriptor());
        } else {
            print(value);
        }
    }

    protected void printFrameType(final Object type) {
        if (type == Opcodes.TOP) {
            pwprint("Top");
        } else if (type == Opcodes.INTEGER) {
            pwprint("Integer");
        } else if (type == Opcodes.FLOAT) {
            pwprint("Float");
        } else if (type == Opcodes.LONG) {
            pwprint("Long");
        } else if (type == Opcodes.DOUBLE) {
            pwprint("Double");
        } else if (type == Opcodes.NULL) {
            pwprint("Null");
        } else if (type == Opcodes.UNINITIALIZED_THIS) {
            pwprint("UninitializedThis");
        } else if (type instanceof Label) {
            pwprint("Uninitialized ");
            print((Label) type);
        } else {
            pwprint("Object ");
            pwprint(type);
        }
    }
}