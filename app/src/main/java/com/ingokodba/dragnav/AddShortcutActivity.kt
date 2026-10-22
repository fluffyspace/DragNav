package com.ingokodba.dragnav

import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.dragnav.R
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.modeli.PinnedShortcut
import com.ingokodba.dragnav.modeli.RainbowMapa
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Started when another app asks to pin a shortcut to the launcher ("Add to home screen").
 * The user picks any combination of favorites, existing folders and a new folder (or nothing, for the main list only), and the shortcut is then stored as a [PinnedShortcut].
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
            // Nothing checked = main list only (every pinned shortcut is in the all-apps list)
            val choices = listOf(getString(R.string.pin_shortcut_favorites)) +
                folders.map { it.folderName } + getString(R.string.pin_shortcut_new_folder)
            val checked = BooleanArray(choices.size)
            AlertDialog.Builder(this@AddShortcutActivity)
                .setTitle(R.string.pin_shortcut_title)
                .setMultiChoiceItems(choices.toTypedArray(), checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setPositiveButton(R.string.pin_shortcut_add) { _, _ ->
                    val destination = Destination(
                        favorite = checked[0],
                        folderIds = folders.filterIndexed { i, _ -> checked[i + 1] }.map { it.id },
                        newFolder = checked.last(),
                    )
                    if (destination.newFolder) askNewFolderName(request, shortcut, destination)
                    else save(request, shortcut, destination)
                }
                .setNegativeButton(R.string.cancel) { _, _ -> cancel() }
                .setOnCancelListener { cancel() }
                .show()
        }
    }

    /** Where to put the shortcut; it is always in the main list, the rest is optional and combinable. */
    private data class Destination(
        val favorite: Boolean,
        val folderIds: List<Int>,
        val newFolder: Boolean,
        val newFolderName: String = "",
    )

    private fun askNewFolderName(request: LauncherApps.PinItemRequest, shortcut: ShortcutInfo, destination: Destination) {
        val input = EditText(this).apply {
            setSingleLine()
            hint = getString(R.string.folder_name)
            setText((shortcut.shortLabel ?: "").toString())
            selectAll()
        }
        val container = FrameLayout(this).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.new_folder)
            .setView(container)
            .setPositiveButton(R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) cancel() else save(request, shortcut, destination.copy(newFolderName = name))
            }
            .setNegativeButton(R.string.cancel) { _, _ -> cancel() }
            .setOnCancelListener { cancel() }
            .show()
    }

    private fun save(request: LauncherApps.PinItemRequest, shortcut: ShortcutInfo, destination: Destination) {
        lifecycleScope.launch(Dispatchers.IO) {
            val saved = try {
                if (!request.accept()) {
                    false
                } else {
                    val label = (shortcut.shortLabel ?: shortcut.longLabel ?: shortcut.id).toString()
                    val db = AppDatabase.getInstance(this@AddShortcutActivity)
                    val dao = db.pinnedShortcutDao()
                    var pinned = dao.find(shortcut.`package`, shortcut.id)
                        ?: PinnedShortcut(0, shortcut.id, shortcut.`package`, label).also { dao.insert(it) }
                    pinned = dao.find(pinned.packageName, pinned.shortcutId) ?: pinned
                    if (destination.favorite && !pinned.favorite) {
                        pinned = pinned.copy(favorite = true).also { dao.update(it) }
                    }
                    val folderDao = db.rainbowMapaDao()
                    val item = pinned.toAppInfo()
                    folderDao.getAll().filter { it.id in destination.folderIds }.forEach { folder ->
                        if (folder.apps.none { it.packageName == item.packageName }) {
                            folderDao.update(folder.copy(apps = folder.apps.plus(item).toMutableList()))
                        }
                    }
                    if (destination.newFolder) {
                        folderDao.insertAll(RainbowMapa(0, destination.newFolderName, mutableListOf(item), true))
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

    private fun cancel() {
        Toast.makeText(this, getString(R.string.shortcut_not_added), Toast.LENGTH_SHORT).show()
        finish()
    }
}
