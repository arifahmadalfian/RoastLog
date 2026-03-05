package com.indie.roastlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.indie.roastlog.ui.screens.detail.RoastingDetail
import com.indie.roastlog.ui.screens.form.RoastingFormScreen
import com.indie.roastlog.ui.screens.form.RoastingFormState
import com.indie.roastlog.ui.screens.history.RoastingHistoryScreen
import com.indie.roastlog.ui.screens.running.RoastingRunScreen
import com.indie.roastlog.ui.theme.RoastLogTheme
import kotlinx.serialization.Serializable

// Definisikan Route secara Type-Safe dan implementasi NavKey untuk Navigation 3
@Serializable object HistoryRoute : NavKey
@Serializable object FormRoute : NavKey
@Serializable data class RunRoute(val state: RoastingFormState) : NavKey
@Serializable data class DetailRoute(val id: String) : NavKey

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle =  SystemBarStyle.light(
                (Color.Transparent).toArgb(),
                (Color.Transparent).toArgb()
            ),
            navigationBarStyle = SystemBarStyle.light(
                (Color.Transparent).toArgb(),
                (Color.Transparent).toArgb()
            )
        )
        setContent {
            RoastLogTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RoastLogApp()
                }
            }
        }
    }
}

@Composable
fun RoastLogApp() {
    val navigationState = rememberNavigationState(
        startRoute = HistoryRoute,
        topLevelRoutes = setOf(HistoryRoute)
    )
    val navigator = remember { Navigator(navigationState) }

    val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<HistoryRoute> {
            RoastingHistoryScreen(
                onAddClick = { navigator.navigate(FormRoute) },
                onItemClick = { id -> navigator.navigate(DetailRoute(id)) }
            )
        }
        
        entry<FormRoute> {
            RoastingFormScreen(
                onStartRoast = { formState ->
                    navigator.navigate(RunRoute(formState))
                }
            )
        }
        
        entry<RunRoute> { key ->
            RoastingRunScreen(
                onFinish = { navigator.navigate(HistoryRoute) },
                formState = key.state
            )
        }
        
        entry<DetailRoute> { key ->
            RoastingDetail(
                roastId = key.id,
                onBack = { navigator.goBack() }
            )
        }
    }

    NavDisplay(
        entries = navigationState.toEntries(entryProvider),
        onBack = { navigator.goBack() }
    )
}

@Preview(showBackground = true)
@Composable
fun RoastLogAppPreview() {
    RoastLogTheme {
        RoastLogApp()
    }
}
