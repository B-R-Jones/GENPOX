package com.example.genpox

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.genpox.ui.main.MainScreen
import com.example.genpox.ui.main.MainViewModel

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Main)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
        entryProvider {
            entry<Main> {
                val mainViewModel: MainViewModel = hiltViewModel()
                MainScreen(viewModel = mainViewModel, onItemClick = { navKey -> backStack.add(navKey) }, modifier = Modifier.fillMaxSize())
            }
        },
    )
}
