/*
 * blanco Framework
 * Copyright (C) 2011 IGA Tosiki
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package blanco.beanutils;

import java.util.ArrayList;
import java.util.List;

import blanco.cg.BlancoCgObjectFactory;
import blanco.cg.valueobject.BlancoCgClass;
import blanco.cg.valueobject.BlancoCgField;
import blanco.cg.valueobject.BlancoCgMethod;
import blanco.cg.valueobject.BlancoCgSourceFile;
import blanco.commons.util.BlancoStringUtil;

public class BlancoBeanUtils {
    /**
     * Generates the copyTo method.
     * 
     * @param cgSourceFile
     *            Source code information. Used for input.
     * @param cgClass
     *            Class information. Used for input and output.
     */
    public static void generateCopyToMethod(
            final BlancoCgSourceFile cgSourceFile, final BlancoCgClass cgClass) {

        // Gets an instance of BlancoCgObjectFactory class.
        final BlancoCgObjectFactory cgFactory = BlancoCgObjectFactory
                .getInstance();

        final BlancoCgMethod method = cgFactory.createMethod("copyTo",
                "Copies this value object to the specified target.");
        cgClass.getMethodList().add(method);

        method.getLangDoc().getDescriptionList().add("<P>Cautions for use</P>");
        method.getLangDoc().getDescriptionList().add("<UL>");
        method.getLangDoc().getDescriptionList().add(
                "<LI>Only the shallow range of the object will be subject to the copying process.");
        method.getLangDoc().getDescriptionList().add(
                "<LI>Do not use this method if the object has a circular reference.");
        method.getLangDoc().getDescriptionList().add("</UL>");

        method.getParameterList().add(
                cgFactory.createParameter("target", cgSourceFile.getPackage()
                        + "." + cgClass.getName(), "target value object."));

        final List<java.lang.String> listLine = method.getLineList();

        // TODO: Adds consideration of deep copy.

        listLine.add("if (target == null) {");
        listLine.add("throw new IllegalArgumentException(\"Bug: "
                + cgClass.getName()
                + "#copyTo(target): argument 'target' is null\");");
        listLine.add("}");

        listLine.add("");
        listLine.add("// No needs to copy parent class.");
        listLine.add("");

        for (int indexField = 0; indexField < cgClass.getFieldList().size(); indexField++) {
            final BlancoCgField field = cgClass.getFieldList().get(indexField);

            // Adjusts the array information.
            boolean isArray = field.getType().getArray();
            if (isArray == false && field.getType().getName().endsWith("[]")) {
                isArray = true;
            }

            listLine.add("// Name: " + field.getName());
            listLine.add("// Type: " + field.getType().getName());

            if (field.getStatic()) {
                listLine.add("//   skipped (static field)");
                continue;
            }
            if (field.getFinal()) {
                listLine.add("//   skipped (final field)");
                continue;
            }

            String realType = field.getType().getName();
            if (BlancoStringUtil.null2Blank(field.getType().getGenerics())
                    .length() > 0) {
                realType = realType + field.getType().getGenerics();
            }

            if (isArray == false) {
                listLine
                        .addAll(getCopyFieldLine(field.getName(), realType,
                                "this." + field.getName(), "target."
                                        + field.getName()));
            } else {
                String typeNameWithoutArray = field.getType().getName();
                for (; typeNameWithoutArray.endsWith("[]");) {
                    typeNameWithoutArray = typeNameWithoutArray.substring(0,
                            typeNameWithoutArray.length() - "[]".length());
                }

                listLine.add("if (this." + field.getName() + " != null) {");
                listLine.add("target." + field.getName() + " = new "
                        + typeNameWithoutArray + "[this." + field.getName()
                        + ".length];");
                listLine.add("for (int index = 0; index < this."
                        + field.getName() + ".length; index++) {");
                listLine.addAll(getCopyFieldLine(field.getName(), realType,
                        "this." + field.getName() + "[index]", "target."
                                + field.getName() + "[index]"));

                listLine.add("}");
                listLine.add("}");
            }
        }
    }

    static List<String> getCopyFieldLine(final String fieldName,
            final String fieldType, final String origVarName,
            final String destVarName) {
        final List<String> line = new ArrayList<String>();

        String typeNameWithoutArray = fieldType;
        for (; typeNameWithoutArray.endsWith("[]");) {
            typeNameWithoutArray = typeNameWithoutArray.substring(0,
                    typeNameWithoutArray.length() - "[]".length());
        }

        String typeNameWithoutGenerics = typeNameWithoutArray;
        if (typeNameWithoutGenerics.indexOf("<") > 0) {
            typeNameWithoutGenerics = typeNameWithoutGenerics.substring(0,
                    typeNameWithoutGenerics.indexOf("<"));
        }

        if (typeNameWithoutGenerics.equals("boolean")
                || typeNameWithoutGenerics.equals("char")
                || typeNameWithoutGenerics.equals("byte")
                || typeNameWithoutGenerics.equals("short")
                || typeNameWithoutGenerics.equals("int")
                || typeNameWithoutGenerics.equals("long")
                || typeNameWithoutGenerics.equals("float")
                || typeNameWithoutGenerics.equals("double")) {
            // Primitive type
            line.add(destVarName + " = " + origVarName + ";");
            return line;
        } else if (typeNameWithoutGenerics.equals("java.lang.Boolean")
                || typeNameWithoutGenerics.equals("java.lang.Character")
                || typeNameWithoutGenerics.equals("java.lang.Byte")
                || typeNameWithoutGenerics.equals("java.lang.Short")
                || typeNameWithoutGenerics.equals("java.lang.Integer")
                || typeNameWithoutGenerics.equals("java.lang.Long")
                || typeNameWithoutGenerics.equals("java.lang.Float")
                || typeNameWithoutGenerics.equals("java.lang.Double")) {
            // Primitive wrapper class.
            line.add(destVarName + " = " + origVarName + ";");
            return line;
        } else if (typeNameWithoutGenerics.equals("java.lang.String")) {
            line.add(destVarName + " = " + origVarName + ";");
            return line;
        } else if (typeNameWithoutGenerics.equals("java.math.BigDecimal")) {
            line.add(destVarName + " = " + origVarName + ";");
            return line;
        } else if (typeNameWithoutGenerics.equals("java.util.Date")) {
            line.add(destVarName + " = (" + origVarName
                    + " == null ? null : new Date(" + origVarName
                    + ".getTime()));");
            return line;
        } else if (typeNameWithoutGenerics.equals("java.util.List")) {
            // TODO: Improves behavior in case <?>.
            String genericsType = "?";
            if (typeNameWithoutGenerics.equals(typeNameWithoutArray) == false) {
                genericsType = typeNameWithoutArray.substring(
                        typeNameWithoutGenerics.length() + 1,
                        typeNameWithoutArray.length() - 1);
            }

            line.add("if (" + origVarName + " != null) {");
            line.add("final java.util.Iterator<" + genericsType
                    + "> iterator = " + origVarName + ".iterator();");
            line.add("for (; iterator.hasNext();) {");
            line.add(genericsType + " loopSource = iterator.next();");
            line.add(genericsType + " loopTarget = null;");
            line.addAll(getCopyFieldLine("generics", genericsType,
                    "loopSource", "loopTarget"));
            line.add(destVarName + ".add(loopTarget);");
            line.add("}");
            line.add("}");
            return line;
        } else if (typeNameWithoutGenerics.equals("java.util.Map")) {
            // TODO: Improves behavior in case <?>.
            String genericsType = "?, ?";
            if (typeNameWithoutGenerics.equals(typeNameWithoutArray) == false) {
                genericsType = typeNameWithoutArray.substring(
                        typeNameWithoutGenerics.length() + 1,
                        typeNameWithoutArray.length() - 1);
            }

            final String genericsTargetKeyType = genericsType.substring(0,
                    genericsType.indexOf(",")).trim();
            final String genericsTargetValueType = genericsType.substring(
                    genericsType.indexOf(",") + 1).trim();

            line.add("if (" + origVarName + " != null) {");
            line.add("final java.util.Iterator<java.util.Map.Entry<"
                    + genericsType + ">> iterator = " + origVarName
                    + ".entrySet().iterator();");
            line.add("for (; iterator.hasNext();) {");
            line.add("java.util.Map.Entry<" + genericsType + ">"
                    + " loopSource = iterator.next();");
            line.add(genericsTargetKeyType + " loopKeyTarget = null;");
            line.add(genericsTargetValueType + " loopValueTarget = null;");
            line.addAll(getCopyFieldLine("generics", genericsTargetValueType,
                    "loopSource.getKey()", "loopKeyTarget"));
            line.addAll(getCopyFieldLine("generics", genericsTargetValueType,
                    "loopSource.getValue()", "loopValueTarget"));
            line.add(destVarName + ".put(loopKeyTarget, loopValueTarget);");
            line.add("}");
            line.add("}");
            return line;
        } else {
            // It will go here if it is an unsupported type.
            line.add("// Field[" + fieldName + "] is an unsupported type[" + fieldType
                    + "].");
            return line;
        }
    }
}
