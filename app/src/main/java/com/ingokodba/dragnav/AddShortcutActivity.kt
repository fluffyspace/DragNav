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
 * The user picks favorites, the main list, an existing folder or a new folder, and the shortcut is then stored as a [PinnedShortcut].
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
            val choices = listOf(
                getString(R.string.pin_shortcut_favorites),
                getString(R.string.pin_shortcut_main_list),
            ) + folders.map { it.folderName } + getString(R.string.pin_shortcut_new_folder)
            AlertDialog.Builder(this@AddShortcutActivity)
                .setTitle(R.string.pin_shortcut_title)
                .setItems(choices.toTypedArray()) { _, which ->
                    when {
                        which == 0 -> save(request, shortcut, Destination.Favorites)
                        which == 1 -> save(request, shortcut, Destination.MainList)
                        which == choices.lastIndex -> askNewFolderName(request, shortcut)
                        else -> save(request, shortcut, Destination.Folder(folders[which - 2].id))
                    }
                }
                .setOnCancelListener { cancel() }
                .show()
        }
    }

    private sealed interface Destination {
        /** Shown in the favorites view (and the main list). */
        object Favorites : Destination
        /** Only in the all-apps list, like any installed app. */
        object MainList : Destination
        class Folder(val id: Int) : Destination
        class NewFolder(val name: String) : Destination
    }

    private fun askNewFolderName(request: LauncherApps.PinItemRequest, shortcut: ShortcutInfo) {
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
                if (name.isEmpty()) cancel() else save(request, shortcut, Destination.NewFolder(name))
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
                    if (destination is Destination.Favorites && !pinned.favorite) {
                        pinned = pinned.copy(favorite = true).also { dao.update(it) }
                    }
                    val folderDao = db.rainbowMapaDao()
                    val item = pinned.toAppInfo()
                    when (destination) {
                        is Destination.Folder -> folderDao.getAll().firstOrNull { it.id == destination.id }?.let { folder ->
                            if (folder.apps.none { it.packageName == item.packageName }) {
                                folderDao.update(folder.copy(apps = folder.apps.plus(item).toMutableList()))
                            }
                        }
                        is Destination.NewFolder -> folderDao.insertAll(RainbowMapa(0, destination.name, mutableListOf(item), true))
                        else -> {}
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
