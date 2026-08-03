import com.android.build.api.artifact.SingleArtifact
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.paulchibamba.teleprompter"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.paulchibamba.teleprompter"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room writes the schema of every version to app/schemas/. Committing those files is what
        // makes a future migration reviewable and testable against the real previous schema.
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            // Robolectric runs the Room DAO tests against a real SQLite database on the JVM, so CI
            // covers them without an emulator.
            isIncludeAndroidResources = true
        }
    }
}

// P1 (docs/SPEC.md §0): Prompter declares no permissions at all. This inspects the *merged*
// manifest (after manifest merging from all dependencies), not the source AndroidManifest.xml,
// so a permission pulled in transitively by a library is caught too.
abstract class VerifyNoInternetPermissionTask : DefaultTask() {
    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @TaskAction
    fun verify() {
        val manifestFile = mergedManifest.get().asFile
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(manifestFile)
        val permissionNodes = document.getElementsByTagName("uses-permission")
        // AGP auto-declares this one for us, self-scoped and signature-protected, whenever a
        // dependency dynamically registers a broadcast receiver on API 33+ (here: androidx
        // .profileinstaller.ProfileInstallReceiver). No other app can hold it and it grants no
        // capability to anything — it exists purely so the OS can enforce "not exported" on that
        // receiver. Not a real permission grant, so it's exempt from the "no permissions" rule.
        val offenders = (0 until permissionNodes.length)
            .map { index -> permissionNodes.item(index) as Element }
            .map { element -> element.getAttributeNS("http://schemas.android.com/apk/res/android", "name") }
            .filterNot { name -> name.endsWith(".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION") }
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Prompter must declare no permissions, but the merged manifest declares: $offenders"
            )
        }
    }
}

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        val verifyTask = tasks.register(
            "verify${variant.name.replaceFirstChar { it.uppercase() }}NoInternetPermission",
            VerifyNoInternetPermissionTask::class.java,
        ) {
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
        }
        tasks.named("check").configure { dependsOn(verifyTask) }
        if (!tasks.names.contains("verifyNoInternetPermission")) {
            tasks.register("verifyNoInternetPermission") {
                dependsOn(verifyTask)
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}