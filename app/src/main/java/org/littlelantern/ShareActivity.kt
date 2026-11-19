package org.littlelantern

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * "Share to Little Lantern" from the YouTube app or a browser.
 * Shared videos only enter the review list; nothing reaches Child mode until the parent approves it.
 */
class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.takeIf { it.action == Intent.ACTION_SEND }?.getStringExtra(Intent.EXTRA_TEXT)
        if (text != null) {
            (application as LittleLanternApp).videos.addLink(text)
            Toast.makeText(this, "Sent to Little Lantern for checking", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
