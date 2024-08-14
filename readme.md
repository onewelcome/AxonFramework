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

### Google App Engine

The Google App Engine module is currently completely disabled. If Google App Engine support is required, the associated tests would need to
be fixed and re-enabled to verify whether support still functions correctly after the migration to the updated tech stack.

### Saga's AssociationValueEntry ID

The `id` property in `org.axonframework.saga.repository.jpa.AssociationValueEntry` now uses
`@GeneratedValue(strategy = GenerationType.IDENTITY)` instead of the default `@GeneratedValue`. This change was necessary because, without
specifying the strategy, Hibernate did not properly generate sequential IDs.

It's unclear how this functioned correctly before (if at all), so it's important to be aware of this change. It is recommended to
double-check the behavior of your application using Axon to ensure that this modification has not introduced any unintended changes.

### Instantiating Axon's Serializer

This forked version of the Axon Framework uses an updated version of XStream. The new XStream version prohibits serializing classes that
have not been explicitly allowed. Ensure that all classes previously permitted for serialization remain allowed after applying this forked
version of Axon.

For more information, refer to `org.axonframework.testutils.XStreamSerializerFactory`.

### XStream Converters

Currently, the following XStream converters are used in tests:

- `XStreamCopyOnWriteArraySetConverter`
- `XStreamEmptyListConverter`
- `XStreamLinkedBlockingDequeConverter`
- `XStreamSerializerSpringConfiguration`
- `XStreamUnmodifiableMapConverter`

When applying this forked version of the Axon Framework, it may be necessary to reuse these converters. If so, they will need to be moved
from `src/test` to `src/main` and included as part of the distributable JAR.

Additionally, the application using Axon may require additional converters, depending on the data that has already been serialized by the
previous version of Axon and stored in its database. Proper deserialization of this data using these converters is crucial. Ensure thorough
testing, preferably using real data from a live environment, to confirm that everything works as expected.
 