package com.example.genpox

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Main : NavKey

sealed interface MainTab {
    val route: String
    data object Combinator : MainTab {
        override val route = "combinator"
    }
    data object Splicer : MainTab {
        override val route = "splicer"
    }
    data object Scanner : MainTab {
        override val route = "scanner"
    }
    data object Vault : MainTab {
        override val route = "vault"
    }
    data object Network : MainTab {
        override val route = "network"
    }
    data object Settings : MainTab {
        override val route = "settings"
    }

    companion object {
        fun fromRoute(route: String): MainTab = when (route) {
            "splicer" -> Splicer
            "scanner" -> Scanner
            "vault" -> Vault
            "network" -> Network
            "settings" -> Settings
            else -> Combinator
        }
    }
}
