# DragNav bug-fix plan

**Status (2026-10-11): all Part A–D fixes are done and committed on `main`; `assembleDebug test` passes. The "Not to be done in this pass" items were then handled: pinned shortcuts implemented, dead settings and unreachable screens removed, hidden-in-folders kept as is. What is still open is in `SKIPPED.md`.**

A step-by-step plan for fixing the bugs found in the 2026-10-11 review of `main`.
Each fix says **where**, **what's wrong**, **what to change**, and **how to check it**.

## Rules for whoever runs this plan

- Work only on `main`, or on a branch made from it (suggested: `fix/bug-sweep`). **Never read, check out, diff or merge `archive/all-launcher-modes`.**
- The working tree already has an uncommitted change in `rainbow/SearchOverlayMaterial.kt` that draws folder icons as a 2×2 grid in the search overlay. Keep it, and don't revert it. Commit it first as `show folder app icons in search overlay`.
- Line numbers below are from before any edits and **will drift**. Find each spot by searching for the quoted code, not by line number.
- Source root: `app/src/main/java/com/ingokodba/dragnav/` (paths below are relative to it unless they start with `app/`).
- After **every** fix, build:
  ```bash
  JAVA_HOME=/opt/android-studio/jbr ./gradlew compileDebugKotlin
  ```
  Fix any compile errors before moving on. At the end, run `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug test`.
- Make one commit per numbered fix, with a short lowercase message like the existing history (e.g. `run folder update on main thread after app uninstall`). Don't push.
- Match the surrounding code style. Don't refactor beyond what each step says.
- If a step doesn't match the code you find (the code was changed, or the snippet is missing), **skip it and note it in a `SKIPPED.md` file**. Don't guess.

---

## Part A: crashes and security (do these first)

### A1. Crash when an app inside a folder is uninstalled — ✅ FIXED (`9c93074`)
- **Where:** `MainActivityCompose.kt`, function `appDeleted`, inside the `for (mapa in rainbowMaps)` loop: `viewModel.updateRainbowMapa(updatedMapa)`.
- **Problem:** this runs on `Dispatchers.IO`, and `ViewModel.updateRainbowMapa` sets `MutableLiveData.value`, which throws `IllegalStateException` off the main thread.
- **Fix:** wrap the call:
  ```kotlin
  withContext(Dispatchers.Main) {
      viewModel.updateRainbowMapa(updatedMapa)
  }
  ```
- **Also in `ViewModel.updateRainbowMapa`:** if `index == -1` then `add(-1, mapa)` crashes. Change it to:
  ```kotlin
  fun updateRainbowMapa(mapa: RainbowMapa){
      val current = _rainbow_mape.value!!
      val index = current.indexOfFirst { it.id == mapa.id }
      if (index == -1) return
      _rainbow_mape.value = current.toMutableList().apply { set(index, mapa) }
  }
  ```

### A2. Crash when tapping a notification on Android 8–13 — ✅ FIXED (`2ab4e60`)
- **Where:** `compose/RainbowPathScreen.kt`, in the `NotificationsList(... onNotificationClick = ...)` block: `pendingIntentBackgroundActivityStartMode = ...`.
- **Problem:** that property only exists on API 34+, and minSdk is 26. The `NoSuchMethodError` it throws is an `Error`, so `catch (e: Exception)` doesn't catch it.
- **Fix:**
  ```kotlin
  val options = android.app.ActivityOptions.makeBasic().apply {
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
          pendingIntentBackgroundActivityStartMode =
              android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
      }
  }.toBundle()
  ```

### A3. Search overlay crashes on Android 8–10 — ✅ FIXED (`0f4244e`)
- **Where:** `rainbow/SearchOverlayMaterial.kt`, class `SearchOverlayMaterialView`, `init` block: `insets.getInsets(android.view.WindowInsets.Type.ime())`.
- **Problem:** `WindowInsets.Type` is API 30+.
- **Fix:** use the androidx compat API instead:
  ```kotlin
  androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
      val imeInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())
      setPadding(0, 0, 0, imeInsets.bottom)
      insets
  }
  ```
  (Replace the whole `setOnApplyWindowInsetsListener { v, insets -> ... }` call.)

