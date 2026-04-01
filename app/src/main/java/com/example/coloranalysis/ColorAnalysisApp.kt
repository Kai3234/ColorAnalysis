package com.example.coloranalysis

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.coloranalysis.ui.navigation.NavGraph
import com.example.coloranalysis.ui.theme.ColorAnalysisTheme

@Composable
fun ColorAnalysisApp() {
    ColorAnalysisTheme {
        val navController = rememberNavController()

        NavGraph(navController = navController)
    }
}

