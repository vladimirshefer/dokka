package org.jetbrains.dokka.gradle

import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.kotlin.dsl.get
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gradle.kotlin.KotlinSourceSetGist
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigureWithKotlinSourceSetGistTest {
    @Test
    fun `example gist`() {
        val project = ProjectBuilder.builder().build()

        val f1Jar = project.file("f1.jar")
        val f2Jar = project.file("f2.jar")
        check(f1Jar.createNewFile())
        check(f2Jar.createNewFile())

        val customSourceRoot = project.file("customSourceRoot")
        check(customSourceRoot.mkdirs())

        val gist = KotlinSourceSetGist(
            name = "customName",
            platform = KotlinPlatformType.common,
            isMain = true,
            classpath = project.files(f1Jar, f2Jar),
            sourceRoots = project.files(customSourceRoot),
            dependentSourceSetNames = project.provider { setOf("customRootSourceSet") }
        )

        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        sourceSet.configureWithKotlinSourceSetGist(gist)

        assertEquals(
            "common", sourceSet.build().displayName,
            "Expected platform being used as default displayName for source set"
        )

        assertEquals(
            Platform.common, sourceSet.build().analysisPlatform,
            "Expected common platform being set"
        )

        assertEquals(
            setOf(f1Jar, f2Jar), sourceSet.build().classpath,
            "Expected classpath being present"
        )

        assertEquals(
            setOf(sourceSet.DokkaSourceSetID("customRootSourceSet")), sourceSet.build().dependentSourceSets,
            "Expected customRootSourceSet being present in dependentSourceSets after build"
        )

        assertEquals(
            setOf(customSourceRoot), sourceSet.build().sourceRoots,
            "Expected customSourceRoot being present in sourceRoots after build"
        )
    }

    @Test
    fun `display name for source set customMain`() {
        val project = ProjectBuilder.builder().build()

        val gist = KotlinSourceSetGist(
            name = "customMain",
            platform = KotlinPlatformType.common,
            isMain = true,
            classpath = project.files(),
            sourceRoots = project.files(),
            dependentSourceSetNames = project.provider { emptySet() }
        )

        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        sourceSet.configureWithKotlinSourceSetGist(gist)

        assertEquals(
            "custom", sourceSet.build().displayName,
            "Expected 'Main' being trimmed from source set name and used as display name"
        )
    }

    @Suppress("UnstableApiUsage")
    @Test
    fun `configuration with kotlin source set is live`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.jvm")
        val kotlin = project.kotlin as KotlinJvmProjectExtension
        val mainSourceSet = kotlin.sourceSets["main"]

        /* Make sure that the source roots exist on filesystem */
        mainSourceSet.kotlin.sourceDirectories.elements.get().map { it.asFile }.forEach { it.mkdirs() }

        /* Make sure to remove dependencies that cannot be resolved during test */
        project.configurations.configureEach { configuration ->
            configuration.withDependencies { dependencies ->
                dependencies.removeIf { dependency -> dependency !is FileCollectionDependency }
            }
        }

        val dokkaSourceSet = GradleDokkaSourceSetBuilder("main", project)
        dokkaSourceSet.kotlinSourceSet(mainSourceSet)

        assertEquals(
            listOf(project.file("src/main/kotlin"), project.file("src/main/java")),
            dokkaSourceSet.sourceRoots.elements.get().map { it.asFile },
            "Expected default source roots being present in dokkaSourceSet"
        )

        val customSourceRoot = project.file("src/main/customRoot")
        check(customSourceRoot.mkdirs())
        mainSourceSet.kotlin.srcDir(customSourceRoot)

        assertEquals(
            listOf(project.file("src/main/kotlin"), project.file("src/main/java"), project.file("src/main/customRoot")),
            dokkaSourceSet.sourceRoots.elements.get().map { it.asFile },
            "Expected customRoot being added to source roots in dokkaSourceSet"
        )
    }
}