### A4. AddShortcutActivity crashes on Cancel — ✅ FIXED (`8533d29`)
- **Where:** `AddShortcutActivity.kt`, the Cancel button listener: `Intent(this@AddShortcutActivity, MainActivity::class.java)`.
- **Problem:** `MainActivity` is now a plain stub class in `Constants.kt`, not an Activity, so this throws `ActivityNotFoundException`.
- **Fix:** replace the listener body with:
  ```kotlin
  Toast.makeText(this, getString(R.string.shortcut_not_added), Toast.LENGTH_SHORT).show()
  finish()
  ```
- Also, in the `else` branch where `pinItemRequest == null`, replace `Toast.makeText(this, "no", ...)` with the same `shortcut_not_added` toast followed by `finish()`.
- In the `else` branch of `if (initializeRoom())` (the `Log.d("ingo", "initialize bad")` one), switch to Main, show the `shortcut_not_added` toast and call `finish()`.
- **Don't** try to make pinned shortcuts show up in Rainbow Path. That's a feature decision and out of scope; add a line about it to `SKIPPED.md`.

### A5. Notification contents leak to every installed app — ✅ FIXED (`5674fd2`)
- **Where:** `NotificationListener.kt`, end of `updateNotifications()`: the "Also broadcast for backwards compatibility" block (`sendBroadcast(intent)` with `ACTION_NOTIFICATIONS_UPDATED`). The receiving side is in `MainActivityCompose.onCreate` (the `notificationReceiver` object and its `registerReceiver` calls).
- **Problem:** the broadcast is implicit with no package set, so any app can receive all notification titles, texts and `PendingIntent`s. On API < 33 the activity's receiver is also exported, so any app can inject fake notifications.
- **Fix:**
  1. In `NotificationListener.updateNotifications()`, delete the whole broadcast block (from `// Also broadcast for backwards compatibility` through `sendBroadcast(intent)` and its log line). The direct `onNotificationsChanged` callback already delivers the data.
  2. In `MainActivityCompose.onCreate`, delete the `notificationReceiver = object : BroadcastReceiver ...` block, the `notificationFilter` and its `registerReceiver` if/else. In `onDestroy`, delete the `notificationReceiver?.let { unregisterReceiver(it) }` block and remove the `notificationReceiver` field.
  3. `NotificationData.kt` is now unused. Check with `grep -rn NotificationData app/src/main/java`, and if only its own file matches, delete it.
  4. The request broadcast `"com.ingokodba.dragnav.REQUEST_NOTIFICATION_UPDATE"` is sent in two places in `MainActivityCompose` (in `onCreate` and `onResume`). Add `setPackage(packageName)` to both intents:
     ```kotlin
     val requestIntent = Intent("com.ingokodba.dragnav.REQUEST_NOTIFICATION_UPDATE").setPackage(packageName)
     ```
  5. In `NotificationListener.onListenerConnected`, replace the API if/else around `registerReceiver(updateRequestReceiver, filter ...)` with:
     ```kotlin
     androidx.core.content.ContextCompat.registerReceiver(
         this, updateRequestReceiver, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
     )
     ```
     Also, before registering, unregister any previous receiver (`updateRequestReceiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {} }`), because `onListenerConnected` can run more than once.

### A6. RemoveAppService is exported and crashes on bind — ✅ FIXED (`bd353d3`)
- **Where:** `RemoveAppService.kt` and its `<service android:name="com.ingokodba.dragnav.RemoveAppService" ... android:exported="true" />` entry in `app/src/main/AndroidManifest.xml`.
- **Problem:** nothing in the app starts it (`grep -rn RemoveAppService` only finds the class). Any app can start it to delete records, and binding to it hits `TODO()`, which crashes the launcher process.
- **Fix:** delete `RemoveAppService.kt` and remove its `<service>` element from the manifest.

---

## Part B: data bugs

