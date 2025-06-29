plugins {
    java
}

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    compileOnly(libs.jetbrains.annotations)

    testImplementation(libs.bundles.junit)
}

tasks.jar {
    from(sourceSets.main.get().output)
}

tasks.withType<Test> {
    useJUnitPlatform()
}