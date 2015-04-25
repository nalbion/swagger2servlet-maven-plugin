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
                <#if param.javaType == "String">
                    matcher.group(${paramName_index + 1});<#lt>
                <#elseif param.javaType == "double">
                    Double.parseDouble(matcher.group(${paramName_index + 1}));<#lt>
                <#elseif param.javaType == "float">
                    Float.parseFloat(matcher.group(${paramName_index + 1}));<#lt>
                <#elseif param.javaType == "boolean">
                    Boolean.parseBoolean(matcher.group(${paramName_index + 1}));<#lt>
                <#elseif param.javaType == "int">
                    Integer.parseInt(matcher.group(${paramName_index + 1}));<#lt>
                <#elseif param.javaType == "long">
                    Long.parseLong(matcher.group(${paramName_index + 1}));<#lt>
                <#elseif param.javaType == "java.util.Date">
                    <#if param.format == "date">
                    DATE_FORMAT.parse(matcher.group(${paramName_index + 1}));<#lt>
                    <#else>
                    DATE_TIME_FORMAT.parse(matcher.group(${paramName_index + 1}));<#lt>
                    </#if>
                </#if>
            </#list>
        <#else>
        if ("${operation.path}".equals(pathInfo)) {
        </#if>
        <#if operation.javaReturnType == "void">
            ${operation.javaMethodName}(<#if operation.pathPattern??><#list operation.pathParameters as param>${param}<#if param_has_next>, </#if></#list></#if>);
        <#else>
            ${operation.javaReturnType} responseData = ${operation.javaMethodName}(<#if operation.pathPattern??><#list operation.pathParameters as param>${param}<#if param_has_next>, </#if></#list></#if>);
        </#if>
            return null;
        }
    </#if>
</#list>

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return null;
    }

    protected String handleGetIndex(HttpServletRequest request, HttpServletResponse response, Map<String, Object> model)
        throws HttpResponseException, IOException
    {
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
    public ${operation.javaReturnType} ${operation.javaMethodName}(<#compress><@join_single_line>
    <#if operation.pathParameters??>
        <#list operation.pathParameters as paramName>
            <#assign param = operation.parameters[paramName]>
            final ${param.javaType} ${paramName}<#if paramName_has_next>, </#if>
        </#list>
    </#if>
    </@join_single_line></#compress>) {
    }
</#list>
}