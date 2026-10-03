package com.reelforge.app.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.reelforge.core.designsystem.EmptyState
import com.reelforge.core.designsystem.ReelforgeIcons

/**
 * A stand-in for the editing surface so the browser's "open" action leads
 * somewhere real. The editor itself (preview, timeline, clips) is the next slice;
 * this exists to prove navigation and to carry the project id forward.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorPlaceholderScreen(
    projectId: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ReelforgeIcons.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            EmptyState(
                title = "Editor coming soon",
                message = "Project $projectId is ready. The timeline surface lands in the next slice.",
                icon = ReelforgeIcons.Play,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
