package com.memorystars.leitner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.memorystars.leitner.ui.*

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App(vm) }
    }
}

@Composable
fun App(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    val set = s
    if (set == null) { Box(Modifier.fillMaxSize()); return }
    val dark = when (set.themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val theme = Themes.firstOrNull { it.id == set.themeId } ?: Themes[0]
    val d = LocalDensity.current
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDensity provides Density(d.density, d.fontScale * if (set.bigFont) 1.3f else 1f)
    ) {
        MaterialTheme(colorScheme = themeScheme(theme, dark)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                val nav = rememberNavController()
                val long = { n: String -> navArgument(n) { type = NavType.LongType } }
                val int = { n: String -> navArgument(n) { type = NavType.IntType } }
                NavHost(nav, startDestination = if (set.onboarded) "home" else "welcome") {
                    composable("welcome") { WelcomeScreen(vm, nav) }
                    composable("home") { HomeScreen(vm, nav, theme) }
                    composable("decks") { DecksScreen(vm, nav) }
                    composable("deck/{id}", listOf(long("id"))) { DeckDetailScreen(vm, nav, it.arguments!!.getLong("id")) }
                    composable("editor/{deckId}/{cardId}", listOf(long("deckId"), long("cardId"))) {
                        EditorScreen(vm, nav, it.arguments!!.getLong("deckId"), it.arguments!!.getLong("cardId"))
                    }
                    composable("review/{deckId}", listOf(long("deckId"))) { ReviewScreen(vm, nav, it.arguments!!.getLong("deckId")) }
                    composable("result") { ResultScreen(vm, nav) }
                    composable("rewards") { RewardsScreen(vm, nav) }
                    composable("progress") { ProgressScreen(vm, nav) }
                    composable("themes") { ThemesScreen(vm, nav) }
                    composable("settings") { SettingsScreen(vm, nav) }
                    composable("leitner") { LeitnerSettingsScreen(vm, nav) }
                    composable("parent") { ParentScreen(vm, nav) }
                    composable("practice/{deckId}", listOf(long("deckId"))) {
                        PracticeSetupScreen(vm, nav, it.arguments!!.getLong("deckId"))
                    }
                    composable(
                        "practiceRun/{mode}/{deckId}/{filter}/{order}/{rev}",
                        listOf(int("mode"), long("deckId"), int("filter"), int("order"), int("rev"))
                    ) {
                        val a = it.arguments!!
                        PracticeRunScreen(vm, nav, a.getInt("mode"), a.getLong("deckId"), a.getInt("filter"), a.getInt("order"), a.getInt("rev") == 1)
                    }
                    composable("backup") { BackupScreen(vm, nav) }
                }
            }
        }
    }
}
