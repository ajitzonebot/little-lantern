package org.littlelantern.ui

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import org.littlelantern.data.AppEntry

/** Parent mode returns to Child mode after this long without a touch. */
private const val PARENT_IDLE_MILLIS = 120_000L

@Composable
fun ParentHome(
    isDeviceOwner: Boolean,
    isLocked: Boolean,
    apps: List<AppEntry>,
    allowedGames: Set<String>,
    onToggleGame: (String, Boolean) -> Unit,
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
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Parent mode", style = MaterialTheme.typography.headlineSmall) }
        item { StatusCard(isDeviceOwner, isLocked) }
        item {
            Button(onClick = onBackToChild, modifier = Modifier.fillMaxWidth()) { Text("Back to Child mode") }
        }
        item {
            Column {
                Text("Games allowed in Child mode", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Only allow apps you have checked: no ads, no links out, no rewards or timers.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        items(apps, key = { it.packageName }) { app ->
            val bitmap = remember(app.packageName) { app.icon.toBitmap(96, 96).asImageBitmap() }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(12.dp))
                Text(app.label, modifier = Modifier.weight(1f))
                Switch(
                    checked = app.packageName in allowedGames,
                    onCheckedChange = { onToggleGame(app.packageName, it) },
                )
            }
        }
        item { Text("Maintenance", style = MaterialTheme.typography.titleMedium) }
        item {
            OutlinedButton(onClick = onPauseLock, enabled = isLocked, modifier = Modifier.fillMaxWidth()) {
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
                TextButton(onClick = { confirmRelease = false; onReleaseDeviceOwner() }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRelease = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun StatusCard(isDeviceOwner: Boolean, isLocked: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(
            if (isDeviceOwner) "Device owner: yes" else "Device owner: no (child can leave the app)",
            color = if (isDeviceOwner) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        )
        Text("Lock active: ${if (isLocked) "yes" else "no"}")
        if (!isDeviceOwner) {
            Text(
                "See docs/SETUP.md to set device owner with adb.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
