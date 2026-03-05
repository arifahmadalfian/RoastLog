package com.indie.roastlog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ScaffoldCustom(
    title: String,
    floatingActionButton: @Composable (() -> Unit) = {},
    snackbarHost: @Composable (() -> Unit) = {},
    content: @Composable () -> Unit
) {
    Scaffold(
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Custom Header using Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Invoke content with 0 padding since we handle padding in the parent Column
            content()
        }
    }
}
