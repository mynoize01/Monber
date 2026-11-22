package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CustomerViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val shouldOpenUpdate = intent?.getBooleanExtra("open_update_dialog", false) == true
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize()
        ) {
          val customerViewModel: CustomerViewModel = viewModel()
          LaunchedEffect(shouldOpenUpdate) {
            if (shouldOpenUpdate) {
              customerViewModel.openUpdateDialog()
              customerViewModel.checkForUpdates(autoInstallOnFind = false, isAutoBackgroundCheck = false)
            }
          }
          DashboardScreen(viewModel = customerViewModel)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.getBooleanExtra("open_update_dialog", false)) {
      val vm = ViewModelProvider(this)[CustomerViewModel::class.java]
      vm.openUpdateDialog()
      vm.checkForUpdates(autoInstallOnFind = false, isAutoBackgroundCheck = false)
    }
  }
}