### B1. Apps silently never saved, because new apps get colliding database ids — ✅ FIXED (`860f55b`)
- **Where:** `MainActivityCompose.kt`, `loadNewApps()`: `val id = newApps.size` and `id = id`.
- **Problem:** `AppInfo.id` is `@PrimaryKey(autoGenerate = true)`. Passing index-based ids makes inserts collide with existing rows. The `SQLiteConstraintException` is caught in `saveNewApps()`, so the app is never stored and never appears in the UI, which reads from the database Flow.
- **Fix:** in `loadNewApps()`, always pass `id = 0` (Room then auto-generates) and delete the `val id = newApps.size` line.
- Also in `loadNewApps()`, skip the launcher itself and remove duplicate packages (B2). Final loop:
  ```kotlin
  for (activity in activities) {
      val packageName = activity.applicationInfo.packageName
      if (packageName == this.packageName) continue
      if (newApps.any { it.packageName == packageName }) continue
      newApps.add(
          AppInfo(
              id = 0,
              packageName = packageName,
              label = activity.label.toString(),
              color = "",
              favorite = false,
              frequency = 0,
              lastLaunched = 0,
              hasShortcuts = false
          )
      )
  }
  ```
  The launcher's own row already in the database is removed automatically by the existing "cachedAppsNoLongerInstalled" deletion in `initializeRoom`.

### B2. Duplicate keys can crash the search overlay — ✅ FIXED (`104c104`)
- **Where:** `rainbow/SearchOverlayMaterial.kt`, `itemsIndexed(items = filteredApps, key = { index, item -> item.folderName ?: ... })`.
- **Problem:** two folders with the same name, or two rows with the same package name, produce duplicate keys, and `LazyColumn` throws.
- **Fix part 1:** make keys unique by prefixing the type and appending the index for folders:
  ```kotlin
  key = { index, item ->
      if (item.folderName != null) "folder:${item.folderName}:$index"
      else "app:${item.apps.firstOrNull()?.packageName ?: index}"
  }
  ```
- **Fix part 2 (clean up existing duplicate rows):** in `MainActivityCompose.initializeRoom()`, right after `val allAppsCached = appDao.getAll().toMutableList()`, add:
  ```kotlin
  // Remove duplicate rows for the same package left over from older versions
  allAppsCached.groupBy { it.packageName }.values
      .filter { it.size > 1 }
      .forEach { dupes -> dupes.drop(1).forEach { dup -> appDao.delete(dup); allAppsCached.remove(dup) } }
  ```
- Leave `compose/ActivitiesScreen.kt` (`key = { it.packageName }`) unchanged. B1 and part 2 remove the duplicate rows, and that screen isn't reachable anyway.

### B3. "Remove from folder" fails after the app was favorited or changed — ✅ FIXED (`4e56e72`)
- **Where:** `compose/RainbowPathScreen.kt`, function `openShortcut`, branch `index == shortcuts.size + 2`, `if (isInMap)`:
  ```kotlin
  val mapa = rainbowMape?.firstOrNull { rainbowMapa -> rainbowMapa.apps.contains(thing.apps.first()) }
  ...
  val azurirana_mapa = mapa.copy(apps = mapa.apps.minus(thing.apps.first()).toMutableList())
  ```
- **Problem:** `contains` and `minus` use full data-class equality. Folders store a JSON copy of `AppInfo`, so once any field differs (e.g. `favorite`), nothing matches.
- **Fix:** compare by package name:
  ```kotlin
  val pkg = thing.apps.first().packageName
  val mapa = rainbowMape?.firstOrNull { rainbowMapa -> rainbowMapa.apps.any { it.packageName == pkg } }
  if (mapa != null) {
      val azurirana_mapa = mapa.copy(apps = mapa.apps.filter { it.packageName != pkg }.toMutableList())
      ...
  ```
- If the user is currently inside that folder (`inFolder == true`), call `goToHome()` after removing, so the stale folder view closes.

### B4. Apps uninstalled while the launcher wasn't running stay in folders — ✅ FIXED (`21d2fda`)
- **Where:** `MainActivityCompose.initializeRoom()`, after the `for (app in cachedAppsNoLongerInstalled) { appDao.delete(app) }` loop. The folder loading (`val rainbowMape = rainbowMapaDao.getAll()...`) is further down.
- **Fix:** right after `val rainbowMape = rainbowMapaDao.getAll().toMutableList()`, add:
  ```kotlin
  val installedPackages = installedApps.map { it.packageName }.toSet()
  for (i in rainbowMape.indices) {
      val mapa = rainbowMape[i]
      val kept = mapa.apps.filter { it.packageName in installedPackages }
      if (kept.size != mapa.apps.size) {
          val updated = mapa.copy(apps = kept.toMutableList())
          rainbowMapaDao.update(updated)
          rainbowMape[i] = updated
      }
  }
  ```

