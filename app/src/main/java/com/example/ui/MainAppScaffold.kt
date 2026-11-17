package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.auth.signOutUser
import com.example.ui.screens.NewSampleScreen
import com.example.ui.screens.PromoterAnalysisScreen
import com.example.ui.screens.SavedSamplesScreen
import com.example.ui.screens.SequenceAnalysisScreen
import com.example.ui.screens.WeatherDetailsScreen
import com.example.ui.viewmodel.RiceLoggerViewModel
import com.google.firebase.auth.FirebaseUser

object Routes {
    const val NEW_SAMPLE = "new_sample"
    const val SEQUENCE_ANALYSIS = "sequence_analysis"
    const val SAVED_SAMPLES = "saved_samples"
    const val WEATHER_DETAILS = "weather_details"
    const val PROMOTER_DRAFT = "promoter_analysis_draft"
    const val PROMOTER_SAVED = "promoter_analysis_saved/{sampleId}"

    fun promoterSaved(sampleId: Long) = "promoter_analysis_saved/$sampleId"
}

private data class TopLevelTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    user: FirebaseUser?,
    customEmail: String? = null,
    viewModel: RiceLoggerViewModel,
    onGuestSignOut: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route

    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val savedSamples by viewModel.savedSamples.collectAsStateWithLifecycle()
    val filterOptions by viewModel.filterVarietyOptions.collectAsStateWithLifecycle()
    val selectedVariety by viewModel.selectedVarietyFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var showAccountDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        TopLevelTab(
            route = Routes.NEW_SAMPLE,
            label = stringResource(R.string.nav_new_sample),
            selectedIcon = Icons.Filled.AddCircle,
            unselectedIcon = Icons.Outlined.AddCircleOutline
        ),
        TopLevelTab(
            route = Routes.SEQUENCE_ANALYSIS,
            label = stringResource(R.string.nav_analysis),
            selectedIcon = Icons.Filled.Biotech,
            unselectedIcon = Icons.Outlined.Biotech
        ),
        TopLevelTab(
            route = Routes.SAVED_SAMPLES,
            label = stringResource(R.string.nav_saved),
            selectedIcon = Icons.Filled.FolderSpecial,
            unselectedIcon = Icons.Outlined.FolderSpecial
        )
    )

    val isTopLevelRoute = tabs.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                if (isTopLevelRoute) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = when (currentRoute) {
                                        Routes.SEQUENCE_ANALYSIS -> "FASTA & Sequence Lab"
                                        Routes.SAVED_SAMPLES -> "Saved Field Samples (${savedSamples.size})"
                                        else -> stringResource(R.string.app_name)
                                    },
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = stringResource(R.string.app_subtitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showAccountDialog = true },
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .testTag("researcher_account_button")
                            ) {
                                ResearcherAvatar(user = user, customEmail = customEmail)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            bottomBar = {
                if (isTopLevelRoute && !useNavRail) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelRoute && useNavRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.height(12.dp))
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.NEW_SAMPLE
                    ) {
                        composable(Routes.NEW_SAMPLE) {
                            NewSampleScreen(
                                viewModel = viewModel,
                                draft = draft,
                                onOpenWeatherDetails = {
                                    navController.navigate(Routes.WEATHER_DETAILS)
                                },
                                onOpenSequenceAnalysis = {
                                    navigateToTab(Routes.SEQUENCE_ANALYSIS)
                                },
                                onOpenPromoterAnalysis = {
                                    navController.navigate(Routes.PROMOTER_DRAFT)
                                },
                                onSampleSaved = {
                                    navigateToTab(Routes.SAVED_SAMPLES)
                                }
                            )
                        }

                        composable(Routes.SEQUENCE_ANALYSIS) {
                            SequenceAnalysisScreen(
                                viewModel = viewModel,
                                analysis = draft.fastaAnalysis,
                                fastaStatus = draft.fastaStatus,
                                onOpenPromoterAnalysis = {
                                    navController.navigate(Routes.PROMOTER_DRAFT)
                                }
                            )
                        }

                        composable(Routes.SAVED_SAMPLES) {
                            SavedSamplesScreen(
                                viewModel = viewModel,
                                samples = savedSamples,
                                varietyOptions = filterOptions,
                                selectedVariety = selectedVariety,
                                searchQuery = searchQuery,
                                onOpenSamplePromoterReport = { sampleId ->
                                    navController.navigate(Routes.promoterSaved(sampleId))
                                }
                            )
                        }

                        composable(Routes.WEATHER_DETAILS) {
                            WeatherDetailsScreen(
                                latitude = draft.latitude,
                                longitude = draft.longitude,
                                weather = draft.weather,
                                weatherStatus = draft.weatherStatus,
                                onRefreshWeather = viewModel::fetchWeatherForCurrentLocation,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.PROMOTER_DRAFT) {
                            PromoterAnalysisScreen(
                                analysis = draft.fastaAnalysis,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Routes.PROMOTER_SAVED,
                            arguments = listOf(navArgument("sampleId") { type = NavType.LongType })
                        ) { entry ->
                            val sampleId = entry.arguments?.getLong("sampleId") ?: 0L
                            val entity by viewModel.observeSampleById(sampleId)
                                .collectAsStateWithLifecycle(initialValue = null)
                            val analysis = remember(entity) {
                                entity?.let { viewModel.entityToAnalysis(it) }
                            }
                            PromoterAnalysisScreen(
                                analysis = analysis,
                                titleOverride = entity?.let { "${it.sampleId} (${it.riceVariety})" },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("Researcher Account") },
            text = {
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ResearcherAvatar(user = user, customEmail = customEmail, size = 44)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = user?.displayName ?: customEmail ?: "Field Researcher",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = user?.email ?: customEmail ?: user?.uid ?: "Guest / Offline Session",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Your Room field sample logs are tagged to your current researcher session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccountDialog = false
                        signOutUser(
                            credentialManager = credentialManager,
                            scope = scope
                        )
                        onGuestSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ResearcherAvatar(user: FirebaseUser?, customEmail: String? = null, size: Int = 34) {
    val photoUrl = user?.photoUrl
    if (photoUrl != null) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Researcher profile photo",
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val initials = (user?.displayName ?: user?.email ?: customEmail ?: "R")
            .trim()
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString() ?: "R"
        Surface(
            modifier = Modifier.size(size.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
