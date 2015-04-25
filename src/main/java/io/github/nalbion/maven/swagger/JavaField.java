package io.github.nalbion.maven.swagger;

import com.wordnik.swagger.models.properties.Property;


public class    JavaField {
    private String name;
    private String javaType;
    private String description;

    public JavaField(String name, String javaType, Property property) {
        this.name = name;
        this.javaType = javaType;
        description = property.getDescription();

//        System.out.println("  type: " + property.getType());
//        System.out.println("  reqd: " + property.getRequired());
    }

    public String getName() {
        return name;
    }

    public String getJavaType() {
        return javaType;
    }

    public String getDescription() {
        return description;
    }
}
