plugins {
    id("com.gradleup.shadow")
}

repositories {
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation(project(":core"))
    implementation(project(":v1_21"))
    implementation(project(":v26_1"))
    implementation("org.bstats:bstats-bukkit:3.1.0")
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveBaseName.set("MailDrone")
    archiveClassifier.set("")
    relocate("org.bstats", "ru.maildrone.libs.bstats")
}

// Тонкий jar без зависимостей бесполезен — оставляем только shadow-jar.
tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
