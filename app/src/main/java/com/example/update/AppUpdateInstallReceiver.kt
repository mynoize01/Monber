package com.example.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class PackageInstallEvent {
    data object PromptLaunched : PackageInstallEvent()
    data object Succeeded : PackageInstallEvent()
    data class Failed(val statusCode: Int, val message: String) : PackageInstallEvent()
}

class AppUpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val rawMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = IntentCompat.getParcelableExtra(
                    intent,
                    Intent.EXTRA_INTENT,
                    Intent::class.java
                )
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirmIntent)
                        _events.tryEmit(PackageInstallEvent.PromptLaunched)
                    } catch (e: Exception) {
                        _events.tryEmit(
                            PackageInstallEvent.Failed(
                                status,
                                e.message ?: "Gagal membuka dialog instalasi sistem."
                            )
                        )
                    }
                } else {
                    _events.tryEmit(
                        PackageInstallEvent.Failed(
                            status,
                            "Intent konfirmasi instalasi tidak tersedia."
                        )
                    )
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                _events.tryEmit(PackageInstallEvent.Succeeded)
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                _events.tryEmit(
                    PackageInstallEvent.Failed(status, "Instalasi pembaruan dibatalkan.")
                )
            }

            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                _events.tryEmit(
                    PackageInstallEvent.Failed(
                        status,
                        "Gagal memasang APK: tanda tangan aplikasi berbeda dengan versi yang terpasang."
                    )
                )
            }

            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                _events.tryEmit(
                    PackageInstallEvent.Failed(
                        status,
                        "Ruang penyimpanan tidak cukup untuk memasang pembaruan."
                    )
                )
            }

            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                _events.tryEmit(
                    PackageInstallEvent.Failed(
                        status,
                        "APK pembaruan tidak kompatibel dengan perangkat ini."
                    )
                )
            }

            else -> {
                val errorText = if (rawMessage.isNotBlank()) {
                    "Gagal memasang pembaruan: $rawMessage"
                } else {
                    "Gagal memasang pembaruan aplikasi (kode $status)."
                }
                _events.tryEmit(PackageInstallEvent.Failed(status, errorText))
            }
        }
    }

    companion object {
        private val _events = MutableSharedFlow<PackageInstallEvent>(extraBufferCapacity = 8)
        val events: SharedFlow<PackageInstallEvent> = _events.asSharedFlow()
    }
}
