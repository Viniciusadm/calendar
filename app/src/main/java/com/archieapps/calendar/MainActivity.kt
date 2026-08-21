package com.archieapps.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.core.store.Settings
import com.archieapps.calendar.design.CalendarTheme
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.feature.calendar.CalendarViewModel
import com.archieapps.calendar.feature.calendar.MonthScreen
import com.archieapps.calendar.feature.calendar.SetupScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalendarTheme {
                CalendarApp()
            }
        }
    }
}

@Composable
private fun CalendarApp() {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    var configured by remember { mutableStateOf(settings.isConfigured) }
    val colors = LocalChronicle.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.ground,
    ) { insets ->
        Box(Modifier.padding(insets)) {
            if (!configured) {
                SetupScreen(
                    initialBaseUrl = settings.baseUrl,
                    onSave = { baseUrl, token ->
                        settings.baseUrl = baseUrl
                        settings.token = token
                        configured = true
                    },
                )
            } else {
                Chronicle(settings)
            }
        }
    }
}

@Composable
private fun Chronicle(settings: Settings) {
    val viewModel: CalendarViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CalendarViewModel(CalendarApi(settings)) as T
        }
    )

    val state by viewModel.state.collectAsState()

    MonthScreen(
        state = state,
        onSelect = viewModel::select,
        onShiftMonth = viewModel::goToMonth,
        onToday = viewModel::today,
        onRetry = viewModel::load,
    )
}
