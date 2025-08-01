import com.rickbusarow.kgx.libsCatalog
import com.rickbusarow.kgx.version
import com.squareup.workflow1.buildsrc.internal.javaTarget
import com.squareup.workflow1.buildsrc.internal.javaTargetVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("com.android.test")
  id("org.jetbrains.kotlin.android")
}

// Note: We are not including our defaults from .buildscript as we do not need the base Workflow
// dependencies that those include.

android {
  compileSdk = libsCatalog.version("compileSdk").toInt()

  compileOptions {
    sourceCompatibility = javaTargetVersion
    targetCompatibility = javaTargetVersion
  }

  defaultConfig {
    minSdk = 28
    targetSdk = libsCatalog.version("targetSdk").toInt()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    // This benchmark buildType is used for benchmarking, and should function like your
    // release build (for example, with minification on). It's signed with a debug key
    // for easy local/CI testing.
    create("benchmark") {
      isDebuggable = true
      signingConfig = getByName("debug").signingConfig
      matchingFallbacks.add("release")
      proguardFile("baseline-proguard-rules.pro")
    }
  }

  // Unclear: there are kotlin_builtins duplication between 1.5.20 and 1.6.10?
  packaging {
    resources.excludes.add("**/*.kotlin_*")
  }

  targetProjectPath = ":benchmarks:performance-poetry:complex-poetry"
  namespace = "com.squareup.benchmarks.performance.poetry.complex.benchmark"
  experimentalProperties["android.experimental.self-instrumenting"] = true
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_1_8)
    freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
  }
}

dependencies {
  implementation(libs.androidx.macro.benchmark)
  implementation(libs.androidx.test.espresso.core)
  implementation(libs.androidx.test.junit)
  implementation(libs.androidx.test.uiautomator)

  implementation(project(":benchmarks:performance-poetry:complex-poetry"))
  implementation(project(":samples:containers:poetry"))
  implementation(project(":workflow-core"))
}

androidComponents {
  beforeVariants(selector().all()) {
    it.enable = it.buildType == "benchmark"
  }
}
