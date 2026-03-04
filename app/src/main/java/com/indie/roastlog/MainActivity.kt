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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.indie.roastlog.ui.screens.detail.RoastingDetail
import com.indie.roastlog.ui.screens.form.RoastingFormScreen
import com.indie.roastlog.ui.screens.form.RoastingFormState
import com.indie.roastlog.ui.screens.history.RoastingHistoryScreen
import com.indie.roastlog.ui.screens.running.RoastingRunScreen
import com.indie.roastlog.ui.theme.RoastLogTheme
import kotlinx.serialization.Serializable

// Definisikan Route secara Type-Safe
@Serializable object HistoryRoute
@Serializable object FormRoute
@Serializable data class RunRoute(val state: RoastingFormState)
@Serializable data class DetailRoute(val id: String)

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
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = HistoryRoute) {
        composable<HistoryRoute> {
            RoastingHistoryScreen(
                onAddClick = { navController.navigate(FormRoute) },
                onItemClick = { id -> navController.navigate(DetailRoute(id)) }
            )
        }
        
        composable<FormRoute> {
            RoastingFormScreen(
                onStartRoast = { formState ->
                    navController.navigate(RunRoute(formState))
                }
            )
        }
        
        composable<RunRoute> { backStackEntry ->
            val route: RunRoute = backStackEntry.toRoute()
            RoastingRunScreen(formState = route.state)
        }
        
        composable<DetailRoute> { backStackEntry ->
            val route: DetailRoute = backStackEntry.toRoute()
            RoastingDetail(roastId = route.id)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoastLogAppPreview() {
    RoastLogTheme {
        RoastLogApp()
    }
}
