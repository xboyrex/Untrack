package com.xboyrex.untrack

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProcessTextActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent?.action
        val rawText = when (action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            "com.xboyrex.untrack.ACTION_CLEAN" -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }

        if (rawText.isNullOrBlank()) {
            finish()
            return
        }

        val isReadOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)

        lifecycleScope.launch {
            val result = UntrackPipeline.processText(rawText)
            result.primaryReport?.let { rep ->
                UntrackStore.recordCleanResult(
                    HistoryItem(
                        originalUrl = rep.originalUrl,
                        cleanedUrl = rep.cleanedUrl,
                        domain = rep.parsedCleaned.site,
                        removedParamsCount = rep.parameterRows.count { it.isRemoved },
                        frontendConverted = rep.convertedFrontendDomain,
                        shortLinkResolved = rep.shortLinkResolved
                    )
                )
            }

            if (action == "com.xboyrex.untrack.ACTION_CLEAN") {
                setResult(Activity.RESULT_OK, Intent().putExtra(Intent.EXTRA_TEXT, result.outputText))
                finish()
                return@launch
            }

            if (!isReadOnly) {
                // Replace selected text inline inside the calling app
                val reply = Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, result.outputText)
                setResult(Activity.RESULT_OK, reply)
                Toast.makeText(applicationContext, "Link untracked!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                // Forward read-only selection to MainActivity studio
                val openMain = Intent(this@ProcessTextActivity, MainActivity::class.java).apply {
                    this.action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, rawText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(openMain)
                finish()
            }
        }
    }
}
