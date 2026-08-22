package com.archieapps.calendar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.archieapps.calendar.core.store.Settings
import com.archieapps.calendar.design.CalendarTheme
import com.archieapps.calendar.feature.calendar.WeekStart
import com.archieapps.calendar.feature.settings.readPreferences
import com.archieapps.calendar.feature.settings.theme
import com.archieapps.calendar.feature.settings.write
import com.archieapps.calendar.feature.widget.EXTRA_OCCURRENCE
import com.archieapps.calendar.feature.widget.WidgetBridge
import com.archieapps.calendar.ui.Root

class MainActivity : ComponentActivity() {
    private var launchedOccurrence by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        launchedOccurrence = intent.getStringExtra(EXTRA_OCCURRENCE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        launchedOccurrence = intent?.getStringExtra(EXTRA_OCCURRENCE)

        setContent {
            val context = LocalContext.current
            val settings = remember { Settings(context) }
            var preferences by remember { mutableStateOf(settings.readPreferences()) }

            LaunchedEffect(preferences.weekStartsMonday) {
                WeekStart.apply(preferences.weekStartsMonday)
            }

            CalendarTheme(mode = preferences.theme()) {
                Root(
                    settings = settings,
                    preferences = preferences,
                    focusOccurrence = launchedOccurrence,
                    onFocusHandled = { launchedOccurrence = null },
                    onPreferences = { transform ->
                        val next = transform(preferences)
                        val looked = next.themeMode != preferences.themeMode

                        settings.write(next)
                        preferences = next

                        if (looked) {
                            WidgetBridge.onLookChanged(context)
                        }
                    },
                )
            }
        }
    }
}
