package com.example.genpox.ui.main

import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import java.io.File
import java.io.FileNotFoundException

class ComposeBindingTest {

    private val forbiddenUiImports = listOf(
        "androidx.compose.ui",
        "android.view.View",
        "android.widget.",
        "androidx.activity.ComponentActivity"
    )

    private fun findUiMainSourceFile(fileName: String): File {
        val relativePath = "src/main/java/com/example/genpox/ui/main/$fileName"
        val searchPaths = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath"),
            File("pox-android/app/$relativePath"),
            File("../../pox-android/app/$relativePath")
        )
        for (file in searchPaths) {
            if (file.exists() && file.isFile) {
                return file
            }
        }
        throw FileNotFoundException("Could not locate source file: $fileName")
    }

    private fun auditViewModelImports(fileName: String) {
        val file = findUiMainSourceFile(fileName)
        val lines = file.readLines()
        val violations = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("import ")) {
                for (forbidden in forbiddenUiImports) {
                    if (trimmed.contains(forbidden)) {
                        violations.add(line)
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            fail("Compose binding architecture violation in ${file.name}: ViewModel references UI/View packages directly:\n" +
                 violations.joinToString("\n"))
        }
    }

    @Test
    fun testViewModelsHaveNoDirectUiDependencies() {
        // Assert that the ViewModels do not directly import UI layout classes
        auditViewModelImports("MainViewModel.kt")
        auditViewModelImports("SplicerViewModel.kt")
        auditViewModelImports("ScannerViewModel.kt")
        auditViewModelImports("ReactorViewModel.kt")
        auditViewModelImports("NetworkViewModel.kt")
    }
}
