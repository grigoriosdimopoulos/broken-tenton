package com.linkedinautomation.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.linkedinautomation.domain.model.*
import com.linkedinautomation.presentation.approvalqueue.ApprovalQueueScreen
import com.linkedinautomation.presentation.dashboard.DashboardScreen
import com.linkedinautomation.presentation.history.HistoryScreen
import com.linkedinautomation.presentation.monitor.MonitorScreen
import com.linkedinautomation.presentation.settings.SettingsViewModel
import com.linkedinautomation.presentation.settings.screens.ClaudePersonaScreen
import com.linkedinautomation.presentation.settings.screens.SettingsScreen
import com.linkedinautomation.presentation.setup.SetupViewModel
import com.linkedinautomation.presentation.setup.screens.*

@Composable
fun AppNavGraph(isSetupComplete: Boolean, pendingCount: Int) {
    val navController = rememberNavController()
    val startDest = if (isSetupComplete) Screen.Dashboard.route else Screen.Welcome.route

    val mainTabs = listOf(
        Triple(Screen.Dashboard.route, Icons.Default.Home, "Dashboard"),
        Triple(Screen.ApprovalQueue.route, Icons.Default.Inbox, "Queue"),
        Triple(Screen.Monitor.route, Icons.Default.Monitor, "Monitor"),
        Triple(Screen.History.route, Icons.Default.History, "History"),
        Triple(Screen.Settings.route, Icons.Default.Settings, "Settings")
    )
    val mainRoutes = mainTabs.map { it.first }.toSet()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in mainRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    mainTabs.forEach { (route, icon, label) ->
                        val badge = if (route == Screen.ApprovalQueue.route && pendingCount > 0) pendingCount else 0
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (badge > 0) {
                                    BadgedBox(badge = { Badge { Text(badge.toString()) } }) {
                                        Icon(icon, label)
                                    }
                                } else {
                                    Icon(icon, label)
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        val setupVm: SetupViewModel = hiltViewModel()

        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── Setup Flow ──────────────────────────────────────────────
            composable(Screen.Welcome.route) {
                WelcomeScreen(onNext = { navController.navigate(Screen.SourceMode.route) })
            }
            composable(Screen.SourceMode.route) {
                val prefs by setupVm.prefs.collectAsState()
                SourceModeScreen(
                    selected = prefs.sourceMode,
                    onSelect = { setupVm.setSourceMode(it) },
                    onNext = {
                        if (prefs.sourceMode == SourceMode.LINKEDIN)
                            navController.navigate(Screen.Credentials.route)
                        else
                            navController.navigate(Screen.JobBoardSelection.route)
                    }
                )
            }
            composable(Screen.Credentials.route) {
                val prefs by setupVm.prefs.collectAsState()
                CredentialsScreen(
                    email = prefs.linkedInEmail,
                    password = prefs.linkedInPassword,
                    onEmailChange = { setupVm.setCredentials(it, prefs.linkedInPassword) },
                    onPasswordChange = { setupVm.setCredentials(prefs.linkedInEmail, it) },
                    onNext = { navController.navigate(Screen.JobPreferences.route) }
                )
            }
            composable(Screen.JobBoardSelection.route) {
                val prefs by setupVm.prefs.collectAsState()
                JobBoardSelectionScreen(
                    selected = prefs.selectedJobBoards,
                    onToggle = { board ->
                        val current = prefs.selectedJobBoards.toMutableList()
                        if (current.contains(board)) current.remove(board) else current.add(board)
                        setupVm.setSelectedJobBoards(current)
                    },
                    onNext = { navController.navigate(Screen.JobPreferences.route) }
                )
            }
            composable(Screen.JobPreferences.route) {
                val prefs by setupVm.prefs.collectAsState()
                JobPreferencesScreen(
                    keywords = prefs.jobKeywords,
                    location = prefs.location,
                    remoteOnly = prefs.remoteOnly,
                    hybridOk = prefs.hybridOk,
                    onsiteOk = prefs.onsiteOk,
                    onKeywordsChange = { setupVm.setJobKeywords(it) },
                    onLocationChange = { setupVm.setLocation(it, prefs.remoteOnly, prefs.hybridOk, prefs.onsiteOk) },
                    onRemoteChange = { setupVm.setLocation(prefs.location, it, prefs.hybridOk, prefs.onsiteOk) },
                    onHybridChange = { setupVm.setLocation(prefs.location, prefs.remoteOnly, it, prefs.onsiteOk) },
                    onOnsiteChange = { setupVm.setLocation(prefs.location, prefs.remoteOnly, prefs.hybridOk, it) },
                    onNext = { navController.navigate(Screen.Exclusions.route) }
                )
            }
            composable(Screen.Exclusions.route) {
                val prefs by setupVm.prefs.collectAsState()
                ExclusionsScreen(
                    excludeKeywords = prefs.excludeKeywords,
                    excludeCompanies = prefs.excludeCompanies,
                    onExcludeKeywordsChange = { setupVm.setExclusions(it, prefs.excludeCompanies) },
                    onExcludeCompaniesChange = { setupVm.setExclusions(prefs.excludeKeywords, it) },
                    onNext = { navController.navigate(Screen.ExperienceBio.route) }
                )
            }
            composable(Screen.ExperienceBio.route) {
                val prefs by setupVm.prefs.collectAsState()
                ExperienceBioScreen(
                    bio = prefs.experienceBio,
                    onBioChange = { setupVm.setExperienceBio(it) },
                    onNext = { navController.navigate(Screen.ClaudeSetup.route) }
                )
            }
            composable(Screen.ClaudeSetup.route) {
                val prefs by setupVm.prefs.collectAsState()
                ClaudeSetupScreen(
                    apiKey = prefs.claudeApiKey,
                    persona = prefs.claudePersona,
                    onApiKeyChange = { setupVm.setClaudeSetup(it, prefs.claudePersona) },
                    onPersonaChange = { setupVm.setClaudeSetup(prefs.claudeApiKey, it) },
                    onNext = { navController.navigate(Screen.Resume.route) }
                )
            }
            composable(Screen.Resume.route) {
                val prefs by setupVm.prefs.collectAsState()
                ResumeScreen(
                    resumeFileName = prefs.resumeFileName,
                    onResumePicked = { uri -> setupVm.copyResume(uri) },
                    onNext = { navController.navigate(Screen.SetupComplete.route) }
                )
            }
            composable(Screen.SetupComplete.route) {
                val prefs by setupVm.prefs.collectAsState()
                val done by setupVm.setupDone.collectAsState()
                LaunchedEffect(done) {
                    if (done) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                }
                SetupCompleteScreen(prefs = prefs, onStart = { setupVm.complete() })
            }

            // ── Main Tabs ───────────────────────────────────────────────
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.ApprovalQueue.route) { ApprovalQueueScreen() }
            composable(Screen.Monitor.route) { MonitorScreen() }
            composable(Screen.History.route) { HistoryScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToPersona = { navController.navigate(Screen.ClaudePersona.route) },
                    onNavigateToAccount = { /* TODO: account settings */ },
                    onNavigateToJobPrefs = { /* TODO: job pref settings */ },
                    onNavigateToResume = { /* TODO: resume settings */ }
                )
            }

            // ── Settings Sub-screens ────────────────────────────────────
            composable(Screen.ClaudePersona.route) {
                ClaudePersonaScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
