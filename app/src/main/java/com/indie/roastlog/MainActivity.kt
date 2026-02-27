package com.indie.roastlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.indie.roastlog.ui.screens.RoastingFormScreen
import com.indie.roastlog.ui.theme.RoastLogTheme

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
    RoastingFormScreen()
}

@Preview(showBackground = true)
@Composable
fun RoastLogAppPreview() {
    RoastLogTheme {
        RoastLogApp()
    }
}
