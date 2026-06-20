plugins {
    java
    // Подключаем shadow, но применяем только в модуле plugin (сборка финального jar).
    id("com.gradleup.shadow") version "9.4.2" apply false
}

allprojects {
    group = "ru.maildrone"
    version = "0.1.0"
}

subprojects {
    apply(plugin = "java-library")

    repositories {
        // PaperMC maven-public — группа, проксирующая в т.ч. Maven Central и Adventure.
        // Ставим первым: в песочнице прямой доступ к repo.maven.apache.org нестабилен (TLS).
        maven("https://repo.papermc.io/repository/maven-public/")
        mavenCentral()
    }

    // Компилируем против baseline 1.21.x (байткод Java 21).
    // Такой jar грузится и на 1.21.x (Java 21), и на 26.x (Java 25 исполняет байткод 21).
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // Сохраняем имена параметров (полезно для команд/рефлексии).
        options.compilerArgs.add("-parameters")
    }
}
