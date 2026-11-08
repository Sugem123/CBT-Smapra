package com.smapra.exambro.presentation

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.smapra.exambro.presentation.navigation.ExamBroNavGraph
import com.smapra.exambro.presentation.navigation.NavRoutes
import com.smapra.exambro.presentation.theme.ExamBroTheme
import com.smapra.exambro.security.SEBCompatibility
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sebCompatibility: SEBCompatibility

    private var pendingSebUrl: String? = null
    private var isInExam = false
    private var keepSplash by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        val nativeSplash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold native splash until Compose is ready
        nativeSplash.setKeepOnScreenCondition { keepSplash }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        enableEdgeToEdge()

        pendingSebUrl = parseSebIntent(intent)

        setContent {
            // Release native splash once Compose first frame drawn
            keepSplash = false

            ExamBroTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    ExamBroNavGraph(
                        navController = navController,
                        sebUrl = pendingSebUrl,
                    )

                    pendingSebUrl?.let { sebUrl ->
                        val httpUrl = sebCompatibility.parseSebUrl(sebUrl)
                        if (httpUrl != null) {
                            navController.navigate(NavRoutes.examRoute(httpUrl)) {
                                popUpTo(NavRoutes.SETUP) { inclusive = false }
                            }
                        }
                        pendingSebUrl = null
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val sebUrl = parseSebIntent(intent)
        if (sebUrl != null) {
            pendingSebUrl = sebUrl
            recreate()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isInExam) {
            // ExamViewModel handles violation detection
        }
    }

    var onFocusChangedListener: ((Boolean) -> Unit)? = null

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        onFocusChangedListener?.invoke(hasFocus)
    }

    fun setExamActive(active: Boolean) {
        isInExam = active
    }

    private fun parseSebIntent(intent: Intent?): String? {
        val data = intent?.data ?: return null
        val scheme = data.scheme?.lowercase() ?: return null
        if (scheme == "seb" || scheme == "sebs") {
            return data.toString()
        }
        return null
    }
}
