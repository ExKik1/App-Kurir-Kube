package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import com.example.core.components.KubeBottomNav
import com.example.core.components.KubeNavTab
import com.example.core.model.DeliveryStatus
import com.example.core.repository.OrderRepository
import com.example.features.auth.AuthScreen
import com.example.features.auth.OnboardingScreen
import com.example.features.dashboard.DashboardScreen
import com.example.features.dashboard.WalletScreen
import com.example.features.delivery.DeliveryListScreen
import com.example.features.map.DeliveryMapScreen
import com.example.features.profile.ProfileScreen
import com.example.features.splash.SplashScreen
import com.example.ui.theme.KubeTheme

enum class AppScreen {
    SPLASH,
    ONBOARDING,
    AUTH_LOGIN,
    AUTH_SIGNUP,
    MAIN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KubeTheme {
                KubeCourierApp()
            }
        }
    }
}

@Composable
fun KubeCourierApp() {
    // Current primary route screen (Splash -> Onboarding -> Auth -> MainWrapper)
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.SPLASH) }
    val repository = remember { OrderRepository.instance }

    // Hardware Back Button Handling for primary routes
    when (currentScreen) {
        AppScreen.AUTH_LOGIN, AppScreen.AUTH_SIGNUP -> {
            BackHandler {
                currentScreen = AppScreen.ONBOARDING
            }
        }
        else -> {}
    }

    Crossfade(targetState = currentScreen, label = "screen_route_crossfade") { screen ->
        when (screen) {
            AppScreen.SPLASH -> {
                // Centered Splash Screen: pushReplacement to Onboarding
                SplashScreen(
                    onSplashFinished = {
                        currentScreen = AppScreen.ONBOARDING
                    }
                )
            }
            AppScreen.ONBOARDING -> {
                // Swipeable PageView Onboarding: pushReplacement to Login
                OnboardingScreen(
                    onContinue = {
                        currentScreen = AppScreen.AUTH_LOGIN
                    }
                )
            }
            AppScreen.AUTH_LOGIN -> {
                // SafeArea & SingleChildScrollView Auth Form
                AuthScreen(
                    initialIsSignUp = false,
                    onBack = { currentScreen = AppScreen.ONBOARDING },
                    onAuthSuccess = { currentScreen = AppScreen.MAIN }
                )
            }
            AppScreen.AUTH_SIGNUP -> {
                AuthScreen(
                    initialIsSignUp = true,
                    onBack = { currentScreen = AppScreen.ONBOARDING },
                    onAuthSuccess = { currentScreen = AppScreen.MAIN }
                )
            }
            AppScreen.MAIN -> {
                // MainWrapper Scaffold with persistent bottomNavigationBar and IndexedStack
                MainWrapper(
                    repository = repository,
                    onSignOut = {
                        currentScreen = AppScreen.AUTH_LOGIN
                    }
                )
            }
        }
    }
}

/**
 * MainWrapper: Root Scaffold that permanently anchors KubeBottomNav in its
 * bottomNavigationBar property, hosting an IndexedStack body that preserves state
 * across all 5 core tabs (Home, Delivery, Map, Wallet, Profile).
 */
@Composable
fun MainWrapper(
    repository: OrderRepository,
    onSignOut: () -> Unit
) {
    var selectedNavTab by rememberSaveable { mutableStateOf(KubeNavTab.HOME) }
    var selectedOrderId by rememberSaveable { mutableStateOf<String?>("ESB-00128") }
    var activeDeliveryFilter by rememberSaveable { mutableStateOf<DeliveryStatus?>(null) }

    // Back button inside MainWrapper: returns to Home tab if not already on Home
    if (selectedNavTab != KubeNavTab.HOME) {
        BackHandler {
            selectedNavTab = KubeNavTab.HOME
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        bottomBar = {
            // Permanent Bottom Navigation Bar anchored in Scaffold.bottomBar
            KubeBottomNav(
                selectedTab = selectedNavTab,
                onTabSelected = { tab ->
                    if (tab == KubeNavTab.DELIVERY) {
                        activeDeliveryFilter = null
                    }
                    // Tab change ONLY updates the state index without pushing new routes
                    selectedNavTab = tab
                }
            )
        }
    ) { _ ->
        // ExtendBody: true (Jetpack Compose implementation)
        // Body fills the full screen and content flows behind the floating GlassContainer Bottom Bar
        // IndexedStack Equivalent: Preserves scroll positions, filters, and state across tabs
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Tab 0: Home (Dashboard)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (selectedNavTab == KubeNavTab.HOME) 1f else 0f)
                    .graphicsLayer {
                        alpha = if (selectedNavTab == KubeNavTab.HOME) 1f else 0f
                    }
            ) {
                DashboardScreen(
                    repository = repository,
                    onNavigateToDelivery = { filter ->
                        activeDeliveryFilter = filter
                        selectedNavTab = KubeNavTab.DELIVERY
                    },
                    onNavigateToProfile = {
                        selectedNavTab = KubeNavTab.PROFILE
                    }
                )
            }

            // Tab 1: Delivery List
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (selectedNavTab == KubeNavTab.DELIVERY) 1f else 0f)
                    .graphicsLayer {
                        alpha = if (selectedNavTab == KubeNavTab.DELIVERY) 1f else 0f
                    }
            ) {
                DeliveryListScreen(
                    repository = repository,
                    initialFilter = activeDeliveryFilter,
                    onSelectOrder = { orderId ->
                        selectedOrderId = orderId
                        selectedNavTab = KubeNavTab.MAP
                    },
                    onNavigateToProfile = {
                        selectedNavTab = KubeNavTab.PROFILE
                    }
                )
            }

            // Tab 2: Map & Tracking
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (selectedNavTab == KubeNavTab.MAP) 1f else 0f)
                    .graphicsLayer {
                        alpha = if (selectedNavTab == KubeNavTab.MAP) 1f else 0f
                    }
            ) {
                DeliveryMapScreen(
                    orderId = selectedOrderId,
                    repository = repository,
                    onNavigateBack = {
                        selectedNavTab = KubeNavTab.DELIVERY
                    },
                    onNavigateToProfile = {
                        selectedNavTab = KubeNavTab.PROFILE
                    }
                )
            }

            // Tab 3: Wallet
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (selectedNavTab == KubeNavTab.WALLET) 1f else 0f)
                    .graphicsLayer {
                        alpha = if (selectedNavTab == KubeNavTab.WALLET) 1f else 0f
                    }
            ) {
                WalletScreen(
                    repository = repository,
                    onNavigateToProfile = {
                        selectedNavTab = KubeNavTab.PROFILE
                    }
                )
            }

            // Tab 4: Profile
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (selectedNavTab == KubeNavTab.PROFILE) 1f else 0f)
                    .graphicsLayer {
                        alpha = if (selectedNavTab == KubeNavTab.PROFILE) 1f else 0f
                    }
            ) {
                ProfileScreen(
                    repository = repository,
                    onSignOut = onSignOut
                )
            }
        }
    }
}
