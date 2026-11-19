package org.littlelantern.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.littlelantern.lock.LockState

/** Parent mode returns to Child mode after this long without a touch. */
private const val PARENT_IDLE_MILLIS = 120_000L

data class ParentStatus(
    val isDeviceOwner: Boolean,
    val lock: LockState,
    val todayMinutes: Int,
    val dailyLimitMin: Int,
    val sessionMin: Int,
    val restMin: Int,
    val isResting: Boolean,
    val voluntaryStops: Int,
    val toReview: Int,
    val approved: Int,
    val unsentFeedback: Int,
)

@Composable
fun ParentHome(
    status: ParentStatus,
    onOpenLockSettings: () -> Unit,
    onOpenVideos: () -> Unit,
    onOpenFeedback: () -> Unit,
    onCycleDailyLimit: () -> Unit,
    onCycleSession: () -> Unit,
    onCycleRest: () -> Unit,
    onWake: () -> Unit,
    onBackToChild: () -> Unit,
    onPauseLock: () -> Unit,
    onReleaseDeviceOwner: () -> Unit,
    lastInteraction: () -> Long,
) {
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            if (SystemClock.uptimeMillis() - lastInteraction() > PARENT_IDLE_MILLIS) {
                onBackToChild()
                break
            }
        }
    }
    var confirmRelease by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Parent mode", style = MaterialTheme.typography.headlineSmall) }
        item { LockCard(status, onOpenLockSettings) }
        item { StatusCard(status) }
        item {
            Button(onClick = onBackToChild, modifier = Modifier.fillMaxWidth()) { Text("Back to Child mode") }
        }
        item {
            OutlinedButton(onClick = onOpenVideos, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (status.toReview > 0) "Videos: ${status.toReview} to review" else "Videos (${status.approved} approved)",
                )
            }
        }
        item {
            OutlinedButton(onClick = onOpenFeedback, modifier = Modifier.fillMaxWidth()) {
                Text(if (status.unsentFeedback > 0) "Feedback: ${status.unsentFeedback} not sent" else "Feedback")
            }
        }
        item { Text("Limits", style = MaterialTheme.typography.titleMedium) }
        item {
            Card {
                SettingRow("Daily limit", "${status.dailyLimitMin} min", onCycleDailyLimit)
                SettingRow("Session length", "${status.sessionMin} min", onCycleSession)
                SettingRow("Rest after a session", "${status.restMin} min", onCycleRest)
            }
        }
        if (status.isResting) {
            item {
                OutlinedButton(onClick = onWake, modifier = Modifier.fillMaxWidth()) { Text("Wake the lantern now") }
            }
        }
        item { Text("Maintenance", style = MaterialTheme.typography.titleMedium) }
        item {
            OutlinedButton(onClick = onPauseLock, enabled = status.lock != LockState.NONE, modifier = Modifier.fillMaxWidth()) {
                Text("Pause lock until Child mode")
            }
        }
        item {
            OutlinedButton(onClick = { confirmRelease = true }, enabled = status.isDeviceOwner, modifier = Modifier.fillMaxWidth()) {
                Text("Remove device owner (before uninstall)")
            }
        }
    }

    if (confirmRelease) {
        AlertDialog(
            onDismissRequest = { confirmRelease = false },
            title = { Text("Remove device owner?") },
            text = { Text("Little Lantern will stop locking this device. Set it up again with adb to restore the lock.") },
            confirmButton = { TextButton(onClick = { confirmRelease = false; onReleaseDeviceOwner() }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirmRelease = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 4.dp),
    ) { content() }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(value, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun StatusCard(s: ParentStatus) {
    Card {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Today: ${s.todayMinutes} of ${s.dailyLimitMin} min watched", fontWeight = FontWeight.Bold)
            Text("Times your child chose \"All done\": ${s.voluntaryStops}")
            Text(if (s.isResting) "Child mode is resting" else "Child mode is open")
        }
    }
}

@Composable
private fun LockCard(s: ParentStatus, onOpenLockSettings: () -> Unit) {
    val red = androidx.compose.ui.graphics.Color(0xFFF6E3DF)
    val green = androidx.compose.ui.graphics.Color(0xFFE7ECE4)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (s.lock == LockState.NONE) red else green)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when (s.lock) {
            LockState.FULL -> Text("Full lock is on", fontWeight = FontWeight.Bold)
            LockState.PINNED -> {
                Text("App pinning is on", fontWeight = FontWeight.Bold)
                Text("Gestures are blocked. Leaving needs your phone PIN. Restarting the phone ends pinning; the app pins again when opened.")
            }
            LockState.NONE -> {
                Text("Not locked: your child can swipe out", fontWeight = FontWeight.Bold)
                Text("1. Open Android settings, Security, App pinning (name differs by phone).")
                Text("2. Turn on App pinning and \"Ask for PIN before unpinning\".")
                Text("3. Come back and go to Child mode. Little Lantern pins itself; tap Pin or Got it if asked.")
                OutlinedButton(onClick = onOpenLockSettings, modifier = Modifier.fillMaxWidth()) { Text("Open Android settings") }
            }
        }
    }
}
