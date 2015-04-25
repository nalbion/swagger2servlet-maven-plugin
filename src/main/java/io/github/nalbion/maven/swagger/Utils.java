package io.github.nalbion.maven.swagger;

import com.wordnik.swagger.models.properties.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import java.util.Map;

public class Utils {

    static String camelCase(String original) {
        String converted = original.replaceAll("[^\\w\\d]", " ");
        return WordUtils.capitalize(converted).replaceAll(" ", "");
    }

    static String parseResponseProperty(Property schema, Map<String, String> data) {
        String returnType;
        boolean isPrimitiveReturn = false;

        if (null == schema) {
            returnType = "void";
            isPrimitiveReturn = true;
        } else {
            if (schema instanceof StringProperty) {
                returnType = "String";
                if (null != data) data.put("type", "string");
            } else if (schema instanceof UUIDProperty) {
                returnType = "UUID";
                if (null != data) data.put("type", "string");
            } else if (schema instanceof BooleanProperty) {
                returnType = "boolean";
                isPrimitiveReturn = true;
                if (null != data) data.put("type", "boolean");
            } else if (schema instanceof DateProperty) {
                returnType = "Date";
                if (null != data) {
                    data.put("type", "string");
                    data.put("format", "date");
                }
            } else if (schema instanceof DateTimeProperty) {
                returnType = "Date";
                if (null != data) {
                    data.put("type", "string");
                    data.put("format", "date-time");
                }
            } else if (schema instanceof DecimalProperty) {
                returnType = "BigDecimal";
                if (null != data) data.put("type", "number");
            } else if (schema instanceof DoubleProperty) {
                returnType = "double";
                isPrimitiveReturn = true;
                if (null != data) {
                    data.put("type", "number");
                    data.put("format", "double");
                }
            } else if (schema instanceof FloatProperty) {
                returnType = "float";
                isPrimitiveReturn = true;
                if (null != data) {
                    data.put("type", "number");
                   data.put("format", "float");
                }
            } else if (schema instanceof IntegerProperty) {
                returnType = "int";
                isPrimitiveReturn = true;
                if (null != data) {
                    data.put("type", "number");
                    data.put("format", "int32");
                }
            } else if (schema instanceof LongProperty) {
                returnType = "long";
                isPrimitiveReturn = true;
                if (null != data) {
                    data.put("type", "number");
                    data.put("format", "int64");
                }
            } else if (schema instanceof ArrayProperty) {
                // List<MyCustomClass>.class, responseContainer = "List"
                Property items = ((ArrayProperty) schema).getItems();
                returnType = "List<" + parseResponseProperty(items, null) + ">";
                if (null != data) {
                    data.put("container", "List");
                    data.put("type", "array");
                }
//                } else if (schema instanceof MapProperty) {
//                    // List<MyCustomClass>.class, responseContainer = "List"
//                    String type = ((MapProperty) schema).getAdditionalProperties().getType();
//                    data.put("class", "Map<" + StringUtils.capitalize(type) + ">.class");
//                    data.put("container", "Map");
//                    data.put("type", "object");
            } else if (schema instanceof ObjectProperty) {
                String type = schema.getType();
                returnType = StringUtils.capitalize(type);
//                } else if (schema instanceof FileProperty) {
            } else if (schema instanceof RefProperty) {
                returnType = ((RefProperty)schema).getSimpleRef();
            } else {
                String type = schema.getType();
                returnType = StringUtils.capitalize(type);
            }
        }

        if (null != data) {
            data.put("class", isPrimitiveReturn ? returnType : (returnType + ".class"));
        }
        return returnType;
    }
}
