import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.FileSystemOperations
import java.security.MessageDigest
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.marat.albion"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.marat.albion"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Для личной установки подписываем debug-ключом; замените на свой keystore для публикации.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    sourceSets.getByName("androidTest").assets.srcDir("schemas")

    androidResources {
        // mp3/xlsx уже сжаты — не тратим время на повторное сжатие.
        noCompress += listOf("mp3", "xlsx", "epub", "pdf")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// ---------------------------------------------------------------------------
// Материалы: collections/*.xlsx, audio/<колода>/*.mp3, grammar/*.md
// копируются в assets/content при albion.bundleContent=true.
// ---------------------------------------------------------------------------
/** Имена MP3 не гарантируют, что внутри произнесено нужное слово. Фиксируем проверенный набор. */
abstract class VerifyAlbionAudio : DefaultTask() {
    @get:Internal
    abstract val audioDir: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val recordings: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val baseline: RegularFileProperty

    @TaskAction
    fun verify() {
        val expected = linkedMapOf<String, String>()
        baseline.get().asFile.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.forEach { line ->
            val parts = line.split("  ", limit = 2)
            if (parts.size != 2 || !parts[0].matches(Regex("[a-f0-9]{64}")) || expected.put(parts[1], parts[0]) != null) {
                throw GradleException("Некорректная запись в ${baseline.get().asFile}: $line")
            }
        }
        val root = audioDir.get().asFile
        val actual = recordings.files.associateBy { it.relativeTo(root).invariantSeparatorsPath }
        val changed = actual.filter { (name, file) ->
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    digest.update(buffer, 0, n)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) } != expected[name]
        }.keys
        val missing = expected.keys - actual.keys
        if (changed.isNotEmpty() || missing.isNotEmpty()) {
            throw GradleException(
                "Изменена непроверенная озвучка: ${(changed + missing).take(10).joinToString()}. " +
                    "Проверьте произношение и обновите app/audio-checksums.sha256 (см. README.md проекта)."
            )
        }
        logger.lifecycle("Озвучка: проверены ${actual.size} файлов.")
    }
}

abstract class SyncAlbionContent @Inject constructor(
    private val fs: FileSystemOperations,
) : DefaultTask() {
    /** Корень с материалами; сам по себе не вход — входы ниже (только нужные файлы). */
    @get:Internal
    abstract val sourceDir: DirectoryProperty

    /** Только нужные файлы из collections/, audio/, grammar/ — от них зависит пересборка. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun sync() {
        val src = sourceDir.orNull?.asFile
        if (src != null && !File(src, "collections").isDirectory) {
            throw GradleException("Не найдена папка с материалами: ${File(src, "collections")}. Укажите albion.contentDir в gradle.properties.")
        }
        fs.sync {
            into(outputDir.dir("content"))
            if (src != null) {
                from(File(src, "collections")) {
                    include("*.xlsx")
                    exclude("~$*")
                    into("collections")
                }
                from(File(src, "audio")) {
                    include("**/*.mp3")
                    into("audio")
                }
                from(File(src, "grammar")) {
                    include("*.md")
                    into("grammar")
                }
                from(File(src, "library")) {
                    include("*.txt", "*.md", "*.epub", "*.pdf")
                    into("library")
                }
            }
        }
    }
}

val bundleContent = providers.gradleProperty("albion.bundleContent").map(String::toBoolean).getOrElse(true)
val contentDir = rootProject.layout.projectDirectory.dir(
    providers.gradleProperty("albion.contentDir").getOrElse("..")
)

val verifyAlbionAudio = tasks.register<VerifyAlbionAudio>("verifyAlbionAudio") {
    group = "albion"
    description = "Проверяет контрольные суммы аудионабора после проверки произношения."
    audioDir.set(contentDir.dir("audio"))
    recordings.from(contentDir.dir("audio").asFileTree.matching { include("**/*.mp3") })
    // Эталон входит в исходники приложения, а не во внешнюю папку материалов.
    baseline.set(layout.projectDirectory.file("audio-checksums.sha256"))
}

val syncAlbionContent = tasks.register<SyncAlbionContent>("syncAlbionContent") {
    group = "albion"
    description = "Копирует xlsx, озвучку и грамматику в assets/content."
    if (bundleContent) {
        dependsOn(verifyAlbionAudio)
        sourceDir.set(contentDir)
        sources.from(
            contentDir.dir("collections").asFileTree.matching { include("*.xlsx"); exclude("~$*") },
            contentDir.dir("audio").asFileTree.matching { include("**/*.mp3") },
            contentDir.dir("grammar").asFileTree.matching { include("*.md") },
            contentDir.dir("library").asFileTree.matching { include("*.txt", "*.md", "*.epub", "*.pdf") },
        )
    }
    outputDir.set(layout.buildDirectory.dir("generated/albionContent"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(syncAlbionContent, SyncAlbionContent::outputDir)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.media3.exoplayer)
    implementation(libs.mlkit.translate)
    // Текст из PDF для библиотеки (Apache 2.0).
    implementation(libs.pdfbox.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kxml2)
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
