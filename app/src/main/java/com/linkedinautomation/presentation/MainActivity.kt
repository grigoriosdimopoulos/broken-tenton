package com.linkedinautomation.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.linkedinautomation.presentation.navigation.AppNavGraph
import com.linkedinautomation.presentation.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val isSetupComplete by viewModel.isSetupComplete.collectAsState(false)
                val pendingCount by viewModel.pendingCount.collectAsState(0)
                AppNavGraph(
                    isSetupComplete = isSetupComplete,
                    pendingCount = pendingCount
                )
            }
        }
    }
}
