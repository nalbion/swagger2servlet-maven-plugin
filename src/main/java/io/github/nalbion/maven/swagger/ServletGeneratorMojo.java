package io.github.nalbion.maven.swagger;

import com.wordnik.swagger.models.*;
import com.wordnik.swagger.models.properties.Property;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import io.swagger.parser.SwaggerParser;
import org.apache.commons.lang.WordUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import java.io.*;
import java.nio.file.*;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Generate a servlet(s) from a Swagger spec
 */
@Mojo(name="generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ServletGeneratorMojo extends AbstractMojo {

    /**
     * Can be a local file or a HTTP URL.
     * Can be a JSON or YAML file.
     */
    @Parameter(defaultValue = "src/main/webapp/swagger.json")
    private String schemaLocation;

    /**
     * Can be used as an alternative to <code>schemaLocation</code>
     * to specify a set of inclusions/exclusions on the local file system
     */
    @Parameter
    private FileSet schemaFiles;

    /**
     * Name of the package to which the servlet(s) will belong
     */
    @Parameter(defaultValue = "${project.groupId}.api")
    private String packageName;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/swagger2servlet")
    private File outputDirectory;

    @Parameter(defaultValue = "true")
    private boolean generateSwaggerAnnotations;

    /**
     * (Optional) If specified, must contain files:
     * <ul>
     *     <li>base-class-template.ftl</li>
     *     <li>class-template.ftl</li>
     * </ul>
     */
    @Parameter
    private File templateDirectory;

    @Parameter(readonly = true, defaultValue = "${project.build.sourceEncoding}")
    private String sourceEncoding;

    public void execute() throws MojoExecutionException {
        if (sourceEncoding == null) {
            sourceEncoding = "UTF-8";
        }

        if (schemaFiles != null) {
            int index = 0;
            FileSetManager fileSetManager = new FileSetManager();
            String[] includedSchemaFiles = fileSetManager.getIncludedFiles(schemaFiles);
System.out.println("included schema files:" + includedSchemaFiles);
            for (String schemaFile : includedSchemaFiles) {
                try {
                     processSchema(schemaFile, index++);
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to parse " + schemaFile, e);
                } catch (TemplateException e) {
                    throw new MojoExecutionException("Failed to process template", e);
                }
            }
        } else {
            try {
                Path path = Paths.get(schemaLocation);
                if (Files.isDirectory(path)) {
                    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                        int index = 0;

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            try {
                                 processSchema(file.toString(), index++);
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to parse " + file.toString(), e);
                            } catch (TemplateException e) {
                                throw new RuntimeException("Failed to process template", e);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } else {
                     processSchema(schemaLocation, 0);
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to parse schema", e);
            } catch (TemplateException e) {
                throw new MojoExecutionException("Failed to process template", e);
            }
        }
    }

    private void processSchema(String schemaPathName, int index) throws IOException, TemplateException {
        System.out.println("Processing schema: " + schemaPathName);
        // Skip the schema if there's no code to generate
        Swagger swagger = loadSwagger(schemaPathName);
        if (swagger == null) {
            System.out.println("No swagger - skipping " + schemaPathName);
            return;
        }

        Map<String, com.wordnik.swagger.models.Path> paths = swagger.getPaths();
        if (paths == null) {
            System.out.println("No paths - skipping " + schemaPathName);
            return;
        }

        // Load data for the templates
        File schemaFile = new File(schemaPathName);
        String className = determineApiClassName(swagger, schemaFile);

        Map<String, Object> data = new HashMap<>();
        data.put("generateSwaggerAnnotations", generateSwaggerAnnotations);
        data.put("packageName", packageName);
        data.put("className", className);
        data.put("basePath", swagger.getBasePath());

        Map<String, Model> definitions = swagger.getDefinitions();
        String modelPackageName = this.packageName + ".model";
        data.put("modelPackageName", modelPackageName);

        // Create model/entity classes for the schema's definitions
        Configuration templateConfig = loadConfiguration();
        processDefinitions(definitions, modelPackageName, templateConfig);

        // Parse the rest operations for the API
        processRestOperations(paths, data);

        // Generate the API servlet code
        processTemplate(templateConfig, "api-servlet-template.ftl", data, packageName, className);
        if (0 == index) {
            processTemplate(templateConfig, "abstract-servlet-template.ftl", data, packageName, "AbstractServlet");
        }
    }

    private Swagger loadSwagger(String schemaPathName) {
//        // build an authorization value
//        AuthorizationValue mySpecialHeader = new AuthorizationValue()
//                .keyName("x-special-access")  //  the name of the authorization to pass
//                .value("i-am-special")        //  the value of the authorization
//                .type("header");              //  the location, as either `header` or `query`

//        // or in a single constructor
//        AuthorizationValue apiKey = new AuthorizationValue("api_key", "special-key", "header");
//        Swagger swagger = new SwaggerParser().read(schemaLocation,
//                                    Arrays.asList(mySpecialHeader, apiKey)
//        );

        return new SwaggerParser().read(schemaPathName);
    }

    private String determineApiClassName(Swagger swagger, File schemaFile) {
        Info info = swagger.getInfo();
        String title = info == null ? null : info.getTitle();
        if (title == null) {
            title = schemaFile.getName().replaceAll("(api|API)?\\.(yaml|json)$", "");
        }
        return Utils.camelCase(title) + "Api";
    }

    private void processDefinitions(Map<String, Model> definitions,
                                    String packageName,
                                    Configuration templateConfig) throws IOException, TemplateException {
        Map<String, Object> classData = new HashMap<>();
        List<JavaField> fields = new LinkedList<>();
        Set<String> requiredImports = new HashSet<>();

        for (String definitionName : definitions.keySet()) {
            classData.clear();
            classData.put("packageName", packageName);
            classData.put("className", definitionName);

            fields.clear();
            classData.put("fields", fields);
            requiredImports.clear();
            classData.put("requiredImports", requiredImports);

            Model model = definitions.get(definitionName);

            Map<String, Property> properties = model.getProperties();
            if (null != properties) {
                for (String fieldName : properties.keySet()) {
                    Property property = properties.get(fieldName);
                    String javaType = Utils.parseResponseProperty(property, null);
                    fields.add(new JavaField(fieldName, javaType, property));
                }
            }

            processTemplate(templateConfig, "entity-class-template.ftl",
                            classData, packageName, definitionName);
        }
    }

    private void processTemplate(Configuration cfg, String templateName,
                                 Map<String, Object> data,
                                 String packageName, String className)
            throws IOException, TemplateException {
        File outputFile = new File(outputDirectory,
                packageName.replaceAll("\\.", "/") + "/" + className + ".java");
        if (!outputFile.exists()) {
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
        }
        Writer out = new FileWriter(outputFile);

        Template template = cfg.getTemplate(templateName);
        template.process(data, out);
        out.close();
        System.out.println("Generated servlet source: " + outputFile);
    }

    private void processRestOperations(Map<String, com.wordnik.swagger.models.Path> paths,
                                       Map<String, Object> data) {
        List<RestOperation> restOperations = new LinkedList<>();

        data.put("operations", restOperations);
        boolean hasPathPatterns = false;

        for (String pathName : paths.keySet()) {
            com.wordnik.swagger.models.Path path = paths.get(pathName);

            hasPathPatterns |= addOperation(restOperations, "GET", pathName, path.getGet());
            hasPathPatterns |= addOperation(restOperations, "POST", pathName, path.getPost());
            hasPathPatterns |= addOperation(restOperations, "PUT", pathName, path.getPut());
            hasPathPatterns |= addOperation(restOperations, "DELETE", pathName, path.getDelete());
            hasPathPatterns |= addOperation(restOperations, "PATCH", pathName, path.getPatch());
        }

        data.put("hasPathPatterns", hasPathPatterns);
    }

    private boolean addOperation(List<RestOperation> restOperations,
                              String method, String pathName, Operation operation) {
        boolean hasPathPatterns = false;

        if (operation != null) {
            RestOperation restOperation = new RestOperation(method, pathName, operation);
            if (null != restOperation.getPathPattern()) {
                hasPathPatterns = true;
            }
            restOperations.add(restOperation);
        }

        return hasPathPatterns;
    }

    private Configuration loadConfiguration() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setDefaultEncoding(sourceEncoding);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        TemplateLoader templateLoader;
        ClassTemplateLoader classTemplateLoader = new ClassTemplateLoader(getClass(), "/");
        if (templateDirectory != null) {
            FileTemplateLoader fileTemplateLoader = new FileTemplateLoader(templateDirectory);
            TemplateLoader[] templateLoaders = new TemplateLoader[]{fileTemplateLoader, classTemplateLoader};
            templateLoader = new MultiTemplateLoader(templateLoaders);
        } else {
            templateLoader = classTemplateLoader;
        }
        cfg.setTemplateLoader(templateLoader);

        return cfg;
    }
}
