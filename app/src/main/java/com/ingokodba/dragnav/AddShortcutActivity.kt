package com.ingokodba.dragnav

import android.content.pm.LauncherApps
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.dragnav.R
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.modeli.PinnedShortcut
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Started when another app asks to pin a shortcut to the launcher ("Add to home screen").
 * The user picks the main list or a folder, and the shortcut is then stored as a [PinnedShortcut].
 */
@AndroidEntryPoint
class AddShortcutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launcherApps = getSystemService(LauncherApps::class.java)
        val request = launcherApps.getPinItemRequest(intent)
        val shortcut = request?.shortcutInfo
        if (request == null || !request.isValid || request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT || shortcut == null) {
            cancel()
            return
        }

        lifecycleScope.launch {
            val folders = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@AddShortcutActivity).rainbowMapaDao().getAll()
            }
            val choices = listOf(getString(R.string.pin_shortcut_main_list)) + folders.map { it.folderName }
            AlertDialog.Builder(this@AddShortcutActivity)
                .setTitle(R.string.pin_shortcut_title)
                .setItems(choices.toTypedArray()) { _, which ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val saved = try {
                            if (!request.accept()) {
                                false
                            } else {
                                val label = (shortcut.shortLabel ?: shortcut.longLabel ?: shortcut.id).toString()
                                val db = AppDatabase.getInstance(this@AddShortcutActivity)
                                val dao = db.pinnedShortcutDao()
                                val pinned = dao.find(shortcut.`package`, shortcut.id)
                                    ?: PinnedShortcut(0, shortcut.id, shortcut.`package`, label).also { dao.insert(it) }
                                if (which > 0) {
                                    val folder = folders[which - 1]
                                    val item = (dao.find(pinned.packageName, pinned.shortcutId) ?: pinned).toAppInfo()
                                    if (folder.apps.none { it.packageName == item.packageName }) {
                                        db.rainbowMapaDao().update(folder.copy(apps = folder.apps.plus(item).toMutableList()))
                                    }
                                }
                                true
                            }
                        } catch (e: Exception) {
                            Log.e("AddShortcutActivity", "Failed to pin shortcut", e)
                            false
                        }
                        withContext(Dispatchers.Main) {
                            if (saved) finish() else cancel()
                        }
                    }
                }
                .setOnCancelListener { cancel() }
                .show()
        }
    }

    private fun cancel() {
        Toast.makeText(this, getString(R.string.shortcut_not_added), Toast.LENGTH_SHORT).show()
        finish()
    }
}
