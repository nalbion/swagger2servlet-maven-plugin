package ${packageName};

import com.wordnik.swagger.annotations.*;

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
@Api(value = "${basePath}"<#if description??>, description = "${description}"</#if>)
@SuppressWarnings("serial")
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
<#if operation.description??>    // ${operation.description}</#if>
            // TODO: implement
    </#if>
</#list>
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
    }

    protected String handleGetIndex(HttpServletRequest request, HttpServletResponse response, Map<String, Object> model)
        throws HttpResponseException, IOException
    {
    }
<#list operations as operation>

    /**
     * ${operation.description}
     * ${operation.method}  ${operation.path}
     */
<#if operation.responses??>
    <#assign keys = operation.responses?keys>
    @ApiResponses(Array(
        <#list keys as key>
        new ApiResponse(code = ${key},
                     message = "${operation.responses[key].getDescription()}"<#if operation.responses[key].getSchema()??>,
                    response = ${operation.responses[key].getSchema().getType()?cap_first}.class</#if>
        )<#if key_has_next>,</#if>
        </#list>
    ))
</#if>
<#if operation.parameters??>
    @ApiImplicitParams({
    <#list operation.parameters as param>
        @ApiImplicitParam(name = "${param.name}",
                <#if param.description??>value = "${param.description}",</#if>
                <#if param.default??>defaultValue = "${param.default}",</#if>
                <#if param.required??>required = ${param.required?c},</#if>
                <#if param.access??>access = "${param.access}",</#if>
                <#--<#if param.format??>format = "${param.format}", </#if>-->
                <#if param.type??>dataType = "${param.type}",</#if>
                paramType = "${param.in}")<#if param_has_next>,</#if>
    </#list>
    })
</#if>
    public void ${operation.javaMethodName}() {
    }
</#list>
}