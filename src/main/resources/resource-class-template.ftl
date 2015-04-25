<#macro multiline_javadoc>
    <#local captured><#nested></#local>
     ${ captured?trim?replace("[\\\r\\\n]+", "\\\n     * ", "rm") }
</#macro>
package ${packageName};

<#list requiredImports as import>
import ${import};
</#list>

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ${className} {
    private static final Logger logger = LoggerFactory.getLogger(${className}.class);

<#list fields as field>
    private ${field.javaType} ${field.name};
</#list>

<#list fields as field>

    /**
<#if field.description??>
    <@multiline_javadoc>
     * ${field.description}
    </@multiline_javadoc>
</#if>
     */
    public ${field.javaType} get${field.name?capitalize}() {
        return ${field.name};
    }
    public void set${field.name?capitalize}(final ${field.javaType} ${field.name}) {
        this.${field.name} = ${field.name};
    }
</#list>
}
