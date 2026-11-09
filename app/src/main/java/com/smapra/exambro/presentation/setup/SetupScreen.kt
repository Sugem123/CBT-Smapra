package com.smapra.exambro.presentation.setup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smapra.exambro.R
import com.smapra.exambro.presentation.theme.Gold400
import com.smapra.exambro.presentation.theme.Gold600
import com.smapra.exambro.presentation.theme.GradientNavyEnd
import com.smapra.exambro.presentation.theme.GradientNavyMid
import com.smapra.exambro.presentation.theme.GradientNavyStart
import com.smapra.exambro.presentation.theme.Navy700
import com.smapra.exambro.presentation.theme.Navy900
import com.smapra.exambro.presentation.theme.White

@Composable
fun SetupScreen(
    onNavigateToExam: (String) -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showQrScanner by remember { mutableStateOf(false) }

    // Secret technician input: ketuk logo 5x cepat untuk input URL manual khusus teknisi
    var secretLogoTapCount by remember { mutableStateOf(0) }
    var lastLogoTapTime by remember { mutableStateOf(0L) }
    var showTechPinDialog by remember { mutableStateOf(false) }
    var techPinInput by remember { mutableStateOf("") }
    var techPinError by remember { mutableStateOf(false) }
    var showTechUrlDialog by remember { mutableStateOf(false) }
    var techUrlInput by remember { mutableStateOf("http://sassman1prambon.sytes.net") }
    var techUrlError by remember { mutableStateOf<String?>(null) }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) showQrScanner = true
    }

    // Pindai ulang ancaman aplikasi melayang setiap kali siswa kembali ke aplikasi (ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.scanThreats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-check update saat app dibuka
    LaunchedEffect(Unit) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        val currentVersionCode = packageInfo.versionCode
        val currentVersionName = packageInfo.versionName ?: "1.0.0"
        viewModel.checkForUpdate(currentVersionCode, currentVersionName)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SetupEvent.NavigateToExam -> onNavigateToExam(event.url)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Hero Header ──
            HeroHeader(
                onLogoTap = {
                    val now = System.currentTimeMillis()
                    if (now - lastLogoTapTime > 3_000L) {
                        secretLogoTapCount = 0
                    }
                    lastLogoTapTime = now
                    secretLogoTapCount++
                    if (secretLogoTapCount >= 5) {
                        secretLogoTapCount = 0
                        techPinInput = ""
                        techPinError = false
                        showTechPinDialog = true
                    }
                }
            )

            // ── Main Content ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-28).dp)
                    .background(
                        MaterialTheme.colorScheme.background,
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    )
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp, bottom = 32.dp),
            ) {
                // Feature badges
                FeatureBadgeRow()

                // Update notification banner if update is available
                uiState.updateInfo?.let { updateInfo ->
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openUpdateDialog() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pembaruan v${updateInfo.versionName} Siap!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Klik di sini untuk langsung mengunduh update",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Error alert jika scan QR tidak valid
                uiState.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = viewModel::dismissError,
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Tutup",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                // Peringatan Keras Jika Ada Aplikasi Floating Terlarang (Pre-Exam Security Block)
                if (uiState.detectedThreats.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Aplikasi Melayang Terdeteksi!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Ditemukan aplikasi yang berpotensi digunakan untuk kecurangan. Ujian diblokir sampai aplikasi ini dicopot atau izinnya dimatikan:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Daftar Aplikasi yang terdeteksi
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                uiState.detectedThreats.forEach { threat ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = threat.appName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                                Text(
                                                    text = threat.reason,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                )
                                            }

                                            TextButton(
                                                onClick = {
                                                    try {
                                                        // Buka halaman pengaturan detail aplikasi untuk langsung uninstall / cabut izin
                                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                            data = Uri.fromParts("package", threat.packageName, null)
                                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                        }
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {
                                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                        }
                                                        context.startActivity(intent)
                                                    }
                                                },
                                            ) {
                                                Text("Atur", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Izin Layar HP", maxLines = 1)
                                }

                                Button(
                                    onClick = viewModel::scanThreats,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pindai Ulang", maxLines = 1)
                                }
                            }
                        }
                    }
                } else {
                    // Kartu Normal: Pindai QR Code Ujian (Hanya tampil jika HP bersih dari aplikasi floating)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                modifier = Modifier.size(68.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_qr_scan),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Akses Ujian Siswa",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Silakan maju ke depan kelas dan pindai QR Code yang disediakan oleh pengawas untuk langsung masuk ke lembar ujian.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA,
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasCameraPermission) {
                                        showQrScanner = true
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                enabled = !uiState.isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.5.dp,
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Menghubungkan Server...",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_qr_scan),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Pindai QR Code Server",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Footer & Manual Check Update
                val currentYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) }
                val versionInfo = remember {
                    val flavorName = if (com.smapra.exambro.BuildConfig.IS_LEGACY) "Android 5+" else "Android 8+"
                    "v${com.smapra.exambro.BuildConfig.VERSION_NAME} ($flavorName)"
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "CBT SMAN 1 PRAMBON © $currentYear",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = versionInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        )
                        TextButton(
                            onClick = {
                                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                @Suppress("DEPRECATION")
                                val currentVersionCode = packageInfo.versionCode
                                val currentVersionName = packageInfo.versionName ?: "1.0.0"
                                viewModel.checkForUpdate(
                                    currentVersionCode = currentVersionCode,
                                    currentVersionName = currentVersionName,
                                    showDialogOnFound = true,
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        ) {
                            if (uiState.isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (uiState.isCheckingUpdate) "Memeriksa..." else "Cek Update",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }

        // Dialog 1: Autentikasi PIN Khusus Teknisi / Proktor (Tap 5x Logo Sekolah)
        if (showTechPinDialog) {
            AlertDialog(
                onDismissRequest = {
                    showTechPinDialog = false
                    techPinInput = ""
                    techPinError = false
                },
                title = { Text("Akses Khusus Teknisi / Proktor") },
                text = {
                    Column {
                        Text(
                            text = "Kamera HP siswa bermasalah? Masukkan PIN Proktor untuk membuka input URL manual.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = techPinInput,
                            onValueChange = {
                                techPinInput = it
                                techPinError = false
                            },
                            label = { Text("PIN Proktor") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            isError = techPinError,
                            supportingText = if (techPinError) {
                                { Text("PIN salah! Akses ditolak.", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (techPinInput == "131313") {
                                showTechPinDialog = false
                                techPinInput = ""
                                techPinError = false
                                showTechUrlDialog = true
                            } else {
                                techPinError = true
                            }
                        },
                    ) {
                        Text("Verifikasi")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showTechPinDialog = false
                        techPinInput = ""
                        techPinError = false
                    }) {
                        Text("Batal")
                    }
                },
            )
        }

        // Dialog 2: Input URL Manual Oleh Teknisi (Tanpa Diketahui Siswa)
        if (showTechUrlDialog) {
            AlertDialog(
                onDismissRequest = {
                    showTechUrlDialog = false
                    techUrlError = null
                },
                title = { Text("Input Alamat Server CBT") },
                text = {
                    Column {
                        Text(
                            text = "Masukkan alamat server Moodle / SEB URL untuk menghubungkan perangkat siswa ini:",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = techUrlInput,
                            onValueChange = {
                                techUrlInput = it
                                techUrlError = null
                            },
                            label = { Text("Alamat Server") },
                            placeholder = { Text("http://sassman1prambon.sytes.net") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            singleLine = true,
                            isError = techUrlError != null,
                            supportingText = techUrlError?.let { err ->
                                { Text(err, color = MaterialTheme.colorScheme.error) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = techUrlInput.trim()
                            if (trimmed.isBlank()) {
                                techUrlError = "Alamat server tidak boleh kosong"
                            } else {
                                showTechUrlDialog = false
                                techUrlError = null
                                // Panggil langsung proses masuk ujian
                                viewModel.onScanQr(trimmed)
                            }
                        },
                    ) {
                        Text("Mulai Ujian")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showTechUrlDialog = false
                        techUrlError = null
                    }) {
                        Text("Batal")
                    }
                },
            )
        }

        // Update notification dialog
        if (uiState.showUpdateDialog && uiState.updateInfo != null) {
            val updateInfo = uiState.updateInfo!!
            UpdateAvailableDialog(
                versionName = updateInfo.versionName,
                releaseNotes = updateInfo.releaseNotes,
                isDownloading = uiState.isDownloadingUpdate,
                downloadProgress = uiState.updateDownloadProgress,
                onUpdate = { viewModel.downloadAndInstall(context) },
                onDismiss = { viewModel.dismissUpdateDialog() },
            )
        }

        // QR Scanner fullscreen overlay
        if (showQrScanner) {
            QrScannerScreen(
                onQrScanned = { scannedUrl ->
                    showQrScanner = false
                    viewModel.onScanQr(scannedUrl)
                },
                onDismiss = { showQrScanner = false },
            )
        }
    }
}

@Composable
private fun UpdateAvailableDialog(
    versionName: String,
    releaseNotes: String,
    isDownloading: Boolean,
    downloadProgress: Float,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        icon = {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        },
        title = {
            Text(
                text = "Update Tersedia!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column {
                Text(
                    text = "Versi baru v$versionName sudah tersedia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ) {
                        Text(
                            text = releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Mengunduh... ${(downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdate,
                enabled = !isDownloading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mengunduh...")
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Update Sekarang")
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text("Nanti Saja")
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun HeroHeader(
    onLogoTap: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GradientNavyStart,
                        GradientNavyMid,
                        GradientNavyEnd,
                    ),
                ),
            ),
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-50).dp, y = (-30).dp)
                .alpha(0.06f)
                .background(Gold400, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = 20.dp)
                .alpha(0.05f)
                .background(White, CircleShape),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo with elegant shadow + Secret tap detector for technician
            Image(
                painter = painterResource(id = R.drawable.logo_school),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(88.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        ambientColor = Gold400.copy(alpha = 0.25f),
                        spotColor = Gold400.copy(alpha = 0.25f),
                    )
                    .clip(CircleShape)
                    .border(2.dp, Gold400.copy(alpha = 0.3f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLogoTap,
                    ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CBT SMAN 1 PRAMBON",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
                color = White,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Gold accent line
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Gold400.copy(alpha = 0.4f),
                                Gold600,
                                Gold400.copy(alpha = 0.4f),
                            ),
                        ),
                    ),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Secure Exam Browser",
                style = MaterialTheme.typography.titleSmall.copy(
                    letterSpacing = 2.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = Gold400.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FeatureBadgeRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        FeatureBadge(
            icon = Icons.Filled.Lock,
            label = "Kiosk\nMode",
            tint = MaterialTheme.colorScheme.primary,
        )
        FeatureBadge(
            icon = Icons.Filled.Close,
            label = "Anti\nScreenshot",
            tint = MaterialTheme.colorScheme.tertiary,
        )
        FeatureBadge(
            icon = Icons.Filled.Info,
            label = "Deteksi\nFloat App",
            tint = MaterialTheme.colorScheme.secondary,
        )
        FeatureBadge(
            icon = Icons.Filled.Check,
            label = "SEB\nProtocol",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun FeatureBadge(
    icon: ImageVector,
    label: String,
    tint: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(76.dp),
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(14.dp),
            color = tint.copy(alpha = 0.1f),
            tonalElevation = 0.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
        )
    }
}
