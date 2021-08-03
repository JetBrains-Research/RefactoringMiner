dependencies {
    implementation(project(":refactoringminer"))
}

open class IOCliTask : org.jetbrains.intellij.tasks.RunIdeTask() {
    // Name of operation to perform (h/help for usage)
    val operation: String? by project

    // Path to the local git directory or URL
    val pathToGit: String? by project

    // Commit, tag, branch or pull-request
    val position: String? by project

    // Commit or tag for bt and bc options
    val endPosition: String? by project

    val timeout: String? by project

    // Path to json for output
    val json: String? by project

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
    }
}

tasks {
    register<IOCliTask>("refactoringminer-CLI") {
        dependsOn("buildPlugin")
        args = listOf(
            "RefactoringMiner",
            operation,
            pathToGit,
            position,
            endPosition,
            timeout,
            json
        ).map { it.orEmpty() }
    }
}
