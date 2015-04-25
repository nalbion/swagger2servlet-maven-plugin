<#macro compress_empty_lines><#local captured><#nested></#local>${ captured?replace("^\\s*[\\n\\r]+", "", "rm") }</#macro>
<#macro compress_single_line>
    <#local captured><#nested></#local>
${ captured?replace("^\\s+|\\s+$|\\n|\\r", "", "rm") }
</#macro>
package ${packageName};

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
@Api(value = "${basePath}"<#if description??>, description = "${description}"</#if>)
@SuppressWarnings("serial")
<#if produces??>
@Produces({<#list produces as procuct>"#{product}<#if procuct_has_next>, </#if></#list>})
</#if>
</#if>
public class ${className} extends AbstractServlet {
    Logger logger = LoggerFactory.getLogger(${className}.class);

    protected String handleGetRequest(HttpServletRequest request, HttpServletResponse response,
                                   String pathInfo, Map<String, Object> model)
        throws HttpResponseException, IOException
    {
        if (null == pathInfo) {
            return handleGetIndex(request, response, model);
<#list operations as operation>
    <#if operation.method == "GET">
        } else if ("${operation.path}".equals(pathInfo)) {
<@compress_empty_lines>
<#if operation.summary??>
            // ${operation.summary}
<#elseif operation.description??>
            // ${operation.description}
</#if>
</@compress_empty_lines>
            <#if operation.javaReturnType == "void">
            ${operation.javaMethodName}();
            <#else>
            return ${operation.javaMethodName}();
            </#if>
    </#if>
</#list>
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        return null;
    }

    protected String handleGetIndex(HttpServletRequest request, HttpServletResponse response, Map<String, Object> model)
        throws HttpResponseException, IOException
    {
    }
<#list operations as operation>

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
    @ApiOperation(<#if operation.summary??>value = "${operation.summary}"</#if>
            <#if operation.description??><#if operation.summary??>,</#if>
                notes = "${operation.description}"</#if>)
<#if operation.responses??>
    @ApiResponses(Array(
        <#list operation.responses as response>
        new ApiResponse(code = ${response.code},
                     message = "${response.description}"<#if response.class??>,
                    response = ${response.class}</#if>
        )<#if response_has_next>,</#if>
        </#list>
    ))
</#if>
<#if operation.hasImplicitParameters()>
    @ApiImplicitParams({
    <#list operation.parameters as param>
        <#if param.in != "path">
        @ApiImplicitParam(name = "${param.name}",
                <#if param.description??>value = "${param.description}",</#if>
                <#if param.default??>defaultValue = "${param.default}",</#if>
                <#if param.required??>required = ${param.required?c},</#if>
                <#if param.access??>access = "${param.access}",</#if>
                <#--<#if param.format??>format = "${param.format}", </#if>-->
                <#if param.type??>dataType = "${param.type}",</#if>
                paramType = "${param.in}")<#if param_has_next>,</#if>
        </#if>
    </#list>
    })
</#if>
</@compress_empty_lines>
</#if>
    public ${operation.javaReturnType} ${operation.javaMethodName}() {
    }
</#list>
}