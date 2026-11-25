package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.Intent
import android.os.Build
import com.example.BuildConfig
import com.example.ui.components.LoanAnnuityCalculatorSection
import com.example.ui.components.PnLoginPopupDialog
import com.example.ui.components.SettingsMenuPopupDialog
import com.example.ui.components.SettingsWaveProfileHeader
import com.example.ui.viewmodel.CustomerViewModel
import com.example.update.AppUpdateManager
import com.example.update.AppUpdateState

@Composable
fun SettingsScreenView(
    viewModel: CustomerViewModel,
    modifier: Modifier = Modifier,
    onOpenAnalytics: () -> Unit = { viewModel.setBottomNav("DASHBOARD") }
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    var showEditWaDialog by remember { mutableStateOf(false) }
    var showAppPreferencesDialog by remember { mutableStateOf(false) }

    // ViewModel states
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val lastSyncMetadata by viewModel.lastSyncMetadata.collectAsState()
    val loggedInPN by viewModel.loggedInPN.collectAsState()
    val loggedInMantriDetails by viewModel.loggedInMantriDetails.collectAsState()

    // App Update (GitHub Releases: https://github.com/mynoize01/Monber.git)
    val appUpdateState by viewModel.appUpdateState.collectAsState()
    var hasManualUpdateCheck by remember { mutableStateOf(false) }

    LaunchedEffect(appUpdateState) {
        if (!hasManualUpdateCheck) return@LaunchedEffect
        when (val state = appUpdateState) {
            is AppUpdateState.UpToDate -> {
                Toast.makeText(
                    context,
                    "Aplikasi sudah versi terbaru (v${state.currentVersion})",
                    Toast.LENGTH_SHORT
                ).show()
                hasManualUpdateCheck = false
            }
            is AppUpdateState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                hasManualUpdateCheck = false
            }
            is AppUpdateState.Installing, is AppUpdateState.ReadyToInstall, is AppUpdateState.NeedsInstallPermission -> {
                hasManualUpdateCheck = false
            }
            else -> Unit
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val state = appUpdateState
        if (granted && state is AppUpdateState.UpdateAvailable) {
            AppUpdateManager.showUpdateNotification(context, state.release)
        }
    }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.retryInstallAfterPermissionGranted()
    }

    // Monthly Links
    val url1 by viewModel.url1.collectAsState()
    val url2 by viewModel.url2.collectAsState()
    val url4 by viewModel.url4.collectAsState()
    val eUrl2 by viewModel.eUrl2.collectAsState()
    val kUrl1 by viewModel.kUrl1.collectAsState()

    // Daily Links
    val url3 by viewModel.url3.collectAsState()
    val di319Url by viewModel.di319Url.collectAsState()
    val lw325DailyUrl by viewModel.lw325DailyUrl.collectAsState()
    val eUrl1 by viewModel.eUrl1.collectAsState()

    // User requested: "Input Link Spreadsheet default tertutup dan ketika di buka Default pada posisi daily"
    var isInputLinkSelected by remember { mutableStateOf(false) }
    var selectedLinkCategory by remember { mutableStateOf("DAILY") } // Default pada posisi daily ("DAILY", "MONTHLY", "LW321_ALL")

    // Dialogs for PN login & logoff
    var showLoginDialog by remember { mutableStateOf(false) }
    var showConfirmLogoff by remember { mutableStateOf(false) }

    // Hitung Selisih Bulan State (Default hidden)
    var isCalculatorExpanded by remember { mutableStateOf(false) }
    var startCal by remember {
        mutableStateOf(java.util.Calendar.getInstance())
    }
    var endCal by remember {
        mutableStateOf(java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, 1) })
    }

    val startDatePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.YEAR, year)
                    set(java.util.Calendar.MONTH, month)
                    set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                startCal = newCal
            },
            startCal.get(java.util.Calendar.YEAR),
            startCal.get(java.util.Calendar.MONTH),
            startCal.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    val endDatePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.YEAR, year)
                    set(java.util.Calendar.MONTH, month)
                    set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                endCal = newCal
            },
            endCal.get(java.util.Calendar.YEAR),
            endCal.get(java.util.Calendar.MONTH),
            endCal.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    val startSelectedDateStr = String.format("%02d/%02d/%4d",
        startCal.get(java.util.Calendar.DAY_OF_MONTH),
        startCal.get(java.util.Calendar.MONTH) + 1,
        startCal.get(java.util.Calendar.YEAR)
    )
    val endSelectedDateStr = String.format("%02d/%02d/%4d",
        endCal.get(java.util.Calendar.DAY_OF_MONTH),
        endCal.get(java.util.Calendar.MONTH) + 1,
        endCal.get(java.util.Calendar.YEAR)
    )

    val (monthDiffResult, monthDiffDetail) = remember(startCal, endCal) {
        val cal1 = (startCal.clone() as java.util.Calendar).apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val cal2 = (endCal.clone() as java.util.Calendar).apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val isReversed = cal2.before(cal1)
        val earlier = if (isReversed) cal2 else cal1
        val later = if (isReversed) cal1 else cal2

        val temp = earlier.clone() as java.util.Calendar
        var months = 0
        while (true) {
            val nextMonth = temp.clone() as java.util.Calendar
            nextMonth.add(java.util.Calendar.MONTH, 1)
            if (!nextMonth.after(later)) {
                months++
                temp.add(java.util.Calendar.MONTH, 1)
            } else {
                break
            }
        }
        val daysRem = ((later.timeInMillis - temp.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
        val totalDays = ((later.timeInMillis - earlier.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
        val rawMonths = (later.get(java.util.Calendar.YEAR) - earlier.get(java.util.Calendar.YEAR)) * 12 + (later.get(java.util.Calendar.MONTH) - earlier.get(java.util.Calendar.MONTH))

        val sign = if (isReversed) "-" else ""
        val mainRes = if (daysRem == 0) {
            "$sign$months Bulan"
        } else {
            "$sign$months Bulan $daysRem Hari"
        }
        val detail = "Selisih Kalender: $sign$rawMonths Bulan • Total: $sign$totalDays Hari"
        Pair(mainRes, detail)
    }

    // Offline file picker
    var activeFileKey by remember { mutableStateOf("") }
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val stringData = inputStream.bufferedReader().use { it.readText() }
                    var name = "file.csv"
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val nameCol = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameCol != -1) {
                                name = c.getString(nameCol)
                            }
                        }
                    }
                    if (activeFileKey.isNotBlank()) {
                        viewModel.saveOfflineFile(activeFileKey, stringData, name)
                        Toast.makeText(context, "Berhasil memuat file: $name", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val cleanPnDigits = remember(loggedInPN) {
        val raw = loggedInPN.trim()
        val digits = Regex("""\d{4,9}""").find(raw)?.value ?: raw
        digits.trimStart('0').ifEmpty { digits }
    }

    val headerDisplayName = remember(loggedInPN, loggedInMantriDetails) {
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

    val headerSubtitle = remember(loggedInPN, cleanPnDigits) {
        if (loggedInPN.isNotBlank()) {
            "Mantri • PN $cleanPnDigits"
        } else {
            "Ketuk untuk Login PN"
        }
    }

    val pnInfoDisplay = if (loggedInPN.isNotBlank()) {
        val details = if (loggedInMantriDetails.isNotBlank() && loggedInMantriDetails.contains(" - ")) {
            loggedInMantriDetails
        } else if (loggedInMantriDetails.isNotBlank() && loggedInMantriDetails != loggedInPN) {
            "$loggedInPN - $loggedInMantriDetails"
        } else if (loggedInPN.trim() == "207803") {
            "207803 - Nurul Mubin"
        } else {
            "$loggedInPN - Mantri"
        }
        "PN Login: $details"
    } else {
        "Belum ada PN aktif. Masuk untuk akses Kelolaan."
    }

    val lastSyncText = remember(lastSyncMetadata) {
        val ts = lastSyncMetadata?.lastSyncedAt ?: 0L
        if (ts > 0L) {
            java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
                .format(java.util.Date(ts))
        } else {
            "Belum ada sinkronisasi"
        }
    }

    val hasUpdateBadge = appUpdateState is AppUpdateState.UpdateAvailable ||
        appUpdateState is AppUpdateState.ReadyToInstall ||
        appUpdateState is AppUpdateState.NeedsInstallPermission

    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Pinned Curved BRI Blue-to-White Wave Header (full-bleed left, right & top behind status bar)
        SettingsWaveProfileHeader(
            displayName = headerDisplayName,
            subtitle = headerSubtitle,
            onProfileClick = { showLoginDialog = true },
            seamlessTopBlend = true,
            topInset = statusBarTopPadding,
            endCornerContent = {
                IconButton(
                    onClick = { showAppPreferencesDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 4.dp)
                        .size(48.dp)
                        .testTag("app_settings_gear_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Buka Pengaturan Aplikasi",
                            tint = Color(0xFF005FAF),
                            modifier = Modifier.size(26.dp)
                        )
                        if (hasUpdateBadge) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(8.dp)
                                    .background(Color(0xFFF97316), shape = androidx.compose.foundation.shape.CircleShape)
                            )
                        }
                    }
                }
            }
        )

        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = Color(0xFF005FAF),
                trackColor = Color(0xFFE0ECFA)
            )
        }

        // Scrollable Content from Blue Wave Boundary Downward
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

                    // 2. Input Link Spreadsheet (Expandable Row)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { isInputLinkSelected = !isInputLinkSelected },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isInputLinkSelected) Color(0xFFEBF4FF) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Input Link Spreadsheet",
                                tint = Color(0xFF005FAF),
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Input Link Spreadsheet",
                                    fontSize = 14.5.sp,
                                    fontWeight = if (isInputLinkSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isInputLinkSelected) Color(0xFF005FAF) else Color(0xFF1E293B)
                                )
                                Text(
                                    text = if (isInputLinkSelected) {
                                        "Pilihan kategori link aktif di bawah"
                                    } else {
                                        "Kelola link Daily, Monthly & LW321 All"
                                    },
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = if (isInputLinkSelected) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isInputLinkSelected) "Tutup Input Link" else "Buka Input Link",
                                tint = if (isInputLinkSelected) Color(0xFF005FAF) else Color(0xFF64748B)
                            )
                        }
                    }

                    // Konten 3 Kategori Link (LW321 All, Monthly, Daily)
                    AnimatedVisibility(visible = isInputLinkSelected) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CategoryTabItem(
                                    title = "LW321 All",
                                    isSelected = selectedLinkCategory == "LW321_ALL",
                                    onClick = { selectedLinkCategory = "LW321_ALL" },
                                    modifier = Modifier.weight(1f)
                                )
                                CategoryTabItem(
                                    title = "Monthly",
                                    isSelected = selectedLinkCategory == "MONTHLY",
                                    onClick = { selectedLinkCategory = "MONTHLY" },
                                    modifier = Modifier.weight(1f)
                                )
                                CategoryTabItem(
                                    title = "Daily",
                                    isSelected = selectedLinkCategory == "DAILY",
                                    onClick = { selectedLinkCategory = "DAILY" },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                when (selectedLinkCategory) {
                                    "LW321_ALL" -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = Color(0xFF005FAF),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "LW321 All (12 Bulan Januari - Desember)",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00386B)
                                            )
                                        }
                                        Text(
                                            text = "Masukkan link spreadsheet LW321 untuk setiap bulan:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF535F70)
                                        )

                                        val monthNames = listOf(
                                            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                                            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                                        )

                                        monthNames.forEachIndexed { index, monthName ->
                                            val monthNum = index + 1
                                            val currentVal by viewModel.lw321AllUrls[monthNum]?.collectAsState() ?: remember { mutableStateOf("") }
                                            LinkInputField(
                                                label = "${String.format("%02d", monthNum)}. $monthName",
                                                value = currentVal,
                                                onValueChange = { viewModel.saveLw321AllLink(monthNum, it) },
                                                onPaste = {
                                                    val clip = clipboardManager.getText()?.text ?: ""
                                                    if (clip.isNotBlank()) {
                                                        viewModel.saveLw321AllLink(monthNum, clip)
                                                        Toast.makeText(context, "$monthName link disalin!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onClear = { viewModel.saveLw321AllLink(monthNum, "") }
                                            )
                                        }
                                    }

                                    "MONTHLY" -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = Color(0xFF005FAF),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Monthly Links",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00386B)
                                            )
                                        }
                                        Text(
                                            text = "Input link spreadsheet bulanan dan database:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF535F70)
                                        )

                                        Text(
                                            text = "Intrakomp",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00386B),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        LinkInputField(
                                            label = "LW321 Bulan Lalu",
                                            value = url1,
                                            onValueChange = { viewModel.saveLink("url_1", it) },
                                            onPaste = {
                                                val clip = clipboardManager.getText()?.text ?: ""
                                                viewModel.saveLink("url_1", clip)
                                            },
                                            onClear = { viewModel.saveLink("url_1", "") }
                                        )

                                        LinkInputField(
                                            label = "LW321 Akhir Bulan",
                                            value = url2,
                                            onValueChange = { viewModel.saveLink("url_2", it) },
                                            onPaste = {
                                                val clip = clipboardManager.getText()?.text ?: ""
                                                viewModel.saveLink("url_2", clip)
                                            },
                                            onClear = { viewModel.saveLink("url_2", "") }
                                        )

                                        Text(
                                            text = "Ekstrakomp",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00386B),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        LinkInputField(
                                            label = "LW325 Akhir Bulan",
                                            value = eUrl1,
                                            onValueChange = { viewModel.saveLink("e_url_1", it) },
                                            onPaste = {
                                                val clip = clipboardManager.getText()?.text ?: ""
                                                viewModel.saveLink("e_url_1", clip)
                                            },
                                            onClear = { viewModel.saveLink("e_url_1", "") }
                                        )

                                        Text(
                                            text = "Database",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00386B),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        LinkInputField(
                                            label = "Database LW321",
                                            value = url4,
                                            onValueChange = { viewModel.saveLink("url_4", it) },
                                            onPaste = {
                                                val clip = clipboardManager.getText()?.text ?: ""
                                                viewModel.saveLink("url_4", clip)
                                            },
                                            onClear = { viewModel.saveLink("url_4", "") }
                                        )

                                        LinkInputField(
                                            label = "Database LW325",
                                            value = eUrl2,
                                            onValueChange = { viewModel.saveLink("e_url_2", it) },
                                            onPaste = {
                                                val clip = clipboardManager.getText()?.text ?: ""
                                                viewModel.saveLink("e_url_2", clip)
                                            },
                                            onClear = { viewModel.saveLink("e_url_2", "") }
                                        )

                                        LinkInputField(
                                            label = "Database Kelolaan",
                                            value = kUrl1,
                                            onValueChange = { viewModel.saveLink("k_url_1", it) },
                                            onPaste = {
                                                val clip = clipboardManager.getText()?.text ?: ""
                                                viewModel.saveLink("k_url_1", clip)
                                            },
                                            onClear = { viewModel.saveLink("k_url_1", "") }
                                        )
                                    }

                                    "DAILY" -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Today,
                                                contentDescription = null,
                                                tint = Color(0xFF005FAF),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Daily Links",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00386B)
                                            )
                                        }
                                        Text(
                                            text = "Input link spreadsheet harian (Daily):",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF535F70)
                                        )

                                        LinkInputField(
                                            label = "LW321 Daily",
                                            value = url3,
                                            onValueChange = { viewModel.saveLink("url_3", it) },
                                            onPaste = {
                                                val clip = clipboardManager.getText()?.text ?: ""
                                                viewModel.saveLink("url_3", clip)
                                            },
                                            onClear = { viewModel.saveLink("url_3", "") }
                                        )

                                        LinkInputField(
                                            label = "DI319",
                                            value = di319Url,
                                            onValueChange = { viewModel.saveLink("di319_url", it) },
                                            onPaste = {
                                                val clip = clipboardManager.getText()?.text ?: ""
                                                viewModel.saveLink("di319_url", clip)
                                            },
                                            onClear = { viewModel.saveLink("di319_url", "") }
                                        )

                                        LinkInputField(
                                            label = "LW325 Daily",
                                            value = lw325DailyUrl,
                                            onValueChange = { viewModel.saveLink("lw325_daily_url", it) },
                                            onPaste = {
                                                val clip = clipboardManager.getText()?.text ?: ""
                                                viewModel.saveLink("lw325_daily_url", clip)
                                            },
                                            onClear = { viewModel.saveLink("lw325_daily_url", "") }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        viewModel.refreshSpreadsheetData()
                                        Toast.makeText(context, "Link tersimpan & sedang disinkronkan...", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    if (isRefreshing) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sedang Menyinkronkan...")
                                    } else {
                                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Simpan & Sinkronkan Sekarang", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Akses PN Mantri Row
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showLoginDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = "Akses PN Mantri",
                                tint = Color(0xFF005FAF),
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Akses PN Mantri",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = pnInfoDisplay,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEBF4FF)
                            ) {
                                Box(
                                    modifier = Modifier.padding(9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = if (loggedInPN.isBlank()) "Login PN" else "Ubah PN",
                                        tint = Color(0xFF005FAF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )

                    // 4. Kalkulator Pinjaman (Popup Row Style)
                    LoanAnnuityCalculatorSection(usePopupRowStyle = true)

                    // 5. Kalkulator Selisih Bulan (Popup Row Style)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .testTag("month_diff_calc_toggle")
                            .clickable { isCalculatorExpanded = !isCalculatorExpanded },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isCalculatorExpanded) Color(0xFFEBF4FF) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Kalkulator Selisih Bulan",
                                tint = Color(0xFF005FAF),
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Kalkulator Selisih Bulan",
                                    fontSize = 14.5.sp,
                                    fontWeight = if (isCalculatorExpanded) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isCalculatorExpanded) Color(0xFF005FAF) else Color(0xFF1E293B)
                                )
                                Text(
                                    text = if (isCalculatorExpanded) {
                                        "Ketuk untuk menyembunyikan kalkulator"
                                    } else {
                                        "Hitung rentang bulan & hari jatuh tempo"
                                    },
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = if (isCalculatorExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isCalculatorExpanded) "Tutup Kalkulator" else "Buka Kalkulator",
                                tint = if (isCalculatorExpanded) Color(0xFF005FAF) else Color(0xFF64748B)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isCalculatorExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        startDatePickerDialog.updateDate(
                                            startCal.get(java.util.Calendar.YEAR),
                                            startCal.get(java.util.Calendar.MONTH),
                                            startCal.get(java.util.Calendar.DAY_OF_MONTH)
                                        )
                                        startDatePickerDialog.show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF00386B)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Tgl / Bulan Awal",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = Color(0xFF005FAF),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = startSelectedDateStr,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00386B)
                                            )
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        endDatePickerDialog.updateDate(
                                            endCal.get(java.util.Calendar.YEAR),
                                            endCal.get(java.util.Calendar.MONTH),
                                            endCal.get(java.util.Calendar.DAY_OF_MONTH)
                                        )
                                        endDatePickerDialog.show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF00386B)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Bulan / Tgl Akhir",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Event,
                                                contentDescription = null,
                                                tint = Color(0xFF005FAF),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = endSelectedDateStr,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00386B)
                                            )
                                        }
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEBF5FF),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "Hasil Selisih Bulan",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E40AF)
                                    )
                                    Text(
                                        text = monthDiffResult,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF005FAF)
                                    )
                                    if (monthDiffDetail.isNotBlank()) {
                                        Text(
                                            text = monthDiffDetail,
                                            fontSize = 10.sp,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )

                    // 6. Refresh / Sinkron Data CTA + Cache Timestamp
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.refreshSpreadsheetData(force = true)
                                Toast.makeText(context, "Menyinkronkan data ke memori HP...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("settings_refresh_button")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sedang Menyinkronkan...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Data",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Refresh / Sinkron Data", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = Color(0xFF005FAF),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Terakhir update: $lastSyncText",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

        // Branding & App Build inside scrollable white region
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Facebook
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = com.example.R.drawable.ic_facebook),
                        contentDescription = "Facebook",
                        tint = Color(0xFF1877F2),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "@nurul mubin",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00386B)
                    )
                }

                // Instagram
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = com.example.R.drawable.ic_instagram),
                        contentDescription = "Instagram",
                        tint = Color(0xFF005FAF),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "@nUbiin__",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00386B)
                    )
                }
            }

            Text(
                text = "App build ver. ${BuildConfig.VERSION_NAME}",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B)
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 6.5.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.6.sp)) {
                        append("A ")
                    }
                    withStyle(SpanStyle(fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)) {
                        append("nUbiin__ ")
                    }
                    withStyle(SpanStyle(fontSize = 6.5.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.6.sp)) {
                        append("PRODUCTION  •  ")
                    }
                    withStyle(SpanStyle(fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)) {
                        append("MONBER APP ")
                    }
                    withStyle(SpanStyle(fontSize = 6.5.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.5.sp)) {
                        append("SISTEM MONITORING & KELOLAAN KREDIT MIKRO  •  ")
                    }
                    withStyle(SpanStyle(fontSize = 7.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)) {
                        append("PORTOFOLIO INTRAKOMP & EKSTRAKOMP  ")
                    }
                    withStyle(SpanStyle(fontSize = 6.5.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.5.sp)) {
                        append("ANALISIS KOLEKTIBILITAS  •  RENCANA & HASIL KUNJUNGAN MANTRI  •  SINKRONISASI REAL-TIME ")
                    }
                    withStyle(SpanStyle(fontSize = 7.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)) {
                        append("GOOGLE SHEETS  ")
                    }
                    withStyle(SpanStyle(fontSize = 6.5.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.5.sp)) {
                        append("INTEGRASI ")
                    }
                    withStyle(SpanStyle(fontSize = 7.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)) {
                        append("WHATSAPP & SURAT SP  ")
                    }
                    withStyle(SpanStyle(fontSize = 6.5.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.5.sp)) {
                        append("CRAFTED WITH PRECISION BY ")
                    }
                    withStyle(SpanStyle(fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)) {
                        append("NURUL MUBIN")
                    }
                },
                color = Color(0xFF64748B).copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialog Login PN
    if (showLoginDialog) {
        PnLoginPopupDialog(
            initialPn = loggedInPN,
            onDismiss = { showLoginDialog = false },
            onLogin = { inputPn ->
                viewModel.saveLogin(inputPn)
                showLoginDialog = false
            },
            onLogoffClick = { showConfirmLogoff = true }
        )
    }

    // Dialog Konfirmasi Logoff
    if (showConfirmLogoff) {
        AlertDialog(
            onDismissRequest = { showConfirmLogoff = false },
            containerColor = Color.White,
            title = { Text("Konfirmasi Keluar", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin keluar dari sesi PN ini?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logoff()
                        showConfirmLogoff = false
                        showAppPreferencesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Ya, Logoff")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (showAppPreferencesDialog) {
                        TextButton(
                            onClick = {
                                showConfirmLogoff = false
                                showAppPreferencesDialog = false
                            }
                        ) {
                            Text("Tutup Menu")
                        }
                    }
                    TextButton(onClick = { showConfirmLogoff = false }) {
                        Text("Batal")
                    }
                }
            }
        )
    }

    if (showAppPreferencesDialog) {
        SettingsMenuPopupDialog(
            viewModel = viewModel,
            context = context,
            appUpdateState = appUpdateState,
            notificationPermissionLauncher = notificationPermissionLauncher,
            installPermissionLauncher = installPermissionLauncher,
            onMarkManualUpdateCheck = { hasManualUpdateCheck = true },
            onPickOfflineFile = { key ->
                activeFileKey = key
                fileLauncher.launch("*/*")
            },
            onOpenEditWa = { showEditWaDialog = true },
            onOpenLogin = { showLoginDialog = true },
            onConfirmLogoff = { showConfirmLogoff = true },
            onOpenAnalytics = onOpenAnalytics,
            onDismiss = { showAppPreferencesDialog = false }
        )
    }

    if (showEditWaDialog) {
        EditWaTemplatesDialog(
            viewModel = viewModel,
            onDismiss = { showEditWaDialog = false }
        )
    }
}

