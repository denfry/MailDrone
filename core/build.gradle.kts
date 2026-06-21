dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.72-stable")
    api(project(":adapter-api"))

    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
