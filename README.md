# Sico

A lightweight, powerful Inversion of Control (IoC) for Java and Kotlin applications, inspired by Spring but with a minimalist approach.

## Showcase
> You can check [demo examples](sico/src/test/java/dev/hogoshi/sico/) page or docs on spring page.

## Features

- Dependency injection via constructor and field injection
- Component scanning with package traversal
- Lifecycle management with @PostConstruct and @PreDestroy
- Scheduled task execution with @Scheduled annotation
- Support for different bean scopes (singleton, prototype)
- Configuration classes with @Bean and @Scope methods
- Circular dependency detection

## Installation

### Gradle

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.hogoshi.sico:sico:X.X.X")
}
```

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>dev.hogoshi.sico</groupId>
        <artifactId>sico</artifactId>
        <version>X.X.X</version>
    </dependency>
</dependencies>
```

> Note: Replace `X.X.X` with the desired version tag from the [releases page](https://github.com/zhogoshi/sico/releases).

## Usage

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

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
