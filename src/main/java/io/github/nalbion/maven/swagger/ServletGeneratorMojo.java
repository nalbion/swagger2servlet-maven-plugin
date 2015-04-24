package io.github.nalbion.maven.swagger;

import com.wordnik.swagger.models.*;
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
                    parseSchema(schemaFile, index++);
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
                                parseSchema(file.toString(), index++);
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to parse " + file.toString(), e);
                            } catch (TemplateException e) {
                                throw new RuntimeException("Failed to process template", e);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } else {
                    parseSchema(schemaLocation, 0);
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to parse schema", e);
            } catch (TemplateException e) {
                throw new MojoExecutionException("Failed to process template", e);
            }
        }
    }

    static String camelCase(String original) {
        String converted = original.replaceAll("[^\\w\\d]", " ");
        return WordUtils.capitalize(converted).replaceAll(" ", "");
    }

    private void parseSchema(String schemaPathName, int index) throws IOException, TemplateException {
        System.out.println(schemaPathName);
        File schemaFile = new File(schemaPathName);
        Swagger swagger = new SwaggerParser().read(schemaPathName);

        if (swagger == null) {
            System.out.println("No swagger - skipping " + schemaPathName);
            return;
        }
        Map<String, com.wordnik.swagger.models.Path> paths = swagger.getPaths();
        if (paths == null) {
            System.out.println("No paths - skipping " + schemaPathName);
            return;
        }

        schemaFile.getName();

        Info info = swagger.getInfo();
        String title = info == null ? null : info.getTitle();
        if (title == null) {
            title = schemaFile.getName().replaceAll("(api|API)?\\.(yaml|json)$", "");
        }
        String className = camelCase(title) + "Api";

        File outputFile = new File(outputDirectory,
                                packageName.replaceAll("\\.", "/") + "/" + className + ".java");

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

        Template baseClassTemplate = cfg.getTemplate("base-class-template.ftl");
        Template classTemplate = cfg.getTemplate("class-template.ftl");

        Map<String, Object> data = new HashMap<>();
        data.put("packageName", packageName);
        data.put("className", className);
        data.put("basePath", swagger.getBasePath());

        List<RestOperation> restOperations = new LinkedList<>();
        data.put("operations", restOperations);

        for (String pathName : paths.keySet()) {
            com.wordnik.swagger.models.Path path = paths.get(pathName);

            addOperation(restOperations, "GET", pathName, path.getGet());
            addOperation(restOperations, "POST", pathName, path.getPost());
            addOperation(restOperations, "PUT", pathName, path.getPut());
            addOperation(restOperations, "DELETE", pathName, path.getDelete());
            addOperation(restOperations, "PATCH", pathName, path.getPatch());
        }

        if (!outputFile.exists()) {
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
        }
        Writer out = new FileWriter(outputFile);
        classTemplate.process(data, out);
        out.close();
        System.out.println("Generated servlet source: " + outputFile);
    }

    private void addOperation(List<RestOperation> restOperations,
                              String method, String pathName, Operation operation) {
        if (operation != null) {
            restOperations.add(new RestOperation(method, pathName, operation));
        }
    }




//        // build a authorization value
//        AuthorizationValue mySpecialHeader = new AuthorizationValue()
//                .keyName("x-special-access")  //  the name of the authorization to pass
//                .value("i-am-special")        //  the value of the authorization
//                .type("header");              //  the location, as either `header` or `query`

//        // or in a single constructor
//        AuthorizationValue apiKey = new AuthorizationValue("api_key", "special-key", "header");
//        Swagger swagger = new SwaggerParser().read(schemaLocation,
//                                    Arrays.asList(mySpecialHeader, apiKey)
//        );

        //swagger.getHost()

//        File f = outputDirectory;
//
//        if ( !f.exists() )
//        {
//            f.mkdirs();
//        }
//
//        File touch = new File( f, "touch.txt" );
//
//        FileWriter w = null;
//        try
//        {
//            w = new FileWriter( touch );
//
//            w.write( "touch.txt" );
//        }
//        catch ( IOException e )
//        {
////            throw new MojoExecutionException( "Error creating file " + touch, e );
//        }
//        finally
//        {
//            if ( w != null )
//            {
//                try
//                {
//                    w.close();
//                }
//                catch ( IOException e )
//                {
//                    // ignore
//                }
//            }
//        }
//    }
}
