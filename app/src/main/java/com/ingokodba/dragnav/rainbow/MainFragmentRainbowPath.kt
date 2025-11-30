package com.ingokodba.dragnav.rainbow

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.dragnav.R
import com.google.gson.Gson
import com.ingokodba.dragnav.MainActivity
import com.ingokodba.dragnav.MainFragmentInterface
import com.ingokodba.dragnav.ViewModel
import com.ingokodba.dragnav.modeli.AppInfo
import kotlinx.coroutines.*

/**
 * Fragment hosting the new RainbowPathView with fully configurable path layout
 */
class MainFragmentRainbowPath : Fragment(), MainFragmentInterface {

    private lateinit var pathView: RainbowPathView
    private lateinit var settingsButton: ImageButton
    private lateinit var mActivity: MainActivity

    private val viewModel: ViewModel by activityViewModels()

    override var fragment: Fragment = this

    private var config: PathConfig = PathConfig()
    private var flingJob: Job? = null
    private var countdownJob: Job? = null
    private var currentAppIndex: Int? = null
    private var shortcuts: List<ShortcutInfo> = emptyList()

    private val prefs: SharedPreferences by lazy {
        requireActivity().getSharedPreferences("rainbow_path_config", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = activity as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main_rainbow_path, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pathView = view.findViewById(R.id.rainbow_path_view)
        settingsButton = view.findViewById(R.id.settings_button)

        // Load saved config
        loadConfig()

        // Apply config to view
        pathView.config = config
        pathView.onlyFavorites = prefs.getBoolean("only_favorites", false)

        // Set up event listener
        pathView.setEventListener(object : RainbowPathView.EventListener {
            override fun onAppClicked(appIndex: Int) {
                launchApp(appIndex)
            }

            override fun onAppLongPressed(appIndex: Int) {
                showShortcuts(appIndex)
            }

            override fun onShortcutClicked(shortcutIndex: Int) {
                openShortcut(shortcutIndex)
            }

            override fun onFavoritesToggled() {
                toggleFavorites()
            }

            override fun onFlingStarted() {
                startFlingAnimation()
            }

            override fun onFlingEnded() {
                flingJob?.cancel()
            }

            override fun onLongPressStart(appIndex: Int) {
                startLongPressCountdown()
            }
        })

        // Settings button
        settingsButton.setOnClickListener {
            showSettingsDialog()
        }

        // Load icons if available
        viewModel.icons.value?.let {
            pathView.icons = it
        }

        // Load apps
        updateApps()

        // Observe apps list for changes
        viewModel.appsList.observe(viewLifecycleOwner) { apps ->
            Log.d("RainbowPath", "appsList observer triggered with ${apps?.size ?: 0} apps")
            apps?.let {
                pathView.setApps(it)
                pathView.invalidate()
            }
        }

        // Observe icons for changes
        viewModel.icons.observe(viewLifecycleOwner) { icons ->
            Log.d("RainbowPath", "icons observer triggered")
            icons?.let {
                pathView.icons = it
                pathView.invalidate()
            }
        }

        Log.d("RainbowPath", "Fragment created")
    }

    private fun loadConfig() {
        val configJson = prefs.getString("config", null)
        if (configJson != null) {
            try {
                config = Gson().fromJson(configJson, PathConfig::class.java)
            } catch (e: Exception) {
                Log.e("RainbowPath", "Failed to load config", e)
                config = PathConfig()
            }
        }
    }

    private fun saveConfig() {
        prefs.edit()
            .putString("config", Gson().toJson(config))
            .apply()
    }

    private fun showSettingsDialog() {
        PathSettingsDialog(requireContext(), config) { newConfig ->
            config = newConfig
            pathView.config = config
            saveConfig()
        }.show()
    }

    private fun updateApps() {
        viewModel.appsList.value?.let { apps ->
            Log.d("RainbowPath", "updateApps called with ${apps.size} apps")
            pathView.setApps(apps)
        } ?: Log.d("RainbowPath", "updateApps called but appsList is null")

        viewModel.icons.value?.let {
            pathView.icons = it
        }
        pathView.invalidate()
    }

    private fun getDisplayedApps(): List<AppInfo> {
        val apps = viewModel.appsList.value ?: return emptyList()
        return if (pathView.onlyFavorites) {
            apps.filter { it.favorite }
        } else {
            apps
        }
    }

    private fun launchApp(appIndex: Int) {
        val apps = getDisplayedApps()
        if (appIndex < 0 || appIndex >= apps.size) return

        val app = apps[appIndex]
        val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        }
    }

    private fun startLongPressCountdown() {
        countdownJob?.cancel()
        countdownJob = lifecycleScope.launch {
            delay(250)
            pathView.triggerLongPress()
        }
    }

    private fun showShortcuts(appIndex: Int) {
        countdownJob?.cancel()
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                val launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                if (launcherApps.hasShortcutHostPermission()) {
                    val apps = getDisplayedApps()
                    if (appIndex < 0 || appIndex >= apps.size) return@withContext

                    currentAppIndex = appIndex
                    val app = apps[appIndex]
                    shortcuts = mActivity.getShortcutFromPackage(app.packageName)

                    val shortcutLabels = shortcuts.map { it.shortLabel.toString() }.toMutableList()
                    shortcutLabels.add(if (app.favorite) getString(R.string.remove_from_favorites) else getString(R.string.add_to_favorites))

                    pathView.showShortcuts(appIndex, shortcutLabels)

                    if (shortcuts.isEmpty()) {
                        view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                }
            }
        }
    }

    private fun openShortcut(index: Int) {
        val appIndex = currentAppIndex ?: return

        if (index >= shortcuts.size) {
            // Toggle favorite
            val apps = getDisplayedApps()
            if (appIndex < 0 || appIndex >= apps.size) return

            val app = apps[appIndex]
            app.favorite = !app.favorite
            mActivity.saveAppInfo(app)
            pathView.invalidate()
            return
        }

        val launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        try {
            launcherApps.startShortcut(
                shortcuts[index].`package`,
                shortcuts[index].id,
                null,
                null,
                android.os.Process.myUserHandle()
            )
        } catch (e: Exception) {
            Log.e("RainbowPath", "Failed to start shortcut", e)
        }
    }

    private fun toggleFavorites() {
        pathView.onlyFavorites = !pathView.onlyFavorites
        prefs.edit().putBoolean("only_favorites", pathView.onlyFavorites).apply()
        pathView.invalidate()
    }

    private fun startFlingAnimation() {
        flingJob?.cancel()
        flingJob = lifecycleScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(16) // ~60fps
                pathView.flingUpdate()
            }
        }
    }

    override fun iconsUpdated() {
        viewModel.icons.value?.let {
            pathView.icons = it
            pathView.invalidate()
        }
    }

    override fun selectedItemDeleted() {
        updateApps()
    }

    override fun refreshCurrentMenu() {
        updateApps()
    }

    override fun toggleEditMode() {
        // Not implemented for path view
    }

    override fun goToHome() {
        pathView.scrollToApp(0)
    }

    override fun onPause() {
        super.onPause()
        flingJob?.cancel()
        countdownJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        flingJob?.cancel()
        countdownJob?.cancel()
    }

    companion object {
        fun newInstance(): MainFragmentRainbowPath {
            return MainFragmentRainbowPath()
        }
    }
}
