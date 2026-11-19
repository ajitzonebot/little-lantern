package org.littlelantern.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.littlelantern.feedback.Feedback
import org.littlelantern.feedback.FeedbackKind
import java.text.DateFormat
import java.util.Date

/**
 * Wraps every screen with a small Feedback button in the top corner.
 * Notes are saved on the phone with the screen name; Parent mode sends them to GitHub.
 */
@Composable
fun FeedbackHost(
    enabled: Boolean,
    screenName: String,
    onSave: (screen: String, kind: FeedbackKind, text: String) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    // Remember which screen the note is about at the moment the button was pressed.
    var screenAtOpen by remember { mutableStateOf(screenName) }
    Box(Modifier.fillMaxSize()) {
        content()
        if (enabled) {
            TextButton(
                onClick = { screenAtOpen = screenName; open = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCCFBF9F4)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) { Text("Feedback", fontSize = 13.sp, color = Color(0xFF2F2F2B)) }
        }
    }
    if (open) {
        FeedbackDialog(
            screen = screenAtOpen,
            onDismiss = { open = false },
            onSave = { kind, text ->
                onSave(screenAtOpen, kind, text)
                open = false
            },
        )
    }
}

@Composable
private fun FeedbackDialog(screen: String, onDismiss: () -> Unit, onSave: (FeedbackKind, String) -> Unit) {
    var kind by remember { mutableStateOf(FeedbackKind.BUG) }
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Feedback") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Screen: $screen", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeedbackKind.entries.forEach { k ->
                        if (k == kind) {
                            Button(onClick = { kind = k }) { Text(k.label) }
                        } else {
                            OutlinedButton(onClick = { kind = k }) { Text(k.label) }
                        }
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("What happened, or what should change?") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(kind, text) }, enabled = text.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Parent mode: review saved notes and send them to GitHub as issues. */
@Composable
fun FeedbackScreen(
    items: List<Feedback>,
    repo: String,
    tokenSet: Boolean,
    showButton: Boolean,
    sending: Boolean,
    message: String?,
    onSaveSettings: (repo: String, token: String?) -> Unit,
    onToggleButton: (Boolean) -> Unit,
    onSendAll: () -> Unit,
    onCopyAll: () -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
) {
    var repoText by remember(repo) { mutableStateOf(repo) }
    var tokenText by remember { mutableStateOf("") }
    val unsent = items.count { !it.sent }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Back") }
                Text("Feedback", style = MaterialTheme.typography.headlineSmall)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Show Feedback button on every screen", modifier = Modifier.weight(1f))
                Switch(checked = showButton, onCheckedChange = onToggleButton)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSendAll, enabled = unsent > 0 && tokenSet && !sending, modifier = Modifier.fillMaxWidth()) {
                    Text(if (sending) "Sending..." else "Send $unsent to GitHub")
                }
                OutlinedButton(onClick = onCopyAll, enabled = unsent > 0, modifier = Modifier.fillMaxWidth()) {
                    Text("Copy unsent as text")
                }
                if (message != null) Text(message)
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("GitHub", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = repoText,
                    onValueChange = { repoText = it },
                    label = { Text("Repository (owner/name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = tokenText,
                    onValueChange = { tokenText = it },
                    label = { Text(if (tokenSet) "Token saved. Paste a new one to replace" else "Personal access token") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Fine-grained token with only Issues: read and write on this repository. Stored only on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = {
                    onSaveSettings(repoText, tokenText.ifBlank { null })
                    tokenText = ""
                }) { Text("Save GitHub settings") }
            }
        }
        items(items.sortedByDescending { it.createdAt }, key = { it.id }) { f ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("${f.kind.label} · ${f.screen}", fontWeight = FontWeight.Bold)
                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(f.createdAt)), style = MaterialTheme.typography.bodySmall)
                Text(f.text)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (f.sent) "Sent: ${f.issueUrl}" else "Not sent yet",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onDelete(f.id) }) { Text("Delete") }
                }
            }
        }
    }
}
