package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.R
import com.example.ui.viewmodel.CustomerViewModel
import com.example.update.AppUpdateManager
import com.example.update.AppUpdateState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SettingsPopupPage {
    MENU,
    APP_PREFERENCES,
    HELP_GUIDE,
    ABOUT_MONBER
}

@Composable
fun SettingsMenuPopupDialog(
    viewModel: CustomerViewModel,
    context: Context,
    appUpdateState: AppUpdateState,
    notificationPermissionLauncher: ActivityResultLauncher<String>,
    installPermissionLauncher: ActivityResultLauncher<Intent>,
    onMarkManualUpdateCheck: () -> Unit,
    onPickOfflineFile: (String) -> Unit,
    onOpenEditWa: () -> Unit,
    onOpenLogin: () -> Unit,
    onConfirmLogoff: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onDismiss: () -> Unit
) {
    var currentPage by remember { mutableStateOf(SettingsPopupPage.MENU) }

    val loggedInPN by viewModel.loggedInPN.collectAsState()
    val loggedInMantriDetails by viewModel.loggedInMantriDetails.collectAsState()
    val selectedBottomNav by viewModel.selectedBottomNav.collectAsState()
    val lastSyncMetadata by viewModel.lastSyncMetadata.collectAsState()

    val isOfflineModeEnabled by viewModel.isOfflineModeEnabled.collectAsState()
    val offlineNameUrl1 by viewModel.offlineNameUrl1.collectAsState()
    val offlineNameUrl2 by viewModel.offlineNameUrl2.collectAsState()
    val offlineNameUrl3 by viewModel.offlineNameUrl3.collectAsState()
    val offlineNameUrl4 by viewModel.offlineNameUrl4.collectAsState()
    val offlineNameEUrl1 by viewModel.offlineNameEUrl1.collectAsState()
    val offlineNameEUrl2 by viewModel.offlineNameEUrl2.collectAsState()
    val offlineNameKUrl1 by viewModel.offlineNameKUrl1.collectAsState()
    val offlineNameDi319Url by viewModel.offlineNameDi319Url.collectAsState()
    val offlineNameLw325Daily by viewModel.offlineNameLw325Daily.collectAsState()

    val cleanPnDigits = remember(loggedInPN) {
        val raw = loggedInPN.trim()
        val digits = Regex("""\d{4,9}""").find(raw)?.value ?: raw
        digits.trimStart('0').ifEmpty { digits }
    }

    val displayName = remember(loggedInPN, loggedInMantriDetails) {
        when {
            loggedInPN.isBlank() -> "Masuk Akun PN"
            loggedInMantriDetails.contains(" - ") ->
                loggedInMantriDetails.substringAfter(" - ").trim().ifBlank { "Mantri BRI" }
            loggedInPN.trim() == "207803" -> "Nurul Mubin"
            loggedInMantriDetails.isNotBlank() && loggedInMantriDetails != loggedInPN ->
                loggedInMantriDetails.trim()
            else -> "Mantri BRI"
        }
    }

    val displaySubtitle = remember(loggedInPN, cleanPnDigits) {
        if (loggedInPN.isNotBlank()) {
            "Mantri • PN $cleanPnDigits"
        } else {
            "Ketuk untuk Login PN"
        }
    }

    val lastSyncText = remember(lastSyncMetadata) {
        val ts = lastSyncMetadata?.lastSyncedAt ?: 0L
        if (ts > 0L) {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(ts))
        } else {
            "Belum ada sinkronisasi"
        }
    }

    Dialog(
        onDismissRequest = {
            if (currentPage != SettingsPopupPage.MENU) {
                currentPage = SettingsPopupPage.MENU
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(enabled = currentPage != SettingsPopupPage.MENU) {
            currentPage = SettingsPopupPage.MENU
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 420.dp)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("settings_menu_popup_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Curved Layered Wave Header (BRI Blue Wave + White Pocket + Avatar + BRI Mark)
                SettingsWaveProfileHeader(
                    displayName = displayName,
                    subtitle = displaySubtitle,
                    onProfileClick = onOpenLogin
                )

                // Body Section (Main Menu or Sub-pages)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            if (targetState != SettingsPopupPage.MENU) {
                                (slideInHorizontally(tween(220)) { it / 3 } + fadeIn(tween(200))) togetherWith
                                    (slideOutHorizontally(tween(180)) { -it / 4 } + fadeOut(tween(150)))
                            } else {
                                (slideInHorizontally(tween(220)) { -it / 3 } + fadeIn(tween(200))) togetherWith
                                    (slideOutHorizontally(tween(180)) { it / 4 } + fadeOut(tween(150)))
                            }
                        },
                        label = "settings_popup_page"
                    ) { page ->
                        when (page) {
                            SettingsPopupPage.MENU -> {
                                MainPopupMenuList(
                                    selectedBottomNav = selectedBottomNav,
                                    loggedInPN = loggedInPN,
                                    hasAvailableUpdate = appUpdateState is AppUpdateState.UpdateAvailable ||
                                        appUpdateState is AppUpdateState.ReadyToInstall ||
                                        appUpdateState is AppUpdateState.NeedsInstallPermission,
                                    onSelectBeranda = {
                                        viewModel.setBottomNav("DASHBOARD")
                                        onDismiss()
                                    },
                                    onSelectNasabah = {
                                        val targetTab = if (loggedInPN.isNotBlank()) "KELOLAAN" else "MONITORING"
                                        viewModel.setBottomNav(targetTab)
                                        onDismiss()
                                    },
                                    onSelectAnalitik = {
                                        onDismiss()
                                        onOpenAnalytics()
                                    },
                                    onSelectAppPreferences = {
                                        currentPage = SettingsPopupPage.APP_PREFERENCES
                                    },
                                    onSelectHelpGuide = {
                                        currentPage = SettingsPopupPage.HELP_GUIDE
                                    },
                                    onSelectAbout = {
                                        currentPage = SettingsPopupPage.ABOUT_MONBER
                                    }
                                )
                            }

                            SettingsPopupPage.APP_PREFERENCES -> {
                                AppPreferencesSubPage(
                                    viewModel = viewModel,
                                    context = context,
                                    isOfflineModeEnabled = isOfflineModeEnabled,
                                    offlineNames = mapOf(
                                        "url_1" to offlineNameUrl1,
                                        "url_2" to offlineNameUrl2,
                                        "url_3" to offlineNameUrl3,
                                        "url_4" to offlineNameUrl4,
                                        "e_url_1" to offlineNameEUrl1,
                                        "e_url_2" to offlineNameEUrl2,
                                        "k_url_1" to offlineNameKUrl1,
                                        "di319_url" to offlineNameDi319Url,
                                        "lw325_daily_url" to offlineNameLw325Daily
                                    ),
                                    appUpdateState = appUpdateState,
                                    notificationPermissionLauncher = notificationPermissionLauncher,
                                    installPermissionLauncher = installPermissionLauncher,
                                    onMarkManualUpdateCheck = onMarkManualUpdateCheck,
                                    onPickOfflineFile = onPickOfflineFile,
                                    onOpenEditWa = onOpenEditWa,
                                    onBack = { currentPage = SettingsPopupPage.MENU }
                                )
                            }

                            SettingsPopupPage.HELP_GUIDE -> {
                                HelpGuideSubPage(
                                    onBack = { currentPage = SettingsPopupPage.MENU }
                                )
                            }

                            SettingsPopupPage.ABOUT_MONBER -> {
                                AboutMonberSubPage(
                                    lastSyncText = lastSyncText,
                                    onOpenUpdateSection = { currentPage = SettingsPopupPage.APP_PREFERENCES },
                                    onBack = { currentPage = SettingsPopupPage.MENU }
                                )
                            }
                        }
                    }
                }

                // Bottom Soft-Red Pill Action Button ("Keluar" / "Tutup")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 18.dp)
                ) {
                    val isSubPage = currentPage != SettingsPopupPage.MENU
                    val bottomLabel = when {
                        isSubPage -> "Tutup"
                        loggedInPN.isNotBlank() -> "Keluar"
                        else -> "Keluar"
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (!isSubPage && loggedInPN.isNotBlank()) {
                                    onConfirmLogoff()
                                } else {
                                    onDismiss()
                                }
                            }
                            .testTag("popup_menu_keluar_button"),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFDECEC)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = bottomLabel,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = bottomLabel,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsWaveProfileHeader(
    displayName: String,
    subtitle: String,
    onProfileClick: () -> Unit,
    seamlessTopBlend: Boolean = false,
    endCornerContent: (@Composable BoxScope.() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
    ) {
        // Layered Blue BRI Wave transitioning into crisp White at bottom-right
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Deep blue base gradient
            drawRect(
                brush = Brush.linearGradient(
                    colors = if (seamlessTopBlend) {
                        listOf(
                            Color(0xFF005FAF),
                            Color(0xFF00539C),
                            Color(0xFF0C74D1)
                        )
                    } else {
                        listOf(
                            Color(0xFF003B75),
                            Color(0xFF005FAF),
                            Color(0xFF0C74D1)
                        )
                    },
                    start = Offset(0f, 0f),
                    end = Offset(w, h * 0.7f)
                )
            )

            // Upper diagonal azure accent ribbon
            val azureRibbon = Path().apply {
                moveTo(w * 0.36f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.62f)
                cubicTo(
                    w * 0.78f, h * 0.56f,
                    w * 0.58f, h * 0.25f,
                    w * 0.36f, 0f
                )
                close()
            }
            drawPath(
                path = azureRibbon,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0E76D6).copy(alpha = 0.60f),
                        Color(0xFF38BDF8).copy(alpha = 0.35f)
                    ),
                    start = Offset(w * 0.4f, 0f),
                    end = Offset(w, h * 0.6f)
                )
            )

            // Translucent cyan inner wave band
            val softCyanWave = Path().apply {
                moveTo(0f, h * 0.88f)
                cubicTo(
                    w * 0.28f, h * 0.82f,
                    w * 0.62f, h * 0.40f,
                    w, h * 0.28f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = softCyanWave,
                color = Color(0xFF7DD3FC).copy(alpha = 0.30f)
            )

            // Crisp white curved foreground transition (creates clean corner pocket on bottom-right for BRI mark)
            val whiteWave = Path().apply {
                moveTo(0f, h * 0.96f)
                cubicTo(
                    w * 0.30f, h * 0.96f,
                    w * 0.64f, h * 0.56f,
                    w, h * 0.46f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = whiteWave,
                color = Color.White
            )
        }

        // Profile Row (Left/Center)
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(start = 18.dp, end = 104.dp, bottom = 14.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onProfileClick)
                .padding(vertical = 4.dp)
                .testTag("popup_profile_login_row"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.5.dp, Color(0xFFE0EEFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Foto Profil Mantri",
                    tint = Color(0xFF005FAF),
                    modifier = Modifier.size(34.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = displayName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.90f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bottom-right white wave pocket: custom corner content (e.g., gear icon on SettingsScreenView)
        if (endCornerContent != null) {
            endCornerContent()
        }
    }
}

@Composable
private fun MainPopupMenuList(
    selectedBottomNav: String,
    loggedInPN: String,
    hasAvailableUpdate: Boolean,
    onSelectBeranda: () -> Unit,
    onSelectNasabah: () -> Unit,
    onSelectAnalitik: () -> Unit,
    onSelectAppPreferences: () -> Unit,
    onSelectHelpGuide: () -> Unit,
    onSelectAbout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PopupMenuRowItem(
            icon = Icons.Default.Home,
            label = "Beranda",
            isSelected = selectedBottomNav == "DASHBOARD",
            onClick = onSelectBeranda,
            testTag = "popup_menu_beranda"
        )

        PopupMenuRowItem(
            icon = Icons.Default.Groups,
            label = "Nasabah",
            badgeText = if (loggedInPN.isNotBlank()) "Kelolaan" else "Monitoring",
            isSelected = selectedBottomNav in listOf("MONITORING", "KELOLAAN", "RENCANA"),
            onClick = onSelectNasabah,
            testTag = "popup_menu_nasabah"
        )

        PopupMenuRowItem(
            icon = Icons.Default.BarChart,
            label = "Analitik",
            badgeText = "Detail Unit",
            isSelected = false,
            onClick = onSelectAnalitik,
            testTag = "popup_menu_analitik"
        )

        PopupMenuRowItem(
            icon = Icons.Default.Settings,
            label = "Pengaturan Aplikasi",
            badgeText = if (hasAvailableUpdate) "Update" else null,
            badgeIsAlert = hasAvailableUpdate,
            isSelected = selectedBottomNav == "SETTING",
            onClick = onSelectAppPreferences,
            testTag = "popup_menu_app_preferences"
        )

        PopupMenuRowItem(
            icon = Icons.AutoMirrored.Filled.Help,
            label = "Bantuan & Panduan",
            isSelected = false,
            onClick = onSelectHelpGuide,
            testTag = "popup_menu_bantuan"
        )

        PopupMenuRowItem(
            icon = Icons.Default.Info,
            label = "Tentang Monber",
            isSelected = false,
            onClick = onSelectAbout,
            testTag = "popup_menu_tentang"
        )
    }
}

@Composable
private fun PopupMenuRowItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    badgeText: String? = null,
    badgeIsAlert: Boolean = false
) {
    val containerColor = if (isSelected) Color(0xFFEBF4FF) else Color.Transparent
    val textColor = if (isSelected) Color(0xFF005FAF) else Color(0xFF1E293B)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF005FAF),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!badgeText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (badgeIsAlert) Color(0xFFFFF3E0) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (badgeIsAlert) Color(0xFFE65100) else Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubPageHeader(
    title: String,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("popup_subpage_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali ke Menu",
                    tint = Color(0xFF005FAF)
                )
            }
            Text(
                text = title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00386B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider(color = Color(0xFFE2E8F0))
    }
}

