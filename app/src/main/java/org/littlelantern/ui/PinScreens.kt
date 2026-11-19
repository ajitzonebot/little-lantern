package org.littlelantern.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.littlelantern.gate.PinHasher

private const val MAX_WRONG_PINS = 5
private const val DELETE_KEY = "Del"

/** First run: the parent picks a 6-digit PIN (entered twice). */
@Composable
fun PinSetupScreen(onPinChosen: (String) -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    PinPad(
        title = if (first == null) "Choose a 6-digit parent PIN" else "Enter the PIN again",
        message = error,
        onComplete = { pin ->
            val firstPin = first
            when {
                firstPin == null -> { first = pin; error = null }
                firstPin == pin -> onPinChosen(pin)
                else -> { first = null; error = "PINs did not match. Start again." }
            }
        },
        onCancel = null,
    )
}

/** PIN fallback for the parent gate. Too many wrong tries returns to Child mode. */
@Composable
fun PinEntryScreen(checkPin: (String) -> Boolean, onSuccess: () -> Unit, onCancel: () -> Unit) {
    var error by remember { mutableStateOf<String?>(null) }
    var wrong by remember { mutableIntStateOf(0) }
    PinPad(
        title = "Parent PIN",
        message = error,
        onComplete = { pin ->
            if (checkPin(pin)) {
                onSuccess()
            } else {
                wrong++
                if (wrong >= MAX_WRONG_PINS) onCancel() else error = "Wrong PIN"
            }
        },
        onCancel = onCancel,
    )
}

@Composable
private fun PinPad(
    title: String,
    message: String?,
    onComplete: (String) -> Unit,
    onCancel: (() -> Unit)?,
) {
    var entered by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(message ?: " ", color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(PinHasher.PIN_LENGTH) { i ->
                val filled = i < entered.length
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        ),
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", DELETE_KEY),
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (key.isEmpty()) Color.Transparent else MaterialTheme.colorScheme.surface)
                            .clickable(enabled = key.isNotEmpty()) {
                                if (key == DELETE_KEY) {
                                    entered = entered.dropLast(1)
                                } else if (entered.length < PinHasher.PIN_LENGTH) {
                                    entered += key
                                    if (entered.length == PinHasher.PIN_LENGTH) {
                                        val pin = entered
                                        entered = ""
                                        onComplete(pin)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(key, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (onCancel != null) {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}
