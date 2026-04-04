package com.demo.grpcdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Speed
import com.demo.grpcdemo.ui.CatalogScreen
import com.demo.grpcdemo.ui.CatalogViewModel
import com.demo.grpcdemo.ui.comparison.ComparisonScreen
import com.demo.grpcdemo.ui.comparison.ComparisonViewModel
import dagger.hilt.android.AndroidEntryPoint

private data class NavItem(val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem("Catalog",     Icons.Filled.List),
    NavItem("REST vs gRPC", Icons.Filled.Speed),
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val catalogViewModel:    CatalogViewModel    by viewModels()
    private val comparisonViewModel: ComparisonViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                var selectedTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    bottomBar = {
                        NavigationBar {
                            navItems.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick  = { selectedTab = index },
                                    icon     = { Icon(item.icon, contentDescription = item.label) },
                                    label    = { Text(item.label) },
                                )
                            }
                        }
                    },
                ) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        when (selectedTab) {
                            0 -> CatalogScreen(viewModel = catalogViewModel)
                            1 -> ComparisonScreen(viewModel = comparisonViewModel)
                        }
                    }
                }
            }
        }
    }
}

