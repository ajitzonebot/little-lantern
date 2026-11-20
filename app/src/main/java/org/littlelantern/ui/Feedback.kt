package org.littlelantern.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.littlelantern.feedback.FeedbackTarget

/**
 * Wraps a screen with a small Feedback button in the top-right corner.
 * The note is saved with the screen it was written on, then sent to feedback.md.
 */
@Composable
fun FeedbackHost(
    enabled: Boolean,
    target: FeedbackTarget,
    onSave: (FeedbackTarget, String) -> Unit,
    content: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf<FeedbackTarget?>(null) }
    Box(Modifier.fillMaxSize()) {
        content()
        if (enabled) {
            TextButton(
                onClick = { open = target },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xE6FBF9F4))
                    .border(1.dp, Palette.Line, RoundedCornerShape(16.dp)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) { Text("Feedback", fontSize = 12.sp, color = Palette.InkSoft) }
        }
    }
    open?.let { t ->
        FeedbackDialog(
            target = t,
            onDismiss = { open = null },
            onSave = { text -> onSave(t, text); open = null },
        )
    }
}

@Composable
private fun FeedbackDialog(target: FeedbackTarget, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Feedback") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Screen: ${target.label}", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("What happened, or what should change?") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                )
                Text(
                    "Goes to feedback.md on GitHub. Saved on the phone until it is sent.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
