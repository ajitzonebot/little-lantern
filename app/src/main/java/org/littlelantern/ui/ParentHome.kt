package org.littlelantern.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.littlelantern.data.Video
import org.littlelantern.games.GameInfo
import org.littlelantern.lock.LockState
import java.time.YearMonth

/** Parent mode returns to Child mode after this long without a touch. */
private const val PARENT_IDLE_MILLIS = 120_000L

class ParentSettings(
    val birthMonth: YearMonth?,
    val volumeCap: Float,
    val colourBlind: Boolean,
    val apiKey: String,
    val disabledGames: Set<String>,
    val ageBand: Int,
    val usedMinutesToday: Int,
    val showFeedback: Boolean,
    val feedbackRepo: String,
    val feedbackBranch: String,
)

class FeedbackStatus(
    val pending: Int,
    val tokenSet: Boolean,
    val sending: Boolean,
    val message: String?,
)

class ParentActions(
    val onBackToChild: () -> Unit,
    val onAddVideo: () -> Unit,
    val onRemoveVideo: (String) -> Unit,
    val onBirthMonth: (YearMonth?) -> Unit,
    val onVolumeCap: (Float) -> Unit,
    val onColourBlind: (Boolean) -> Unit,
    val onApiKey: (String) -> Unit,
    val onToggleGame: (String, Boolean) -> Unit,
    val onPauseLock: () -> Unit,
    val onOpenPinningSettings: () -> Unit,
    val onShowFeedback: (Boolean) -> Unit,
    /** repo, branch, new token (null keeps the saved one). */
    val onFeedbackSettings: (String, String, String?) -> Unit,
    val onSendFeedback: () -> Unit,
    val onReleaseDeviceOwner: () -> Unit,
)

