package com.smapra.exambro.presentation.setup

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
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
    val savedUrls by viewModel.savedUrls.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    var showQrScanner by remember { mutableStateOf(false) }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) showQrScanner = true
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
            HeroHeader()

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

                // URL Input Section
                Text(
                    text = "Mulai Ujian",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Masukkan alamat server Moodle atau scan QR Code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Premium URL input with QR scan button
                OutlinedTextField(
                    value = uiState.url,
                    onValueChange = viewModel::onUrlChanged,
                    label = { Text("URL Server Moodle") },
                    placeholder = { Text("https://elearning.sekolah.sch.id") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        IconButton(
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
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_qr_scan),
                                contentDescription = "Scan QR Code",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    isError = uiState.urlError != null,
                    supportingText = uiState.urlError?.let { error ->
                        { Text(text = error, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            keyboardController?.hide()
                            viewModel.onStartExam()
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Divider dengan teks "atau"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )
                    Text(
                        text = "  atau  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tombol Scan QR Code — besar dan eksplisit
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_qr_scan),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Scan QR Code Server",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Premium Start Button
                PremiumStartButton(
                    isLoading = uiState.isLoading,
                    onClick = {
                        keyboardController?.hide()
                        viewModel.onStartExam()
                    },
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Saved URLs (Riwayat)
                AnimatedVisibility(
                    visible = savedUrls.isNotEmpty(),
                    enter = fadeIn(tween(300)) + expandVertically(tween(400)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(300)),
                ) {
                    SavedUrlsCard(
                        savedUrls = savedUrls,
                        onSelectUrl = viewModel::onSelectSavedUrl,
                        onClearHistory = viewModel::onClearHistory,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                    viewModel.onUrlChanged(scannedUrl)
                    showQrScanner = false
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
private fun HeroHeader() {
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
            // Logo with elegant shadow
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
                    .border(2.dp, Gold400.copy(alpha = 0.3f), CircleShape),
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

@Composable
private fun PremiumStartButton(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "buttonScale",
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 4.dp else 12.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Navy700.copy(alpha = 0.3f),
                spotColor = Navy700.copy(alpha = 0.3f),
            ),
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Navy700, Navy900),
                    ),
                    RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Gold400,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Gold400,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Mulai Ujian",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        ),
                        color = White,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Gold400.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedUrlsCard(
    savedUrls: Set<String>,
    onSelectUrl: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Riwayat URL",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                IconButton(
                    onClick = onClearHistory,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Hapus Riwayat",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp,
            )

            Spacer(modifier = Modifier.height(4.dp))

            savedUrls.toList().forEach { url ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectUrl(url) },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}
