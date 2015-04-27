<#macro compress_empty_lines><#local captured><#nested></#local>${ captured?replace("^\\s*[\\n\\r]+", "", "rm") }</#macro>
<#macro compress_single_line>
    <#local captured><#nested></#local>
${ captured?replace("^\\s+|\\s+$|\\n|\\r", "", "rm") }
</#macro>
<#macro join_single_line><#local captured><#nested></#local>${ captured?replace("\\n|\\r", "", "rm") }</#macro>
<#macro multiline_string>
    <#local captured><#nested></#local>
${ captured?trim?replace("\"", "\\\\\"", "rm")
                ?replace("\\\n",
                        "\" +\n" +
                        "                        \"", "rm") }
</#macro>
<#macro multiline_comment>
    <#local captured><#nested></#local>
${ captured?trim?replace("\\\n",
                        "\\\n        //", "rm") }
</#macro>
<#macro parse_parameter javaType>
<#if javaType == "String"><#nested>
<#elseif javaType == "double">Double.parseDouble(<#nested>)
<#elseif javaType == "float">Float.parseFloat(<#nested>)
<#elseif param.javaType == "boolean">Boolean.parseBoolean(<#nested>)
<#elseif param.javaType == "int">Integer.parseInt(<#nested>)
<#elseif param.javaType == "long">Long.parseLong(<#nested>)
<#elseif param.javaType == "java.util.Date">
<#if param.format == "date">DATE_FORMAT.parse(<#nested>)
<#else>DATE_TIME_FORMAT.parse(<#nested>)
</#if>
</#if>
</#macro>
package ${packageName};

import ${modelPackageName}.*;

<#if generateSwaggerAnnotations>
import com.wordnik.swagger.annotations.*;
</#if>

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles all requests for ${basePath}:
 * <ul>
<#list operations as operation>
 *   <dt>${operation.method}  ${operation.path}</dt>
<#if operation.description??> *       <dd>${operation.description}</dd></#if>
</#list>
 * </ul>
 */
