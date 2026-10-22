package com.example.genpox

import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException

class ImportAuditTest {

    private val forbiddenImports = listOf(
        "androidx.compose",
        "android.view",
        "android.widget",
        "androidx.activity",
        "androidx.fragment",
        "androidx.lifecycle",
    )

    private fun findEngineFile(relativePath: String): File {
        val searchPaths = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath"),
            File("pox-android/app/$relativePath"),
            File("../../pox-android/app/$relativePath"),
        )
        for (file in searchPaths) {
            if (file.exists() && file.isFile) {
                return file
            }
        }
        throw FileNotFoundException("Could not locate source file: $relativePath")
    }

    private fun auditSourceFile(filePath: String) {
        val file = findEngineFile(filePath)
        val lines = file.readLines()
        val violations = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("import ")) {
                for (forbidden in forbiddenImports) {
                    if (trimmed.contains(forbidden)) {
                        violations.add(line)
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Import violation in ${file.name}: Forbidden package imported:\n" +
                    violations.joinToString("\n"),
            )
        }
    }

    @Test
    fun testGeneticsEngineHasNoUiDependencies() {
        auditSourceFile("src/main/java/com/example/genpox/data/GeneticsEngine.kt")
    }

    @Test
    fun testDeploymentEngineHasNoUiDependencies() {
        auditSourceFile("src/main/java/com/example/genpox/data/DeploymentEngine.kt")
    }

    @Test
    fun testConflictEngineHasNoUiDependencies() {
        auditSourceFile("src/main/java/com/example/genpox/data/ConflictEngine.kt")
    }

    @Test
    fun testPoxNetworkManagerHasNoUiDependencies() {
        auditSourceFile("src/main/java/com/example/genpox/data/network/PoxNetworkManager.kt")
    }
}
