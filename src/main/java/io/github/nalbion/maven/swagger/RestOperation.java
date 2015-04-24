package io.github.nalbion.maven.swagger;

import com.wordnik.swagger.models.Operation;
import com.wordnik.swagger.models.Response;
import com.wordnik.swagger.models.parameters.*;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestOperation {
    private String method;
    private String path;
    private String description;
    private String javaMethodName;
//    List<Map<String, String>> responses;
    private Map<String, Response> responses;
    private List<Map<String, Object>> parameters;

    /**
     * @param method - GET/POST etc
     * @param path
     * @param operation
     */
    public RestOperation(String method, String path, Operation operation) {
        this.method = method;
        this.path = path;
        description = operation.getDescription();
        javaMethodName = (String)operation.getVendorExtensions().get("x-swagger-js-method-name");
        if (null == javaMethodName) {
            javaMethodName = getPathToMethodName(method, path);
        }

        this.responses = operation.getResponses();

        List<Parameter> parameters = operation.getParameters();
        if (null != parameters) {
            this.parameters = new ArrayList<>(parameters.size());
            for (Parameter parameter : parameters) {
                Map<String, Object> data = new HashMap<>();
                data.put("name", parameter.getName());
                String description = parameter.getDescription();
                if (null != description) {
                    data.put("description",
                              description.trim()
                                            .replaceAll("\"", "\\\\\"")
                                            .replaceAll("\\\n",
                                                    "\" +\n" +
                                                            "                        \""));
                }
                data.put("required", parameter.getRequired());
                data.put("in", parameter.getIn());
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

    public String getJavaMethodName() {
        return javaMethodName;
    }

//    public List<Map<String, String>> getResponses() {
    public Map<String, Response> getResponses() {
        return responses;
    }

    public List<Map<String, Object>> getParameters() {
        return parameters;
    }

    /**
     * Adapted from https://github.com/wcandillon/swagger-js-codegen
     */
    private String getPathToMethodName(String method, String path){
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
