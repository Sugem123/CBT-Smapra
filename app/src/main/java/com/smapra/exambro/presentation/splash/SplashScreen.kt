package com.smapra.exambro.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smapra.exambro.R
import com.smapra.exambro.presentation.theme.Gold400
import com.smapra.exambro.presentation.theme.Gold600
import com.smapra.exambro.presentation.theme.GradientNavyEnd
import com.smapra.exambro.presentation.theme.GradientNavyMid
import com.smapra.exambro.presentation.theme.GradientNavyStart
import com.smapra.exambro.presentation.theme.Grey400
import com.smapra.exambro.presentation.theme.White
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
) {
    val logoScale = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val progressAlpha = remember { Animatable(0f) }
    val statusAlpha = remember { Animatable(0f) }

    var statusText by remember { mutableStateOf("Mempersiapkan sistem...") }

    // Pulsing glow behind logo
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowScale",
    )

    LaunchedEffect(Unit) {
        // Phase 1: Logo entrance
        logoScale.animateTo(1.1f, tween(500, easing = FastOutSlowInEasing))
        logoAlpha.animateTo(1f, tween(400))
        logoScale.animateTo(1f, tween(300, easing = FastOutSlowInEasing))

        // Phase 2: Title
        delay(150)
        titleAlpha.animateTo(1f, tween(500))

        // Phase 3: Subtitle
        delay(100)
        subtitleAlpha.animateTo(1f, tween(400))

        // Phase 4: Progress bar
        delay(200)
        progressAlpha.animateTo(1f, tween(300))
        statusAlpha.animateTo(1f, tween(300))

        // Phase 5: Loading messages
        statusText = "Memeriksa keamanan perangkat..."
        delay(600)
        statusText = "Memuat modul ujian..."
        delay(500)
        statusText = "Mengonfigurasi browser aman..."
        delay(500)
        statusText = "Siap!"
        delay(400)

        onSplashFinished()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GradientNavyStart,
                        GradientNavyMid,
                        GradientNavyEnd,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Responsive sizing: adapt logo/text for screen height
        val screenHeight = maxHeight
        val isSmallScreen = screenHeight < 600.dp
        val logoSize = if (isSmallScreen) 80.dp else 120.dp
        val glowSize = if (isSmallScreen) 120.dp else 160.dp
        val titleSize = if (isSmallScreen) 20.sp else 26.sp
        val subtitleSize = if (isSmallScreen) 13.sp else 16.sp
        val spacing = if (isSmallScreen) 16.dp else 32.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp.coerceAtMost(maxWidth * 0.1f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Glow ring behind logo
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(glowSize)
                        .scale(glowScale)
                        .alpha(glowAlpha)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Gold400.copy(alpha = 0.4f),
                                    Gold600.copy(alpha = 0.1f),
                                    Color.Transparent,
                                ),
                            ),
                            CircleShape,
                        ),
                )

                Image(
                    painter = painterResource(id = R.drawable.logo_school),
                    contentDescription = "Logo CBT SMAN 1 PRAMBON",
                    modifier = Modifier
                        .size(logoSize)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                        .shadow(
                            elevation = 20.dp,
                            shape = CircleShape,
                            ambientColor = Gold400.copy(alpha = 0.3f),
                            spotColor = Gold400.copy(alpha = 0.3f),
                        )
                        .clip(CircleShape),
                )
            }

            Spacer(modifier = Modifier.height(spacing))

            Text(
                text = "CBT SMAN 1 PRAMBON",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = titleSize,
                    letterSpacing = 1.5.sp,
                ),
                color = White,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(titleAlpha.value),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Secure Exam Browser",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = subtitleSize,
                    letterSpacing = 3.sp,
                ),
                color = Gold400.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitleAlpha.value),
            )

            Spacer(modifier = Modifier.weight(1f))

            // Progress area at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(progressAlpha.value)
                    .padding(bottom = if (isSmallScreen) 32.dp else 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Gold400,
                    trackColor = White.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Grey400.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(statusAlpha.value),
                )
            }
        }
    }
}