@Composable
private fun CategoryTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = if (isSelected) Color(0xFF00386B) else Color(0xFF64748B)
        )
    }
}

@Composable
private fun LinkInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            if (value.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Terisi",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Terisi",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color(0xFF1E293B)),
                singleLine = true,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, if (value.isNotBlank()) Color(0xFF005FAF).copy(alpha = 0.5f) else Color(0xFFCBD5E1), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isBlank()) {
                            Text("https://docs.google.com/spreadsheets/...", color = Color.Gray.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                        innerTextField()
                    }
                }
            )

            if (value.isBlank()) {
                IconButton(
                    onClick = onPaste,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF005FAF)
                    )
                }
            } else {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWaTemplatesDialog(
    viewModel: CustomerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentDetailTemplate by viewModel.templateWaDetailDebitur.collectAsState()
    val currentRencanaTemplate by viewModel.templateWaRencana.collectAsState()
    val currentIncludeOs by viewModel.includeOsInRencanaWa.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Detail Debitur, 1: Menu Rencana
    var textDetail by remember(currentDetailTemplate) { mutableStateOf(currentDetailTemplate) }
    var textRencana by remember(currentRencanaTemplate) { mutableStateOf(currentRencanaTemplate) }
    var includeOs by remember(currentIncludeOs) { mutableStateOf(currentIncludeOs) }

    val tabs = listOf("Detail Debitur", "Menu Rencana")

    val detailTags = listOf(
        "{nama}" to "Nama",
        "{keterlambatan}" to "Bulan Telat",
        "{total_tunggakan}" to "Tunggakan",
        "{jatuh_tempo}" to "Jatuh Tempo",
        "{no_rekening}" to "No Rek",
        "{cif}" to "CIF",
        "{plafon}" to "Plafond",
        "{os_pinjaman}" to "Baki Debet",
        "{unit}" to "Unit BRI",
        "{mantri}" to "Nama Mantri",
        "{alamat}" to "Alamat"
    )

    val rencanaTags = listOf(
        "{judul}" to "Judul",
        "{mantri}" to "Nama Mantri",
        "{tanggal}" to "Tanggal",
        "{daftar_kolek}" to "Sesuai Kolek",
        "{daftar_sml2}" to "Daftar SML2",
        "{daftar_sml3}" to "Daftar SML3",
        "{daftar_npl}" to "Daftar NPL",
        "{daftar_semua}" to "Daftar Semua"
    )

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = com.example.R.drawable.ic_whatsapp),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "Pengaturan Edit Teks WA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00386B)
                            )
                            Text(
                                text = "Kustomisasi format teks pesan WhatsApp",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Tab Row
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = Color(0xFF005FAF),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = activeTab == index,
                            onClick = { activeTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (activeTab == index) Color(0xFF005FAF) else Color(0xFF64748B)
                                )
                            }
                        )
                    }
                }

                val currentTags = if (activeTab == 0) detailTags else rencanaTags

                // Tag variables selector
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ketuk tag variabel di bawah untuk menyisipkan ke dalam teks:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155)
                    )

                    // Horizontal scrollable chips for tags
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(currentTags.size) { idx ->
                            val (tag, desc) = currentTags[idx]
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE0F2FE),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF0288D1).copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clickable {
                                        if (activeTab == 0) {
                                            textDetail = if (textDetail.endsWith(" ") || textDetail.endsWith("\n") || textDetail.isEmpty()) {
                                                textDetail + tag
                                            } else {
                                                "$textDetail $tag"
                                            }
                                        } else {
                                            textRencana = if (textRencana.endsWith(" ") || textRencana.endsWith("\n") || textRencana.isEmpty()) {
                                                textRencana + tag
                                            } else {
                                                "$textRencana $tag"
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = Color(0xFF0288D1)
                                    )
                                    Text(
                                        text = "($desc)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color(0xFF00386B).copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (activeTab == 1) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (includeOs) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (includeOs) Color(0xFF22C55E).copy(alpha = 0.6f) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { includeOs = !includeOs }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Tambahkan Info OS Nasabah",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF0F172A)
                                    )
                                    Surface(
                                        color = if (includeOs) Color(0xFFDCFCE7) else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (includeOs) "Aktif" else "Nonaktif",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (includeOs) Color(0xFF166534) else Color(0xFF64748B),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Contoh: 1. Ida Rosida 46jt (opsi dapat diaktifkan / nonaktifkan)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = includeOs,
                                onCheckedChange = { includeOs = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF005FAF)
                                )
                            )
                        }
                    }
                }

                // Editor area
                OutlinedTextField(
                    value = if (activeTab == 0) textDetail else textRencana,
                    onValueChange = { newVal ->
                        if (activeTab == 0) textDetail = newVal else textRencana = newVal
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                    placeholder = {
                        Text("Tuliskan format teks pesan di sini...", fontSize = 11.sp, color = Color.Gray)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedBorderColor = Color(0xFF005FAF),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (activeTab == 0) {
                                textDetail = com.example.ui.viewmodel.DEFAULT_WA_DETAIL_TEMPLATE
                            } else {
                                textRencana = com.example.ui.viewmodel.DEFAULT_WA_RENCANA_TEMPLATE
                                includeOs = true
                            }
                            Toast.makeText(context, "Teks di-reset ke default", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Default", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Batal", color = Color(0xFF64748B))
                        }

                        Button(
                            onClick = {
                                viewModel.saveTemplateWaDetail(textDetail)
                                viewModel.saveTemplateWaRencana(textRencana)
                                viewModel.setIncludeOsInRencanaWa(includeOs)
                                Toast.makeText(context, "Pengaturan pesan WA berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Template", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
    }
}