@Composable
private fun AppPreferencesSubPage(
    viewModel: CustomerViewModel,
    context: Context,
    isOfflineModeEnabled: Boolean,
    offlineNames: Map<String, String>,
    appUpdateState: AppUpdateState,
    notificationPermissionLauncher: ActivityResultLauncher<String>,
    installPermissionLauncher: ActivityResultLauncher<Intent>,
    onMarkManualUpdateCheck: () -> Unit,
    onPickOfflineFile: (String) -> Unit,
    onOpenEditWa: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubPageHeader(title = "Pengaturan Aplikasi", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Mode Offline Switch + CSV Pickers
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SdCard,
                                contentDescription = "Mode Offline",
                                tint = Color(0xFF005FAF),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Mode Offline",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00386B)
                                )
                                Text(
                                    text = if (isOfflineModeEnabled) {
                                        "Menggunakan file CSV lokal"
                                    } else {
                                        "Mengambil spreadsheet online"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Switch(
                            checked = isOfflineModeEnabled,
                            onCheckedChange = { viewModel.toggleOfflineMode(it) },
                            modifier = Modifier.testTag("popup_offline_mode_switch")
                        )
                    }

                    AnimatedVisibility(visible = isOfflineModeEnabled) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Text(
                                text = "Pilih File CSV Lokal:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00386B)
                            )

                            val fileEntries = listOf(
                                "url_1" to "LW321 Bulan Lalu",
                                "url_2" to "LW321 Akhir Bulan",
                                "url_3" to "LW321 Daily",
                                "url_4" to "Database LW321",
                                "e_url_1" to "LW325 Akhir Bulan",
                                "e_url_2" to "Database LW325",
                                "k_url_1" to "Database Kelolaan",
                                "di319_url" to "DI319",
                                "lw325_daily_url" to "LW325 Daily"
                            )

                            fileEntries.forEach { (key, label) ->
                                val currentName = offlineNames[key].orEmpty()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF334155)
                                        )
                                        Text(
                                            text = currentName.ifBlank { "Belum ada file" },
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = if (currentName.isNotBlank()) Color(0xFF005FAF) else Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Button(
                                        onClick = { onPickOfflineFile(key) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.heightIn(min = 40.dp)
                                    ) {
                                        Text("Pilih", style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (currentName.isNotBlank()) {
                                        IconButton(
                                            onClick = { viewModel.deleteOfflineFile(key) },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus $label",
                                                tint = Color(0xFFD32F2F),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Pengaturan Edit Teks WA
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenEditWa)
                    .testTag("open_edit_wa_from_popup"),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_whatsapp),
                            contentDescription = "WhatsApp",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Pengaturan Edit Teks WA",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00386B)
                            )
                            Text(
                                text = "Edit template pesan WA Menu Rencana & Detail",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Teks WA",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF005FAF)
                    )
                }
            }

            // 3. Check for Update (Tepat di bawah tombol pengeditan teks WA)
            val isUpdateBusy = appUpdateState is AppUpdateState.Checking ||
                appUpdateState is AppUpdateState.Downloading ||
                appUpdateState is AppUpdateState.Installing

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !AppUpdateManager.hasNotificationPermission(context)
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    when (appUpdateState) {
                        is AppUpdateState.NeedsInstallPermission -> {
                            installPermissionLauncher.launch(
                                AppUpdateManager.createInstallPermissionIntent(context)
                            )
                        }
                        is AppUpdateState.ReadyToInstall -> {
                            if (AppUpdateManager.canRequestPackageInstalls(context)) {
                                viewModel.retryInstallAfterPermissionGranted()
                            } else {
                                installPermissionLauncher.launch(
                                    AppUpdateManager.createInstallPermissionIntent(context)
                                )
                            }
                        }
                        else -> {
                            onMarkManualUpdateCheck()
                            viewModel.checkForUpdates(
                                autoInstallOnFind = true,
                                isAutoBackgroundCheck = false
                            )
                        }
                    }
                },
                enabled = !isUpdateBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF005FAF),
                    disabledContainerColor = Color(0xFF005FAF).copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("check_for_update_button")
            ) {
                when (appUpdateState) {
                    is AppUpdateState.Checking -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Memeriksa Pembaruan...", fontWeight = FontWeight.Bold)
                    }
                    is AppUpdateState.Downloading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mengunduh ${appUpdateState.progressPercent}%...", fontWeight = FontWeight.Bold)
                    }
                    is AppUpdateState.Installing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Menyiapkan Instalasi...", fontWeight = FontWeight.Bold)
                    }
                    is AppUpdateState.NeedsInstallPermission -> {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Izinkan Instalasi",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Izinkan & Instal Update", fontWeight = FontWeight.Bold)
                    }
                    is AppUpdateState.ReadyToInstall -> {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Instal Update",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Instal Update (${appUpdateState.release.tagName})", fontWeight = FontWeight.Bold)
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Check for Update",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check for Update", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpGuideSubPage(
    onBack: () -> Unit
) {
    val guides = remember {
        listOf(
            "1. Akses PN Mantri" to "Masuk menggunakan nomor PN pada profil header atau menu Setting untuk menampilkan portofolio Kelolaan dan Rencana kunjungan pribadi.",
            "2. Sumber Data & Memori Offline" to "Tempel link Google Sheets CSV (Daily, Monthly, LW321 All) di menu Setting lalu tekan Sinkronkan. Data otomatis tersimpan di memori HP agar tetap cepat tanpa koneksi.",
            "3. Filter Bulan, Tahun & Tenor" to "Gunakan filter Bulan Lunas, Tahun Lunas, dan Tenor pada menu Monitoring & Kelolaan untuk menyaring nasabah jatuh tempo.",
            "4. Rencana Kunjungan & WhatsApp" to "Tambahkan nasabah dari pencarian Rencana (+), kirim pengingat WhatsApp, dan ubah template pesan lewat Pengaturan Aplikasi."
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SubPageHeader(title = "Bantuan & Panduan", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            guides.forEach { (title, desc) ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00386B)
                        )
                        Text(
                            text = desc,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutMonberSubPage(
    lastSyncText: String,
    onOpenUpdateSection: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubPageHeader(title = "Tentang Monber", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App Identity Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF005FAF)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.monber9),
                        contentDescription = "Logo MonBer",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(34.dp)
                            .wrapContentWidth()
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MONBER App",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Versi ${BuildConfig.VERSION_NAME} • By nUbiin__",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Sistem Monitoring & Kelolaan Kredit Mikro",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00386B)
                    )
                    Text(
                        text = "Mendukung pemantauan portofolio Intrakomp & Ekstrakomp, analisis kolektibilitas, rencana kunjungan Mantri, surat SP, dan template WhatsApp.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = Color(0xFF475569)
                    )
                    HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 2.dp))
                    Text(
                        text = "Update cache terakhir: $lastSyncText",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF005FAF)
                    )
                }
            }

            Button(
                onClick = onOpenUpdateSection,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("about_check_update_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = "Check Update",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Check Update",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
