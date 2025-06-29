# Sico

A lightweight, powerful Inversion of Control (IoC) for Java and Kotlin applications, inspired by Spring but with a minimalist approach.

## Showcase
> You can check [demo examples](sico/src/test/java/dev/hogoshi/sico/) or find all info on [wiki](https://github.com/zhogoshi/sico/wiki) page

## Features

- Dependency injection via constructor and field injection
- Component scanning with package traversal
- Lifecycle management with @PostConstruct and @PreDestroy
- Scheduled task execution with @Scheduled annotation
- Support for different bean scopes (singleton, prototype)
- Configuration classes with @Bean and @Scope methods
- Circular dependency detection
- Kotlin DSL support

## Installation

### Gradle

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.hogoshi.sico:sico:1.0.0")
    // or kotlin one
    implementation("dev.hogoshi.sico:sico-kotlin:1.0.0")
}
```

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>dev.hogoshi.sico</groupId>
        <artifactId>sico</artifactId>
        <version>1.0.0</version>
    </dependency>
    <!-- or kotlin one
    <dependency>
        <groupId>dev.hogoshi.sico</groupId>
        <artifactId>sico-kotlin</artifactId>
        <version>1.0.0</version>
    </dependency>
    -->
</dependencies>
```

> Note: Replace `1.0.0` with the desired version tag from the [releases page](https://github.com/zhogoshi/sico/releases).

## Usage

### Java

```java
// Start and scan packages for components
Sico sico = Sico.getInstance();
sico.start();
sico.scan((s) -> s.contains("com.example"), "com.example.test");

// Register a specific class
sico.register(MyService.class);

// Resolve dependencies
MyService service = sico.resolve(MyService.class);

// Cleanup resources and call predestroy callbacks
sico.close();
```

### Kotlin

```kotlin
// Create and configure the container using DSL
val container = ioc {
    scan("com.example")
    scan("org.example.another")
    register(MyService::class.java)
    
    // You can also use reified type parameters
    register<MyRepository>()
}

// Resolve dependencies
val service = container.get<MyService>()
val controller = container.get<MyController>("myController")

// Cleanup resources
container.close()
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.