Welcome to the Axon Framework
=============================

For more information, visit our website: http://www.axonframework.org

If you're looking for the issue tracker, visit http://issues.axonframework.org.

# Forked project
This project has been forked since we needed to migrate it to Java 17.

## Known issues in Java 17+

### Handling Forbidden Reflective Access in Java

This framework relies on XStream, which utilizes its own Java serializer. This serializer requires access to certain internal Java classes,
which are not accessible by default due to the strong encapsulation introduced in recent Java versions. This will produce an error:
> Caused by: java.lang.reflect.InaccessibleObjectException: Unable to make java.lang.Object java.io.ObjectStreamClass.newInstance() throws
> java.lang.InstantiationException,java.lang.reflect.InvocationTargetException,java.lang.UnsupportedOperationException accessible: module
> java.base does not "opens java.io" to unnamed module @64729b1e

To bypass this restriction and allow the tests to pass, the following JVM flag must be added to the test executor's configuration:

```shell
--add-opens java.base/java.io=ALL-UNNAMED
```

#### Configuring the JVM Flag for Test Execution

##### **Maven Surefire Plugin**

To apply this flag in a Maven project, modify the `maven-surefire-plugin` configuration in your `pom.xml`. This ensures that the flag is
applied whenever the tests are executed.

```xml

<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <version>3.1.2</version> <!-- Ensure you use an appropriate version -->
  <configuration>
    <argLine>--add-opens java.base/java.io=ALL-UNNAMED</argLine>
  </configuration>
</plugin>
```

##### **Maven Failsafe Plugin**

If your project uses the Failsafe plugin (commonly for integration tests), you can similarly configure the `maven-failsafe-plugin` in
your `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>3.0.0</version> <!-- Ensure you use an appropriate version -->
    <configuration>
        <argLine>--add-opens java.base/java.io=ALL-UNNAMED</argLine>
    </configuration>
</plugin>
```

##### **Gradle**

For Gradle projects, include the following configuration in your `build.gradle` file. This will automatically apply the necessary JVM flag
during test execution.

```groovy
test {
  jvmArgs '--add-opens', 'java.base/java.io=ALL-UNNAMED'
}
```

##### Automatic IDE Configuration

If you are using IntelliJ IDEA, the IDE will automatically pick up these configurations from your `pom.xml` or `build.gradle` file.
Therefore, there is no need to manually configure the test run settings within IntelliJ IDEA.

### MongoDB support needs testing
Some of the tests related to MongoDB have been ignored for now.