@Composable
fun ParentHome(
    isDeviceOwner: Boolean,
    lockState: LockState,
    videos: List<Video>,
    games: List<GameInfo>,
    settings: ParentSettings,
    feedback: FeedbackStatus,
    actions: ParentActions,
    lastInteraction: () -> Long,
) {
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            if (SystemClock.uptimeMillis() - lastInteraction() > PARENT_IDLE_MILLIS) {
                actions.onBackToChild()
                break
            }
        }
    }
    var confirmRelease by remember { mutableStateOf(false) }
    var birthText by remember { mutableStateOf(settings.birthMonth?.toString() ?: "") }
    var keyText by remember { mutableStateOf(settings.apiKey) }
    var volume by remember { mutableStateOf(settings.volumeCap) }
    var repoText by remember { mutableStateOf(settings.feedbackRepo) }
    var branchText by remember { mutableStateOf(settings.feedbackBranch) }
    var tokenText by remember { mutableStateOf("") }
    val isLocked = lockState != LockState.NONE

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Parent mode", style = MaterialTheme.typography.headlineSmall) }
        item { StatusCard(isDeviceOwner, lockState, settings.usedMinutesToday, actions.onOpenPinningSettings) }
        item {
            Button(onClick = actions.onBackToChild, modifier = Modifier.fillMaxWidth()) { Text("Back to Child mode") }
        }

        item { Section("Videos") }
        item {
            OutlinedButton(onClick = actions.onAddVideo, modifier = Modifier.fillMaxWidth()) { Text("Add a video") }
        }
        if (videos.isEmpty()) {
            item { Small("No videos yet. Add one with a YouTube link.") }
        }
        items(videos, key = { it.id }) { v ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.Surface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(v.title, fontSize = 14.sp, maxLines = 2)
                    Small("${v.durationSeconds / 60} min · pace ${v.report.paceLabel.lowercase()}")
                }
                TextButton(onClick = { actions.onRemoveVideo(v.id) }) { Text("Remove") }
            }
        }

        item { Section("Child") }
        item {
            OutlinedTextField(
                value = birthText,
                onValueChange = {
                    birthText = it
                    val parsed = runCatching { YearMonth.parse(it.trim()) }.getOrNull()
                    if (parsed != null || it.isBlank()) actions.onBirthMonth(parsed)
                },
                label = { Text("Birth month (YYYY-MM)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { Small("Games for age ${settings.ageBand} are shown. Daily limit: 60 minutes, the maximum.") }
        item {
            Column {
                Text("Volume limit: ${(volume * 100).toInt()}%")
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    onValueChangeFinished = { actions.onVolumeCap(volume) },
                    valueRange = 0.1f..1f,
                )
                Small("Keep it at half or below for headphones. The session lowers it further near the end.")
            }
        }
        item {
            SwitchRow("Colour-blind mode", "Hides colour-only games; colour sorting uses shapes.", settings.colourBlind, actions.onColourBlind)
        }

        item { Section("Games") }
        item { Small("The child sees the 10 games for their age. Games from younger ages stay on unless you switch them off.") }
        items(games, key = { it.id }) { g ->
            SwitchRow(
                "${g.title} (age ${g.age})",
                "${g.area} · ${g.basis}",
                g.id !in settings.disabledGames,
            ) { on -> actions.onToggleGame(g.id, on) }
        }

        item { Section("Online checks") }
        item {
            OutlinedTextField(
                value = keyText,
                onValueChange = { keyText = it; actions.onApiKey(it) },
                label = { Text("YouTube Data API key (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { Small("Used only for the paid-placement and made-for-kids checks. Without it those show Unknown.") }

        item { Section("Feedback") }
        item {
            SwitchRow(
                "Feedback button on every screen",
                "For testing. Notes go to feedback.md on GitHub under the screen they were written on.",
                settings.showFeedback,
                actions.onShowFeedback,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = actions.onSendFeedback,
                    enabled = feedback.pending > 0 && feedback.tokenSet && !feedback.sending,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (feedback.sending) "Sending..." else "Send ${feedback.pending} unsent to feedback.md") }
                if (feedback.message != null) Small(feedback.message)
            }
        }
        item {
            OutlinedTextField(
                value = repoText,
                onValueChange = { repoText = it },
                label = { Text("Repository (owner/name)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = branchText,
                onValueChange = { branchText = it },
                label = { Text("Branch (blank = default branch)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = tokenText,
                onValueChange = { tokenText = it },
                label = { Text(if (feedback.tokenSet) "Token saved. Paste a new one to replace" else "GitHub token") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Small("Fine-grained token, this repository only, permission Contents: read and write. Stored only on this phone.")
        }
        item {
            OutlinedButton(
                onClick = {
                    actions.onFeedbackSettings(repoText, branchText, tokenText.ifBlank { null })
                    tokenText = ""
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save GitHub settings") }
        }

        item { Section("Maintenance") }
        item {
            OutlinedButton(onClick = actions.onPauseLock, enabled = isLocked, modifier = Modifier.fillMaxWidth()) {
                Text("Pause lock until Child mode")
            }
        }
        item {
            OutlinedButton(
                onClick = { confirmRelease = true },
                enabled = isDeviceOwner,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Remove device owner (before uninstall)") }
        }
    }

    if (confirmRelease) {
        AlertDialog(
            onDismissRequest = { confirmRelease = false },
            title = { Text("Remove device owner?") },
            text = { Text("Little Lantern will stop locking this device. Set it up again with adb to restore the lock.") },
            confirmButton = {
                TextButton(onClick = { confirmRelease = false; actions.onReleaseDeviceOwner() }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRelease = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Small(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Palette.InkSoft)
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp)
            Small(subtitle)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StatusCard(isDeviceOwner: Boolean, lockState: LockState, usedMinutes: Int, onOpenPinningSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val (lockText, ok) = when (lockState) {
            LockState.FULL -> "Lock: full (device owner)" to true
            LockState.PINNED -> "Lock: App pinning" to true
            LockState.NONE -> "Lock: off (child can leave the app)" to false
        }
        Text(lockText, color = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error)
        Text("Used today: $usedMinutes of 60 minutes")
        if (!isDeviceOwner) {
            Text(
                "App pinning: in Settings, search \"App pinning\" (or \"Screen pinning\"), turn it on with \"Ask for PIN before unpinning\". " +
                    "Child mode pins the app and the phone asks once to confirm. Pinning ends on restart; open the app again to re-pin. " +
                    "For a full lock that survives restarts, set device owner on a spare phone (docs/SETUP.md).",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = onOpenPinningSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Open App pinning settings")
            }
        }
    }
}
