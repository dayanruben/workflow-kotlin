import com.rickbusarow.kgx.libsCatalog
import com.rickbusarow.kgx.version
import com.squareup.workflow1.buildsrc.internal.javaTargetVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("com.android.application")
  id("kotlin-android")
  id("kotlin-parcelize")
}
android {
  compileSdk = libsCatalog.version("compileSdk").toInt()

  compileOptions {
    sourceCompatibility = javaTargetVersion
    targetCompatibility = javaTargetVersion
  }

  defaultConfig {
    targetSdk = libsCatalog.version("targetSdk").toInt()
    minSdk = 29
    applicationId = "com.squareup.benchmarks.performance.complex.poetry"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      signingConfig = signingConfigs.getByName("debug")
      isDebuggable = false
    }
    create("benchmark") {
      initWith(buildTypes.getByName("release"))
      signingConfig = signingConfigs.getByName("debug")
      isDebuggable = false
      // Selects release buildType if the benchmark buildType not available in other modules.
      matchingFallbacks.add("release")
      proguardFile("baseline-proguard-rules.pro")
    }
  }

  // Collisions in packaging.
  packaging {
    resources.excludes.add("META-INF/AL2.0")
    resources.excludes.add("META-INF/LGPL2.1")
  }
  namespace = "com.squareup.benchmarks.performance.complex.poetry"
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_1_8)
  }
}

dependencies {
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation(libs.androidx.test.junit)

  androidTestImplementation(project(":workflow-ui:internal-testing-android"))

  // API on an app module so these are transitive dependencies for the benchmarks.
  api(project(":samples:containers:android"))
  api(project(":samples:containers:common"))
  api(project(":samples:containers:poetry"))
  api(project(":workflow-core"))
  api(project(":workflow-runtime"))
  api(project(":workflow-ui:core-android"))
  api(project(":workflow-ui:core-common"))

  debugImplementation(libs.squareup.leakcanary.android)

  implementation(libs.androidx.activity.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.lifecycle.viewmodel.core)
  implementation(libs.androidx.lifecycle.viewmodel.savedstate)
  // Used to side load Baseline Profile when Benchmarking and not installed by Play Store.
  implementation(libs.androidx.profileinstaller)
  implementation(libs.androidx.recyclerview)
  implementation(libs.androidx.savedstate)
  implementation(libs.androidx.test.uiautomator)
  implementation(libs.androidx.tracing.core)
  implementation(libs.androidx.tracing.ktx)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.timber)
}
