package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import com.example.update.AppUpdateManager

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val shouldOpenUpdate = intent?.getBooleanExtra("open_update_dialog", false) == true
    val justUpdated = intent?.getBooleanExtra("just_updated", false) == true
    if (justUpdated) {
      AppUpdateManager.dismissPostUpdateNotification(this)
    }
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
          LaunchedEffect(justUpdated) {
            if (justUpdated) {
              Toast.makeText(
                this@MainActivity,
                "MonBer berhasil diperbarui ke v${BuildConfig.VERSION_NAME}",
                Toast.LENGTH_SHORT
              ).show()
            }
          }
          DashboardScreen(viewModel = customerViewModel)
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    AppUpdateManager.dismissPostUpdateNotification(this)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.getBooleanExtra("open_update_dialog", false)) {
      val vm = ViewModelProvider(this)[CustomerViewModel::class.java]
      vm.openUpdateDialog()
      vm.checkForUpdates(autoInstallOnFind = false, isAutoBackgroundCheck = false)
    }
    if (intent.getBooleanExtra("just_updated", false)) {
      AppUpdateManager.dismissPostUpdateNotification(this)
      Toast.makeText(
        this,
        "MonBer berhasil diperbarui ke v${BuildConfig.VERSION_NAME}",
        Toast.LENGTH_SHORT
      ).show()
    }
  }
}
