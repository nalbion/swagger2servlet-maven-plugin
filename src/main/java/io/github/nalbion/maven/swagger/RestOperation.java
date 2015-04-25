package io.github.nalbion.maven.swagger;

import com.wordnik.swagger.models.Operation;
import com.wordnik.swagger.models.Response;
import com.wordnik.swagger.models.parameters.*;
import com.wordnik.swagger.models.properties.*;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestOperation {
    private String method;
    private String path;
    private String summary;
    private String description;
    private String javaMethodName;
    private String javaReturnType;
    private List<String> produces;
    private List<Map<String, String>> responses;
    private List<Map<String, Object>> parameters;
    private boolean hasImplicitParameters;

    /**
     * @param method - GET/POST etc
     * @param path
     * @param operation
     */
    public RestOperation(String method, String path, Operation operation) {
        this.method = method;
        this.path = path;
        description = fixMultiLineDescription(operation.getDescription());
        summary = fixMultiLineDescription(operation.getSummary());
        produces = operation.getProduces();
        javaMethodName = (String)operation.getVendorExtensions().get("x-swagger-js-method-name");
        if (null == javaMethodName) {
            javaMethodName = convertPathToMethodName(method, path);
        }

        Map<String, Response> rawResponses = operation.getResponses();
        if (rawResponses.size() == 0) {
            javaReturnType = "void";
        }
        this.responses = new ArrayList<>(rawResponses.size());
        boolean foundReturnTypeFor200 = false;
        for (String code : rawResponses.keySet()) {
            Response response = rawResponses.get(code);
            Map<String, String> data = new HashMap<>();



            data.put("code", code);
            data.put("description", response.getDescription());
            Property schema = response.getSchema();
            String returnType = parseResponseProperty(schema, data);



            if (null == javaReturnType) {
                if ("200".equals(code)) {
                    javaReturnType = returnType;
                    foundReturnTypeFor200 = true;
                } else if (!foundReturnTypeFor200 && code.charAt(0) == '2') {
                    javaReturnType = returnType;
                }
            }

            this.responses.add(data);
        }

        List<Parameter> rawParameters = operation.getParameters();
        if (null != rawParameters) {
            this.parameters = new ArrayList<>(rawParameters.size());
            for (Parameter parameter : rawParameters) {
                Map<String, Object> data = new HashMap<>(8);
                data.put("name", parameter.getName());
                String description = parameter.getDescription();
                if (null != description) {
                    data.put("description", fixMultiLineDescription(description));
                }
                data.put("required", parameter.getRequired());
                data.put("in", parameter.getIn());
                if (!"path".equals(parameter.getIn())) {
                    hasImplicitParameters = true;
                }
                data.put("access", parameter.getAccess());

                if (parameter instanceof CookieParameter) {
                    String value = ((CookieParameter)parameter).getDefaultValue();
                    if (null != value) {
                        data.put("default", value);
                    }
                } else if (parameter instanceof FormParameter) {
                    String value = ((FormParameter)parameter).getDefaultValue();
                    if (null != value) {
                        data.put("default", value);
                    }
                } else if (parameter instanceof HeaderParameter) {
                    String value = ((HeaderParameter)parameter).getDefaultValue();
                    if (null != value) {
                        data.put("default", value);
                    }
                } else if (parameter instanceof PathParameter) {
                    String value = ((PathParameter)parameter).getDefaultValue();
                    if (null != value) {
                        data.put("default", value);
                    }
                } else if (parameter instanceof QueryParameter) {
                    String value = ((QueryParameter)parameter).getDefaultValue();
                    if (null != value) {
                        data.put("default", value);
                    }
                }

                // Cookie, Form, Header, Path, Query
                if (parameter instanceof SerializableParameter) {
                    String value = ((SerializableParameter)parameter).getFormat();
                    if (null != value) {
                        data.put("format", value);
                    }

                    value = ((SerializableParameter)parameter).getType();
                    if (null != value) {
                        data.put("type", value);
                    }
                }

//                data.put("type", parameter.)
                this.parameters.add(data);
            }
        }

//        Map<String, Response> responses = operation.getResponses();
//        this.responses = new ArrayList<>(responses.size());
//
//        for (String code : responses.keySet()) {
//            Response response = responses.get(code);
////            response.getDescription();
////            response.getHeaders();
////            response.getSchema();
////            response.getExamples();
//
//            Map<String, String> responseData = new HashMap<>();
//            responseData.put("code", code);
//            responseData.put("message", response.getDescription());
//            Property schema = response.getSchema();
//            if (null != schema) {
//                responseData.put("response", schema.getType());
//            }
//            this.responses.add(responseData);
//        }
    }

    private String parseResponseProperty(Property schema, Map<String, String> data) {
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

    /**
     * @return "GET", "POST" etc
     */
    public String getMethod() {
        return method;
    }

    /**
     * @return The relative path
     */
    public String getPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }

    public String getSummary() {
        return summary;
    }

    public String getJavaMethodName() {
        return javaMethodName;
    }

    public String getJavaReturnType() {
        return javaReturnType;
    }

    public List<Map<String, String>> getResponses() {
        return responses;
    }

    public List<Map<String, Object>> getParameters() {
        return parameters;
    }

    public boolean hasImplicitParameters() {
        return hasImplicitParameters;
    }

    private String fixMultiLineDescription(String description) {
        if (null == description) { return null; }
        return description.trim()
                .replaceAll("\"", "\\\\\"")
                .replaceAll("\\\n",
                        "\" +\n" +
                                "                        \"");
    }

    /**
     * Adapted from https://github.com/wcandillon/swagger-js-codegen
     */
    private String convertPathToMethodName(String method, String path){
        if("/".equals(path) || StringUtils.isEmpty(path)) {
            return method.toLowerCase();
        } else {
            String[] segments = path.split("[/-]");
            StringBuilder segmentBuilder = new StringBuilder(method.toLowerCase());
            for (int i = 1; i < segments.length; i++) {
                String segment = segments[i];
                if (segment.charAt(0) == '{' && segment.charAt(segment.length() - 1) == '}') {
                    segmentBuilder.append("By")
                                .append(Character.toUpperCase(segment.charAt(1)))
                                .append(segment.substring(2, segment.length() - 1));
                } else {
                    segmentBuilder.append(StringUtils.capitalize(segment));
                }
            }
//            String result = ServletGeneratorMojo.camelCase(segmentBuilder.toString());
//            return method.toLowerCase() + result[0].toUpperCase() + result.substring(1);
            return segmentBuilder.toString();
        }
    };
}
