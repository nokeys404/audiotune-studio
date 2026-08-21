package com.audiotune.studio.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.audiotune.studio.presentation.equalizer.EqualizerScreen
import com.audiotune.studio.presentation.home.HomeScreen
import com.audiotune.studio.presentation.music.MusicScreen
import com.audiotune.studio.presentation.settings.SettingsScreen

@Composable
fun AudioTuneNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToMusic = {
                    navController.navigate(Screen.Music.route)
                },
                onNavigateToEqualizer = {
                    navController.navigate(Screen.Equalizer.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Music.route) {
            MusicScreen()
        }

        composable(Screen.Equalizer.route) {
            EqualizerScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