### B5. ViewModel collectors pile up on every activity recreate — ✅ FIXED (`3145b40`)
- **Where:** `ViewModel.kt`, `initialize()` (launches a `combine(...).collect`) and `initializeDatabaseFlows()` (launches two collectors). `MainActivityCompose.onCreate` calls both on every create, but the ViewModel survives `recreate()`, so each recreate adds more collectors, including ones bound to a closed database.
- **Fix:** keep the jobs and cancel them before relaunching:
  ```kotlin
  private var hiddenFilterJob: kotlinx.coroutines.Job? = null
  private var dbAppsJob: kotlinx.coroutines.Job? = null
  private var dbFoldersJob: kotlinx.coroutines.Job? = null
  ```
  In `initialize()`: `hiddenFilterJob?.cancel(); hiddenFilterJob = viewModelScope.launch { ... }`.
  In `initializeDatabaseFlows()`: `dbAppsJob?.cancel(); dbAppsJob = viewModelScope.launch { ... }`, and the same with `dbFoldersJob`.

### B6. Backup closes the database and kills live updates — ✅ FIXED (`0c20b23`)
- **Where:** `MainActivityCompose.kt`, `backupLauncher`.
- **Problem:** it calls `db.close()` and `setInstanceToNull()` while the UI is still collecting Flows from that instance, so nothing updates until restart. It also does file I/O on the main thread.
- **Fix:** don't close the database. Checkpoint the WAL, then copy, on IO:
  ```kotlin
  private val backupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
          val uri = result.data?.data ?: return@registerForActivityResult
          lifecycleScope.launch(Dispatchers.IO) {
              try {
                  val db = AppDatabase.getInstance(this@MainActivityCompose)
                  db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
                  val currentDB = getDatabasePath(AppDatabase.DATABASE_NAME)
                  contentResolver.openOutputStream(uri, "w")?.use { out ->
                      FileInputStream(currentDB).use { it.copyTo(out) }
                  }
                  Log.d("MainActivityCompose", "Backup successful")
              } catch (e: Exception) {
                  Log.e("MainActivityCompose", "Backup failed", e)
              }
          }
      }
  }
  ```

### B7. Restore and "Reset database" are racy; restart the process instead — ✅ FIXED (`cecf607`)
- Add this helper to `MainActivityCompose`:
  ```kotlin
  /** Kill and relaunch the app so every database/ViewModel reference starts fresh. */
  private fun restartProcess() {
      val intent = packageManager.getLaunchIntentForPackage(packageName)!!
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      startActivity(intent)
      Runtime.getRuntime().exit(0)
  }
  ```
- **Restore (`restoreLauncher`):** on IO, call `db.close()`, delete `getDatabasePath(DATABASE_NAME).path + "-wal"` and `+ "-shm"` if they exist, copy the chosen file over the database file, then `withContext(Dispatchers.Main) { restartProcess() }`. Remove the `recreate()` call.
- **Reset (`dropDatabase`):** replace the body with:
  ```kotlin
  lifecycleScope.launch(Dispatchers.IO) {
      try {
          AppDatabase.getInstance(this@MainActivityCompose).clearAllTables()
          Log.d("MainActivityCompose", "Database dropped successfully")
      } catch (e: Exception) {
          Log.e("MainActivityCompose", "Failed to drop database", e)
      }
      withContext(Dispatchers.Main) { restartProcess() }
  }
  ```
  (No `close()` before `clearAllTables()`.)
- In `compose/GeneralSettingsScreen.kt`, the reset item's `restartAction` calls `mainActivity.dropDatabase()` then `mainActivity.recreate()`. Remove the `recreate()` line, since `dropDatabase` restarts on its own.

---

## Part C: behaviour bugs

### C1. Tapping a folder in the search overlay usually doesn't open it — ✅ FIXED (`b4ca289`)
- **Where:** `compose/RainbowPathScreen.kt`, `showSearchOverlay()`, `onAppClicked`, the `else if (app.folderName != null)` branch.
- **Problem:** it flips to favorites mode, then immediately searches a list that hasn't updated yet, so it fails and reverts. Folders that aren't favorites can never be found.
- **Fix:** open the folder directly with its apps. First add a helper above `openFolder(appIndex: Int)`:
  ```kotlin
  fun openFolderWithApps(folderApps: List<AppInfo>) {
      countdownJob?.cancel()
      pathViewRef.value?.saveScrollPosition()
      inFolder = true
      pathViewRef.value?.inFolder = true
      pathViewRef.value?.setApps(folderApps.map { EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite) })
      pathViewRef.value?.resetFolderScroll()
      pathViewRef.value?.invalidate()
  }
  ```
  Then make `openFolder(appIndex)` call `openFolderWithApps(thing.apps)` after its index checks, instead of repeating the code. Replace the whole folder branch in the search `onAppClicked` with:
  ```kotlin
  } else if (app.folderName != null) {
      if (inFolder) goToHome()
      openFolderWithApps(app.apps)
  }
  ```

