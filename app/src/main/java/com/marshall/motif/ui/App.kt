package com.marshall.motif.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.marshall.motif.SettingsStore
import com.marshall.motif.WidgetStateStore
import com.marshall.motif.ble.BleManager
import com.marshall.motif.ui.screens.ControlsScreen
import com.marshall.motif.ui.screens.DevicesScreen
import com.marshall.motif.ui.screens.FindScreen
import com.marshall.motif.ui.screens.HomeScreen
import com.marshall.motif.ui.screens.SettingsScreen
import com.marshall.motif.ui.screens.SoundScreen
import com.marshall.motif.ui.screens.WearScreen
import com.marshall.motif.ui.theme.MarshallTheme

private enum class AppRoute {
    Devices,
    Home,
    Sound,
    Controls,
    Wear,
    Find,
    Settings,
}

@Composable
fun MarshallApp(ble: BleManager, settings: SettingsStore) {
    MarshallTheme(
        themeMode = settings.themeMode,
        customThemeMode = settings.customThemeMode,
        accentColor = settings.accentColor,
    ) {
        var route by rememberSaveable { mutableStateOf(AppRoute.Devices) }
        val snackbarHostState = remember { SnackbarHostState() }
        var showPicker by remember { mutableStateOf(false) }
        val context = LocalContext.current

        fun navigateBack() {
            route = when (route) {
                AppRoute.Devices -> route
                AppRoute.Home -> AppRoute.Devices
                AppRoute.Sound,
                AppRoute.Controls,
                AppRoute.Wear,
                AppRoute.Find,
                AppRoute.Settings,
                    -> AppRoute.Home
            }
        }

        // System / gesture back: pop in-app stack instead of leaving the app.
        BackHandler(enabled = route != AppRoute.Devices || showPicker) {
            when {
                showPicker -> showPicker = false
                else -> navigateBack()
            }
        }

        LaunchedEffect(
            ble.state.connected,
            ble.state.leftBattery,
            ble.state.rightBattery,
            ble.state.caseBattery,
            ble.state.ancMode,
            ble.state.deviceName,
        ) {
            WidgetStateStore.save(
                context = context,
                left = ble.state.leftBattery,
                right = ble.state.rightBattery,
                case = ble.state.caseBattery,
                ancMode = ble.state.ancMode,
                name = ble.state.deviceName,
                connected = ble.state.connected,
            )
        }

        LaunchedEffect(ble.state.connected) {
            if (ble.state.connected && route == AppRoute.Devices) {
                route = AppRoute.Home
            }
        }

        LaunchedEffect(ble.message) {
            ble.message?.let {
                snackbarHostState.showSnackbar(it)
                ble.consumeMessage()
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                )
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal ||
                                (initialState == AppRoute.Devices && targetState == AppRoute.Home)
                        val enter = if (forward) {
                            slideInHorizontally(tween(280)) { it / 5 } + fadeIn(tween(220))
                        } else {
                            slideInHorizontally(tween(280)) { -it / 5 } + fadeIn(tween(220))
                        }
                        val exit = if (forward) {
                            slideOutHorizontally(tween(240)) { -it / 8 } + fadeOut(tween(180))
                        } else {
                            slideOutHorizontally(tween(240)) { it / 8 } + fadeOut(tween(180))
                        }
                        enter togetherWith exit
                    },
                    label = "route",
                    modifier = Modifier.fillMaxSize(),
                ) { current ->
                    when (current) {
                        AppRoute.Devices -> DevicesScreen(
                            ble = ble,
                            onOpenDevice = { route = AppRoute.Home },
                            onConnect = { showPicker = true },
                            modifier = Modifier.fillMaxSize(),
                        )

                        AppRoute.Home -> HomeScreen(
                            ble = ble,
                            onBack = { route = AppRoute.Devices },
                            onOpenSound = { route = AppRoute.Sound },
                            onOpenControls = { route = AppRoute.Controls },
                            onOpenWear = { route = AppRoute.Wear },
                            onOpenFind = { route = AppRoute.Find },
                            onOpenSettings = { route = AppRoute.Settings },
                            modifier = Modifier.fillMaxSize(),
                        )

                        AppRoute.Sound -> SoundScreen(
                            ble = ble,
                            onBack = { route = AppRoute.Home },
                            modifier = Modifier.fillMaxSize(),
                        )

                        AppRoute.Controls -> ControlsScreen(
                            ble = ble,
                            onBack = { route = AppRoute.Home },
                            modifier = Modifier.fillMaxSize(),
                        )

                        AppRoute.Wear -> WearScreen(
                            ble = ble,
                            onBack = { route = AppRoute.Home },
                            modifier = Modifier.fillMaxSize(),
                        )

                        AppRoute.Find -> FindScreen(
                            ble = ble,
                            onBack = { route = AppRoute.Home },
                            modifier = Modifier.fillMaxSize(),
                        )

                        AppRoute.Settings -> SettingsScreen(
                            ble = ble,
                            settings = settings,
                            onBack = { route = AppRoute.Home },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        if (showPicker) {
            DevicePickerSheet(ble, onDismiss = { showPicker = false })
        }
    }
}
