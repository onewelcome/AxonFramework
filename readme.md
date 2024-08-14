Welcome to the Axon Framework
=============================

For more information, visit our website: http://www.axonframework.org

If you're looking for the issue tracker, visit http://issues.axonframework.org.

# Forked Project

This project has been forked to facilitate its migration to Java 17.

## Overview of Changes

### Migrations

1. **Migration to Java 17**
2. **Migration to Spring Framework 6.x**
3. **Migration to Hibernate ORM 6.x**

### Changes Due to Migrations

#### Generic Caches

Axon Cache is now a generic type. When registering a cache, both key and value types must be specified. For caches that store generic types,
additional steps may be needed to define the cache:

```java
@Bean
public Cache<String, EventSourcedAggregateRoot<String>> cacheAdapter() {
  final CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
      .withCache("testCache", CacheConfigurationBuilder.newCacheConfigurationBuilder(
          String.class,
          EventSourcedAggregateRoot.class,
          ResourcePoolsBuilder.heap(100)
      ))
      .build(true);
  //noinspection unchecked
  var valueType = (Class<EventSourcedAggregateRoot<String>>) (Class<?>) EventSourcedAggregateRoot.class;
  org.ehcache.Cache<String, EventSourcedAggregateRoot<String>> testCache = cacheManager.getCache("testCache", String.class, valueType);
  return new EhCacheAdapter<>(testCache);
}
```

## Known Issues in Java 17+

### Handling Forbidden Reflective Access

This framework relies on XStream, which uses its own Java serializer. This serializer requires access to certain internal Java classes that
are no longer accessible by default due to the strong encapsulation in recent Java versions. This may result in the following error:

> Caused by: java.lang.reflect.InaccessibleObjectException: Unable to make java.lang.Object java.io.ObjectStreamClass.newInstance() throws
> java.lang.InstantiationException,java.lang.reflect.InvocationTargetException,java.lang.UnsupportedOperationException accessible: module
> java.base does not "opens java.io" to unnamed module @64729b1e

To bypass this restriction and allow tests to pass, add the following JVM flag to the test executor's configuration:

```shell
--add-opens java.base/java.io=ALL-UNNAMED
```

> ⚠️ This might also be required to be added to the application's JVM flag that uses Axon in Java 17+.

#### Configuring the JVM Flag for Test Execution

##### **Maven Surefire Plugin**

To apply this flag in a Maven project, modify the `maven-surefire-plugin` configuration in your `pom.xml`:

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

If your project uses the Failsafe plugin (commonly for integration tests), configure the `maven-failsafe-plugin` in your `pom.xml`:

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

For Gradle projects, include the following configuration in your `build.gradle` file to apply the JVM flag during test execution:

```groovy
test {
  jvmArgs '--add-opens', 'java.base/java.io=ALL-UNNAMED'
}
```

##### Automatic IDE Configuration

If you are using IntelliJ IDEA, these configurations will be automatically picked up from your `pom.xml` or `build.gradle` file, so manual
setup within the IDE is not required.

### Modules Requiring Testing

Some tests related to MongoDB and AMQP have been temporarily ignored. If these modules need to be supported, the tests must be fixed and
re-enabled. Currently, they rely heavily on PowerMock, which does not work with newer Java versions due to forbidden reflection it uses.