### C2. Fling runs twice per frame — ✅ FIXED (`e5dc285`)
- **Where:** `compose/RainbowPathScreen.kt`, `onFlingStarted()` launches `flingJob` that calls `pathViewRef.value?.flingUpdate()` every 16 ms. `RainbowPathView` also drives the fling itself with `postOnAnimation(flingRunnable)`.
- **Fix:** in `RainbowPathScreen.kt`, make `onFlingStarted()` and `onFlingEnded()` empty (keep the overrides, since the interface requires them). Remove the `flingJob` variable and every `flingJob?.cancel()` line.
- **Note for the user:** flings will feel shorter and slower after this. If so, tune `flingFriction` / `minFlingVelocity` in `RainbowPathView`. Don't tune them in this pass.

### C3. Long-press menu can appear after the finger is lifted — ✅ FIXED (`5cfbc97`)
- **Where:** `rainbow/RainbowPathView.kt`, `onTouchEvent`, the `MotionEvent.ACTION_UP ->` branch.
- **Fix:** as the very first statement inside `ACTION_UP`, capture and clear the pending long press:
  ```kotlin
  touchedAppIndex = null  // lifting the finger cancels a pending long press (triggerLongPress checks this)
  ```
  `triggerLongPress()` already does nothing when `touchedAppIndex` is null. Check that nothing later in `ACTION_UP` reads `touchedAppIndex` (only a log line does; that's fine).

### C4. View restores folder and favorites state that the screen doesn't know about — ✅ FIXED (`5ac2888`)
- **Where:** `rainbow/RainbowPathView.kt`, `loadScrollStateFromPrefs()` and `saveScrollStateToPrefs()`.
- **Problem:** the view restores `inFolder` and `onlyFavorites` from its own prefs, while `RainbowPathScreen` keeps separate state for both. After a process restart they can disagree (e.g. the view thinks it's in a folder; back and favorites buttons then misbehave).
- **Fix:**
  - In `loadScrollStateFromPrefs()`, delete the reads of `_only_favorites`, `_in_folder` and `_current_scroll`, and the three assignments `onlyFavorites = ...`, `inFolder = ...`, `scrollOffset = ...`. Change the `if` condition to only check the three offsets.
  - In `saveScrollStateToPrefs()`, delete the `putBoolean(..._only_favorites...)`, `putBoolean(..._in_folder...)` and `putFloat(..._current_scroll...)` lines, and update the log line.
  - `setApps()` already picks `scrollOffset` from `onlyFavorites`/`inFolder`, which the screen sets, so nothing else is needed.

### C5. Icon-check flag gets stuck — ✅ FIXED (`f5e42f0`)
- **Where:** `rainbow/RainbowPathView.kt`, `iconCheckRunnable`.
- **Problem:** when `visibleAppsMissingIcons` becomes empty, the runnable stops but leaves `iconCheckActive = true`, so periodic checking never restarts.
- **Fix:**
  ```kotlin
  override fun run() {
      if (visibleAppsMissingIcons.isNotEmpty()) {
          checkAndInvalidateIfIconsLoaded()
          postDelayed(this, 100)
      } else {
          iconCheckActive = false
      }
  }
  ```

---

## Part D: minor

### D1. Feedback email is misspelled and the button does nothing on Android 11+ — ✅ FIXED (`da05ec6`)
- **Where:** `compose/GeneralSettingsScreen.kt` and `compose/HelpSettingsScreen.kt`: `"ingokodba@gamil.com"`.
- **Fix:** change to `"ingokodba@gmail.com"` in both files.
- In `app/src/main/AndroidManifest.xml`, inside the existing `<queries>` element, add:
  ```xml
  <intent>
      <action android:name="android.intent.action.SENDTO" />
      <data android:scheme="mailto" />
  </intent>
  ```

### D2. Crash handler wraps itself on every recreate and leaks activities — ✅ FIXED (`52ad6e4`)
- **Where:** `MainActivityCompose.onCreate`: `Thread.setDefaultUncaughtExceptionHandler(TopExceptionHandler(this))`. Also `TopExceptionHandler.java`.
- **Fix:**
  1. In `TopExceptionHandler.java`, change the field and constructor from `Activity app` to `Context app` and store `app.getApplicationContext()`. (`getExternalFilesDir` exists on `Context`.) Remove the now-unused `android.app.Activity` import.
  2. Move the install into `DragNavApplication`:
     ```kotlin
     @HiltAndroidApp
     class DragNavApplication : Application() {
         override fun onCreate() {
             super.onCreate()
             Thread.setDefaultUncaughtExceptionHandler(TopExceptionHandler(this))
         }
     }
     ```
  3. Delete the `Thread.setDefaultUncaughtExceptionHandler(...)` line from `MainActivityCompose.onCreate`.

### D3. "Recently used" never updates, because `addFrequency` is never called — ✅ FIXED (`cfb7f33`)
- **Where:** `MainActivityCompose.addFrequency(application: AppInfo)` has no callers.
- **Fix:**
  1. Add to `AppInfoDao` in `baza/Daovi.kt`:
     ```kotlin
     @Query("SELECT * FROM AppInfo WHERE packageName = :packageName LIMIT 1")
     fun findByPackageName(packageName: String): AppInfo?
     ```
  2. Replace `addFrequency` with a package-name version that runs on IO (folder entries hold stale copies, so look up the real row):
     ```kotlin
     fun addFrequency(packageName: String) {
         lifecycleScope.launch(Dispatchers.IO) {
             val appDao = AppDatabase.getInstance(this@MainActivityCompose).appInfoDao()
             val app = appDao.findByPackageName(packageName) ?: return@launch
             app.frequency++
             app.lastLaunched = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
             appDao.update(app)
         }
     }
     ```
  3. Call `mainActivity.addFrequency(app.packageName)` right after each `context.startActivity(launchIntent)` that launches an app in `compose/RainbowPathScreen.kt`. There are three: in `launchApp`, in the path view's `onAppClicked`, and in the search overlay's `onAppClicked`.

### D4. Hardcoded Croatian "Nova mapa" — ✅ FIXED (`f778a5c`)
- **Where:** `compose/RainbowPathScreen.kt`, `ShortcutAction("Nova mapa", ...)`.
- **Fix:** add `<string name="new_folder">New folder</string>` to `app/src/main/res/values/strings.xml` and `<string name="new_folder">Nova mapa</string>` to `app/src/main/res/values-b+hr/strings.xml`. Replace the literal with `getTranslatedString(R.string.new_folder, context)`.

### D5. Hardcoded "no" toast — ✅ FIXED (in A4)
- Already handled in A4.

### D6. Bogus `<permission>` declaration in the manifest — ✅ FIXED (`d4ec0ea`)
- **Where:** `app/src/main/AndroidManifest.xml`: `<permission android:name="android.permission.QUERY_ALL_PACKAGES" />`.
- **Problem:** `<permission>` *defines* a permission; it doesn't request one. It does nothing, and the `<queries>` MAIN intent already gives visibility of launcher apps.
- **Fix:** delete that one line. Leave the other `<permission>` lines alone.

---

## Not to be done in this pass (needs the user's decision)

Write these into `SKIPPED.md` and leave them alone:
- Making pinned shortcuts from other apps ("Add to home screen") show up in Rainbow Path (A4).
- The "one-line buttons" and "back button" settings in `GeneralSettingsScreen` have no effect anywhere. Should they be removed or implemented?
- `SearchScreen`, `ActivitiesScreen` and `ActionsScreen` can't be reached by any navigation. Delete them or wire them up?
- Hidden apps still appear inside folders and in folder search results.
- Folders store snapshots of `AppInfo` (label/favorite can go stale). The proper fix is storing package names only, which needs a database migration.
- Retuning fling feel after C2.

## Final check

1. `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug test` passes.
2. `git log --oneline` shows one commit per fix.
3. `SKIPPED.md` lists anything skipped, with the reason.
4. Report back with the list of commits and the contents of `SKIPPED.md`. Don't install the app on a device.
