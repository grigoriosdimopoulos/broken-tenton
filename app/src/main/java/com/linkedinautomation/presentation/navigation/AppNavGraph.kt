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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.linkedinautomation.presentation.activitydetail.ActivityDetailScreen
import com.linkedinautomation.presentation.applicationdetail.ApplicationDetailScreen
import com.linkedinautomation.presentation.linkedinlogin.LinkedInLoginScreen
import com.linkedinautomation.presentation.settings.SettingsViewModel
import com.linkedinautomation.presentation.settings.screens.*
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
                val settingsVm: com.linkedinautomation.presentation.settings.SettingsViewModel = hiltViewModel()
                val importResult by settingsVm.importResult.collectAsState()
                LaunchedEffect(importResult) {
                    if (importResult == true) {
                        settingsVm.clearBackupState()
                        // After restore: go through setup (pre-filled) rather than jumping to Dashboard
                        navController.navigate(Screen.SourceMode.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = false }
                        }
                    }
                }
                WelcomeScreen(
                    onNext = { navController.navigate(Screen.SourceMode.route) },
                    onImportRequest = { uri -> settingsVm.importSettings(uri) },
                    importState = importResult
                )
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
                LinkedInLoginScreen(
                    onSuccess = { navController.navigate(Screen.PersonalInfo.route) },
                    onBack = { navController.popBackStack() }
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
                    onNext = { navController.navigate(Screen.PersonalInfo.route) }
                )
            }
            composable(Screen.PersonalInfo.route) {
                val prefs by setupVm.prefs.collectAsState()
                PersonalInfoScreen(
                    firstName = prefs.firstName,
                    lastName = prefs.lastName,
                    phone = prefs.phone,
                    city = prefs.city,
                    country = prefs.country,
                    linkedInUrl = prefs.linkedInUrl,
                    currentJobTitle = prefs.currentJobTitle,
                    yearsOfExperience = prefs.yearsOfExperience,
                    email = prefs.email,
                    onSave = { fn, ln, ph, ct, co, li, jt, yoe, em ->
                        setupVm.setPersonalInfo(fn, ln, ph, ct, co, li, jt, yoe, em)
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
                    minSalary = prefs.minSalary,
                    onKeywordsChange = { setupVm.setJobKeywords(it) },
                    onLocationChange = { setupVm.setLocation(it, prefs.remoteOnly, prefs.hybridOk, prefs.onsiteOk) },
                    onRemoteChange = { setupVm.setLocation(prefs.location, it, prefs.hybridOk, prefs.onsiteOk) },
                    onHybridChange = { setupVm.setLocation(prefs.location, prefs.remoteOnly, it, prefs.onsiteOk) },
                    onOnsiteChange = { setupVm.setLocation(prefs.location, prefs.remoteOnly, prefs.hybridOk, it) },
                    onMinSalaryChange = { setupVm.setMinSalary(it) },
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
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onApplicationClick = { id ->
                        navController.navigate(Screen.ApplicationDetail.route(id))
                    },
                    onGoToPersonalInfo = {
                        navController.navigate(Screen.SettingsPersonalInfo.route)
                    }
                )
            }
            composable(Screen.ApprovalQueue.route) { ApprovalQueueScreen() }
            composable(Screen.Monitor.route) { MonitorScreen() }
            composable(Screen.History.route) {
                HistoryScreen(
                    onApplicationClick = { id ->
                        navController.navigate(Screen.ApplicationDetail.route(id))
                    },
                    onActivityClick = { logId ->
                        navController.navigate(Screen.ActivityDetail.route(logId))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToPersona = { navController.navigate(Screen.ClaudePersona.route) },
                    onNavigateToAccount = { navController.navigate(Screen.SettingsCredentials.route) },
                    onNavigateToJobPrefs = { navController.navigate(Screen.SettingsJobPrefs.route) },
                    onNavigateToResume = { navController.navigate(Screen.SettingsResume.route) },
                    onNavigateToPersonalInfo = { navController.navigate(Screen.SettingsPersonalInfo.route) },
                    onNavigateToBackup = { navController.navigate(Screen.SettingsBackup.route) }
                )
            }

            // ── Settings Sub-screens ────────────────────────────────────
            composable(Screen.ClaudePersona.route) {
                ClaudePersonaScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsPersonalInfo.route) {
                SettingsPersonalInfoScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsCredentials.route) {
                SettingsCredentialsScreen(
                    onSignIn = { navController.navigate(Screen.LinkedInLogin.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SettingsJobPrefs.route) {
                SettingsJobPrefsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsResume.route) {
                SettingsResumeScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsBackup.route) {
                SettingsBackupScreen(onBack = { navController.popBackStack() })
            }

            // ── LinkedIn Login (cookie-based) ───────────────────────────
            composable(Screen.LinkedInLogin.route) {
                LinkedInLoginScreen(
                    onSuccess = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Application Detail ──────────────────────────────────────
            composable(
                route = Screen.ApplicationDetail.route,
                arguments = listOf(navArgument("appId") { type = NavType.LongType })
            ) {
                ApplicationDetailScreen(onBack = { navController.popBackStack() })
            }

            // ── Activity Log Detail ─────────────────────────────────────
            composable(
                route = Screen.ActivityDetail.route,
                arguments = listOf(navArgument("logId") { type = NavType.LongType })
            ) {
                ActivityDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
