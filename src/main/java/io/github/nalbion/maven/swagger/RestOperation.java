package io.github.nalbion.maven.swagger;

import com.wordnik.swagger.models.Operation;
import com.wordnik.swagger.models.Response;
import com.wordnik.swagger.models.parameters.*;
import com.wordnik.swagger.models.properties.*;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestOperation {
    private String method;
    private String path;
    private String pathPattern;
    private String[] pathParameters;
    private String summary;
    private String description;
    private String javaMethodName;
    private String javaReturnType;
    private List<String> produces;
    private List<Map<String, String>> responses;
    private Map<String, Map<String, Object>> parameters;
    private boolean hasImplicitParameters;

    private static final Pattern PATH_PATTERN_REGEX = Pattern.compile("\\{(\\w+)\\}");

    /**
     * @param method - GET/POST etc
     * @param path
     * @param operation
     */
    public RestOperation(String method, String path, Operation operation) {
        this.method = method;
        description = operation.getDescription();
        summary = operation.getSummary();
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
            String returnType = Utils.parseResponseProperty(schema, data);

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
            this.parameters = new HashMap<>(rawParameters.size());
            for (Parameter parameter : rawParameters) {
                Map<String, Object> data = new HashMap<>(8);

                String javaType = "String";
                String paramName = parameter.getName();
                data.put("name", paramName);
                String description = parameter.getDescription();
                if (null != description) {
                    data.put("description", description);
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
                    String format = ((SerializableParameter)parameter).getFormat();
                    if (null != format) {
                        data.put("format", format);
                    }

                    String type = ((SerializableParameter)parameter).getType();
                    if (null != type) {
                        data.put("type", type);

                        if ("integer".equals(type)) {
                            if ("int64".equals(format)) {
                                javaType = "long";
                            } else {
                                javaType = "int";
                            }
                        } else if ("number".equals(type)) {
                            javaType = format;
                        } else if ("boolean".equals(type)) {
                            javaType = type;
                        } else if ("byte".equals(format)) {
                            javaType = format;
                        } else if ("date".equals(format) || "date-time".equals(format)) {
                            javaType = "java.util.Date";
                        }
                    }
                }

                data.put("javaType", javaType);
                this.parameters.put(paramName, data);
            }
        }

        setPath(path);

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

    /**
     * Updates <code>pathPattern</code> and <code>pathParameters</code> if parameters are
     * embedded within the path
     * @param path
     */
    private void setPath(String path) {
        this.path = path;

        Matcher matcher = PATH_PATTERN_REGEX.matcher(path);
        StringBuilder str = new StringBuilder(path.length());
        LinkedList<String> params = new LinkedList<>();
        int start = 0;
        while (matcher.find()) {
            int end = matcher.start();
            str.append(path.substring(start, end));
            start = matcher.end();
            String paramName = matcher.group(1);
            params.add(paramName);

            Map<String, Object> paramData = parameters.get(paramName);
            // type: integer, number, string, boolean
            String type = (String)paramData.get("type");
            if ("integer".equals(type)) {
                str.append("(\\\\d+)");
            } else if ("number".equals(type)) {
                str.append("(\\\\d+\\\\.\\\\d+)");
            } else if ("boolean".equals(type)) {
                str.append("(true|false)");
            } else {
                String format = (String)paramData.get("format");
                if ("byte".equals(format)) {
                    str.append("(\\\\d{1,3})");
                } else if ("date".equals(format)) {
                    str.append("(\\\\d{4}-[01]\\\\d-[013]\\\\d)");
                } else if ("date-time".equals(format)) {
                    str.append("(\\\\d{4}-[01]\\\\d-[013]\\\\d" +
                            "T" +
                            "[012]\\\\d:[0-6]\\\\d:[0-6]\\\\d");
                } else {
                    str.append("([^/]+)");
                }
            }
        }
        if (start > 0) {
            str.append(path.substring(start));
            this.pathParameters = params.toArray(new String[params.size()]);
            this.pathPattern = str.toString();
        }
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

    public Map<String, Map<String, Object>> getParameters() {
        return parameters;
    }

    public boolean hasImplicitParameters() {
        return hasImplicitParameters;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public String[] getPathParameters() {
        return pathParameters;
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
    }
}
