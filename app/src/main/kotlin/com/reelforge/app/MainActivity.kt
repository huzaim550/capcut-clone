package com.reelforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.reelforge.app.navigation.ReelforgeNavHost
import com.reelforge.core.designsystem.ReelforgeTheme

/**
 * The single entry point. It does nothing but establish the theme and hand the
 * repository to the navigation graph; all screen logic lives in the features.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ReelforgeApplication).container
        setContent {
            ReelforgeTheme {
                ReelforgeNavHost(repository = container.projectRepository)
            }
        }
    }
}
