plugins {
    // Корневой проект сам по себе ничего не собирает (нет исходников). Плагин java
    // здесь НЕ применяем намеренно: иначе создавался бы пустой build/libs/
    // MailDronePlugin-<версия>.jar (только манифест, без plugin.yml), который легко
    // спутать с готовым плагином. Единственный рабочий jar — plugin/build/libs/
    // MailDrone-<версия>.jar (shadowJar). java-library подключается в subprojects ниже.
    // Подключаем shadow, но применяем только в модуле plugin (сборка финального jar).
    id("com.gradleup.shadow") version "9.4.2" apply false
}

allprojects {
    group = "ru.maildrone"
    // Версия берётся из gradle.properties (version=...), переопределяется в CI
    // релиза тегом: ./gradlew build -Pversion=1.2.3. Так все модули и финальный
    // jar (MailDrone-<версия>.jar) получают единую версию из тега.
    version = rootProject.version
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
