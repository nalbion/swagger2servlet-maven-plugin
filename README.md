# swagger2servlet-maven-plugin

Generates Java EE Servlets from a Swagger specification (JSON or YAML) 

## Usage

Add the following to your pom.xml:

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>io.github.nalbion</groupId>
                <artifactId>swagger2servlet-maven-plugin</artifactId>
                <version>1.0-SNAPSHOT</version>
                <configuration>
                    <packageName>package.for.your.api</packageName>
                    <schemaLocation>src/main/swagger</schemaLocation>
                </configuration>
                <executions>
                    <execution>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

...then run:
 
    mvn generate-sources