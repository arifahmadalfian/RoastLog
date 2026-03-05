package com.indie.roastlog.ui.screens.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.indie.roastlog.ui.components.ScaffoldCustom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoastingDetail(
    roastId: String?
) {
    ScaffoldCustom(
        title = "Detail Roasting"
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Detail Roasting ID: $roastId (Halaman Kosong)")
        }
    }
}