<#if generateSwaggerAnnotations>
@Path("${basePath}")
@Api(value = "${basePath}"<#if description??>, description = "<@multiline_string>${description}</@multiline_string>"</#if>)
@SuppressWarnings("serial")
<#if produces??>
@Produces({<#list produces as procuct>"#{product}<#if procuct_has_next>, </#if></#list>})
</#if>
</#if>
public class ${className} extends AbstractServlet {
    Logger logger = LoggerFactory.getLogger(${className}.class);

<#list operations as operation>
    <#if operation.pathPattern??>
    private static final java.util.regex.Pattern ${operation.javaMethodName?replace("[A-Z]", "_$0", "r")?upper_case}_PATTERN
                            = java.util.regex.Pattern.compile("${operation.pathPattern}");
    </#if>
</#list>

    protected String handleGetRequest(HttpServletRequest request, HttpServletResponse response,
                                   String pathInfo, Map<String, Object> model)
        throws HttpResponseException, IOException
    {
        if (null == pathInfo) {
            return handleGetIndex(request, response, model);
        }
<#if hasPathPatterns>

        java.util.regex.Matcher matcher;
</#if>
<#list operations as operation>
    <#if operation.method == "GET">

        <@compress_empty_lines>
            <#if operation.summary??>
        // <@multiline_comment>${operation.summary}</@multiline_comment>
            <#elseif operation.description??>
        // <@multiline_comment>${operation.description}</@multiline_comment>
            </#if>
        </@compress_empty_lines>
        <#if operation.pathPattern??>
        // ${operation.pathPattern}
        matcher = ${operation.javaMethodName?replace("[A-Z]", "_$0", "r")?upper_case}_PATTERN.matcher(pathInfo);
        if (matcher.find()) {
            <#list operation.pathParameters as paramName>
                <#assign param = operation.parameters[paramName]>
            ${param.javaType} ${param.name} = <#rt>
                <@parse_parameter param.javaType>matcher.group(${paramName_index + 1})</@parse_parameter>;<#lt>
            </#list>
        <#else>
        if ("${operation.path}".equals(pathInfo)) {
        </#if>
        <#if operation.javaReturnType == "void">
            ${operation.javaMethodName}(request, response<#if operation.pathPattern??>,
                                                <#list operation.pathParameters as param>${param}<#if param_has_next>, </#if></#list></#if>);
            return null;
        <#else>
            ${operation.javaReturnType} responseData = ${operation.javaMethodName}(request, response<#if operation.pathPattern??>,
                                                <#list operation.pathParameters as param>${param}<#if param_has_next>, </#if></#list></#if>);
            if (null != responseData) {
                renderJsonResponse(response, responseData);
                return null;
            }
        </#if>
        }
    </#if>
</#list>

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return null;
    }

    protected String handlePostRequest(HttpServletRequest request, HttpServletResponse response,
                                        String pathInfo)
        throws HttpResponseException, IOException
    {
<#if hasPathPatterns>
        java.util.regex.Matcher matcher;
</#if>
<#list operations as operation>
    <#if operation.method == "POST">

        <@compress_empty_lines>
            <#if operation.summary??>
        // <@multiline_comment>${operation.summary}</@multiline_comment>
            <#elseif operation.description??>
        // <@multiline_comment>${operation.description}</@multiline_comment>
            </#if>
        </@compress_empty_lines>
        <#if operation.pathPattern??>
        // ${operation.pathPattern}
        matcher = ${operation.javaMethodName?replace("[A-Z]", "_$0", "r")?upper_case}_PATTERN.matcher(pathInfo);
        if (matcher.find()) {
            <#list operation.pathParameters as paramName>
                <#assign param = operation.parameters[paramName]>
            ${param.javaType} ${param.name} = <#rt>
                <@parse_parameter param.javaType>matcher.group(${paramName_index + 1})</@parse_parameter>;<#lt>
            </#list>
        <#else>
        if ("${operation.path}".equals(pathInfo)) {
        </#if>
        <#if operation.javaReturnType == "void">
            ${operation.javaMethodName}(request, response<#if operation.pathPattern??>,
                                <#list operation.pathParameters as param>${param}<#if param_has_next>, </#if></#list></#if>);
            return null;
        <#else>
            ${operation.javaReturnType} responseData = ${operation.javaMethodName}(request, response<#if operation.pathPattern??>,
                                <#list operation.pathParameters as param>${param}<#if param_has_next>, </#if></#list></#if>);
            renderJsonResponse(response, responseData);
            return null;
        </#if>
        }
    </#if>
</#list>

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return null;
    }

    protected String handlePutRequest(HttpServletRequest request, HttpServletResponse response,
                                        String pathInfo)
        throws HttpResponseException, IOException
    {
<#if hasPathPatterns>
        java.util.regex.Matcher matcher;
</#if>
<#list operations as operation>
    <#if operation.method == "PUT">

        <@compress_empty_lines>
            <#if operation.summary??>
        // <@multiline_comment>${operation.summary}</@multiline_comment>
            <#elseif operation.description??>
        // <@multiline_comment>${operation.description}</@multiline_comment>
            </#if>
        </@compress_empty_lines>
        <#if operation.pathPattern??>
        // ${operation.pathPattern}
        matcher = ${operation.javaMethodName?replace("[A-Z]", "_$0", "r")?upper_case}_PATTERN.matcher(pathInfo);
        if (matcher.find()) {
            <#list operation.pathParameters as paramName>
                <#assign param = operation.parameters[paramName]>
            ${param.javaType} ${param.name} = <#rt>
                <@parse_parameter param.javaType>matcher.group(${paramName_index + 1})</@parse_parameter>;<#lt>
            </#list>
        <#else>
        if ("${operation.path}".equals(pathInfo)) {
        </#if>
        <#if operation.javaReturnType == "void">
            ${operation.javaMethodName}(request, response<#if operation.pathPattern??>,
                                <#list operation.pathParameters as param>${param}<#if param_has_next>, </#if></#list></#if>);
            return null;
        <#else>
        ${operation.javaReturnType} responseData = ${operation.javaMethodName}(request, response<#if operation.pathPattern??>,
                                <#list operation.pathParameters as param>${param}<#if param_has_next>, </#if></#list></#if>);
            if (null != responseData) {
                renderJsonResponse(response, responseData);
                return null;
            }
        </#if>
        }
    </#if>
</#list>

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return null;
    }

    protected String handleDeleteRequest(HttpServletRequest request, HttpServletResponse response,
                                        String pathInfo)
        throws HttpResponseException, IOException
    {
<#if hasPathPatterns>
        java.util.regex.Matcher matcher;
</#if>
<#list operations as operation>
    <#if operation.method == "DELETE">

        <@compress_empty_lines>
            <#if operation.summary??>
        // <@multiline_comment>${operation.summary}</@multiline_comment>
           <#elseif operation.description??>
        // <@multiline_comment>${operation.description}</@multiline_comment>
            </#if>
        </@compress_empty_lines>
        <#if operation.pathPattern??>
        // ${operation.pathPattern}
        matcher = ${operation.javaMethodName?replace("[A-Z]", "_$0", "r")?upper_case}_PATTERN.matcher(pathInfo);
        if (matcher.find()) {
            <#list operation.pathParameters as paramName>
                <#assign param = operation.parameters[paramName]>
            ${param.javaType} ${param.name} = <#rt>
                <@parse_parameter param.javaType>matcher.group(${paramName_index + 1})</@parse_parameter>;<#lt>
            </#list>
        <#else>
        if ("${operation.path}".equals(pathInfo)) {
        </#if>
        <#if operation.javaReturnType == "void">
            ${operation.javaMethodName}(request, response<#if operation.pathPattern??>,
                                    <#list operation.pathParameters as param>${param}<#if param_has_next>, </#if></#list></#if>);
            return null;
        <#else>
            ${operation.javaReturnType} responseData = ${operation.javaMethodName}(request, response<#if operation.pathPattern??>,
                                    <#list operation.pathParameters as param>${param}<#if param_has_next>, </#if></#list></#if>);

            if (null != responseData) {
                renderJsonResponse(response, responseData);
                return null;
            }
        </#if>
        }
    </#if>
</#list>

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return null;
    }

    protected String handleGetIndex(HttpServletRequest request, HttpServletResponse response, Map<String, Object> model)
        throws HttpResponseException, IOException
    {
        return null;
    }
<#list operations as operation>
    <#if operation.parameters??>
        <#assign paramNames = operation.parameters?keys>
    </#if>

    /**
<#if operation.summary??>
     * ${operation.summary}
</#if>
<#if operation.description??>
     * ${operation.description}
</#if>
     * ${operation.method}  ${operation.path}
     */
<#if generateSwaggerAnnotations>
<@compress_empty_lines>
    @${operation.method}
    @Path("${operation.path}")
    @ApiOperation(<#if operation.summary??>value = "<@multiline_string>${operation.summary}</@multiline_string>"</#if>
            <#if operation.description??><#if operation.summary??>,</#if>
                notes = "<@multiline_string>${operation.description}</@multiline_string>"</#if>)
<#if operation.responses??>
    @ApiResponses(Array(
        <#list operation.responses as response>
        new ApiResponse(code = ${response.code},
                     message = "<@multiline_string>${response.description}</@multiline_string>"<#if response.class??>,
                    response = ${response.class}</#if>
        )<#if response_has_next>,</#if>
        </#list>
    ))
</#if>
<#if operation.hasImplicitParameters()>
    @ApiImplicitParams({
    <#list paramNames as name>
        <#assign param = operation.parameters[name]>
        <#if param.in != "path">
        @ApiImplicitParam(name = "${param.name}",
                <#if param.description??>value = "${param.description}",</#if>
                <#if param.default??>defaultValue = "${param.default}",</#if>
                <#if param.required??>required = ${param.required?c},</#if>
                <#if param.access??>access = "${param.access}",</#if>
                <#--<#if param.format??>format = "${param.format}", </#if>-->
                <#if param.type??>dataType = "${param.type}",</#if>
                paramType = "${param.in}")<#if name_has_next>,</#if>
        </#if>
    </#list>
    })
</#if>
</@compress_empty_lines>
</#if>
    protected ${operation.javaReturnType} ${operation.javaMethodName}(HttpServletRequest request, HttpServletResponse response<#if operation.pathParameters??>,
                <@join_single_line>
                <#list operation.pathParameters as paramName>
                    <#assign param = operation.parameters[paramName]>
                    final ${param.javaType} ${paramName}<#if paramName_has_next>, </#if>
                </#list>
                </@join_single_line></#if><#lt>) {
<#if operation.parameters??>
        String paramValueString;
    <#assign paramNames = operation.parameters?keys>
    <#list paramNames as paramName>
        <#assign param = operation.parameters[paramName]>
        <#if param.in != 'path'>
            <#if param.in == 'header'>
        paramValueString = request.getHeader("${paramName}");
            <#else>
        paramValueString = request.getParameter("${paramName}");
            </#if>
            <#if param.javaType == 'String'>
        ${param.javaType} ${paramName?replace("-", "_")} = paramValueString;
            <#else>
        ${param.javaType} ${paramName?replace("-", "_")} = (null == paramValueString) ? <@compress_single_line>
                <#if operation.default??>
                    <#if param.javaType == 'string'>
                        "${param.default}"
                    <#else>
                        ${param.default}
                    </#if>
                <#else>
                    <#if param.javaType == 'boolean'>false
                    <#elseif param.javaType?matches('^[a-z][^\\.]*$')>0
                    <#else>null
                    </#if>
                </#if> : <@parse_parameter param.javaType>paramValueString</@parse_parameter>;
                </@compress_single_line>
            </#if>
        </#if>
    </#list>
</#if>
<#if operation.parameters?? && (!operation.pathParameters?? || operation.parameters?size != operation.pathParameters?size) >

        <#if operation.javaReturnType != 'void'>return </#if>${operation.javaMethodName}(request, response,
    <#assign paramNames = operation.parameters?keys>
    <@join_single_line>
        <#list paramNames as paramName>
            <#assign param = operation.parameters[paramName]>
            ${paramName?replace("-", "_")}<#if paramName_has_next>, </#if>
        </#list>
    </@join_single_line><#lt>);
    }

    /**
<#list paramNames as paramName>
    <#assign param = operation.parameters[paramName]>
     * @param ${paramName?replace("-", "_")} <#if param.description??>${param.description}</#if>
</#list>
     */
    protected ${operation.javaReturnType} ${operation.javaMethodName}(HttpServletRequest request, HttpServletResponse response,
    <@join_single_line>
        <#list paramNames as paramName>
            <#assign param = operation.parameters[paramName]>
            final ${param.javaType} ${paramName?replace("-", "_")}<#if paramName_has_next>, </#if><#t></#list>
    </@join_single_line><#lt>) {
</#if>
    <#if operation.javaReturnType != 'void'>
        <#if operation.javaReturnType == 'boolean'>
        return false
        <#elseif operation.javaReturnType?matches('^[a-z][^\\.]*$')>
        return 0;
        <#else>
        return null;
        </#if>
    </#if>
    }
</#list>
}