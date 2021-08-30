import java.io.OutputStream

dependencies {
    implementation(project(":refactoringminer"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r")
}

open class IOCliTask : org.jetbrains.intellij.tasks.RunIdeTask() {
    // Name of operation to perform (h/help for usage)
    @get:Input
    val command: String? by project

    // Path to the local git directory or URL
    @get:Input
    @get:Optional
    val gitProjectPath: String? by project

    // Commit, tag, branch or pull-request
    @get:Input
    @get:Optional
    val startPosition: String? by project

    // Commit or tag for bt and bc options
    @get:Input
    @get:Optional
    val endPosition: String? by project

    @get:Input
    @get:Optional
    val timeout: String? by project

    // Path to json for output
    @get:Input
    @get:Optional
    val output: String? by project

    init {
        jvmArgs = listOf(
            "-Djava.awt.headless=true",
            "--add-exports",
            "java.base/jdk.internal.vm=ALL-UNNAMED",
            "-Djdk.module.illegalAccess.silent=true"
        )
        maxHeapSize = "12g"
        standardInput = System.`in`
        standardOutput = System.`out`
        errorOutput = OutputStream.nullOutputStream()
    }
}

tasks {
    register<IOCliTask>("refactoringminer-CLI") {
        dependsOn("buildPlugin")
        args = listOf(
            "RefactoringMiner",
            command,
            gitProjectPath,
            startPosition,
            endPosition,
            timeout,
            output
        ).map { it.orEmpty() }
    }
}
