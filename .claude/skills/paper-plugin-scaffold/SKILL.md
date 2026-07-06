---
name: paper-plugin-scaffold
description: Scaffold a new Paper 1.21.x plugin project with Gradle Kotlin DSL + shadow plugin + Java 21. Produces a buildable skeleton that `gradle shadowJar` packs into a single fat jar.
---

# Paper plugin scaffold (verified Paper 1.21.8 + JDK 21 + Gradle 8.10)

This is the minimal layout that compiles and produces a valid `<Name>-<ver>.jar` with `plugin.yml` baked in.

## Files (relative to project root)

### `settings.gradle.kts`
```kotlin
rootProject.name = "<PluginName>"
```

### `gradle.properties`
```
group=com.<your.group>
version=0.1.0
org.gradle.jvmargs=-Xmx1024m
```

### `build.gradle.kts`
```kotlin
plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = property("group") as String
version = property("version") as String

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    processResources {
        val props = mapOf("version" to project.version.toString())
        inputs.properties(props)
        filesMatching("plugin.yml") { expand(props) }
    }
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("<PluginName>")
    }
    build { dependsOn(shadowJar) }
}
```

### `src/main/resources/plugin.yml`
```yaml
name: <PluginName>
version: ${version}
main: com.<group>.<PluginName>
api-version: '1.21'
load: POSTWORLD
```

`load: POSTWORLD` matters when your plugin manages its own worlds — main worlds load first, then your `onEnable` runs.

### `src/main/java/com/<group>/<PluginName>.java`
```java
package com.<group>;
import org.bukkit.plugin.java.JavaPlugin;
public final class <PluginName> extends JavaPlugin {
    @Override public void onEnable() { getLogger().info("Enabled."); }
    @Override public void onDisable() { getLogger().info("Disabled."); }
}
```

## Build & verify

```bash
gradle shadowJar
unzip -p build/libs/<PluginName>-<ver>.jar plugin.yml   # version expanded?
unzip -l build/libs/<PluginName>-<ver>.jar | head       # class present?
```

## Gotchas

- `load: POSTWORLD` (default) means worlds you create in `onEnable` via `Bukkit.createWorld(...)` work. With `load: STARTUP` you cannot create worlds at enable time.
- Use `compileOnly` for paper-api — it's provided by the server. Listing it as `implementation` would shade it into the jar (huge, breaks the loader).
- Always `expand(props)` in processResources so `${version}` in `plugin.yml` matches `gradle.properties`.
- `archiveClassifier.set("")` — without this, shadowJar names file `<Plugin>-<ver>-all.jar`, which is annoying to deploy.
