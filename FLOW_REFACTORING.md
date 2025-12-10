# Flow-Based Reactive Architecture Refactoring

## Overview
This refactoring converts the app from a manual cache-based approach to a reactive Flow-based architecture, eliminating the need for manual cache invalidation.

## Key Changes

### 1. Database Layer (Daovi.kt)
- Added Flow-based query methods to all DAOs:
  - `AppInfoDao.getAllFlow(): Flow<List<AppInfo>>`
  - `RainbowMapaDao.getAllFlow(): Flow<List<RainbowMapa>>`
  - `KrugSAplikacijamaDao.getAllFlow(): Flow<List<KrugSAplikacijama>>`

### 2. ViewModel (ViewModel.kt)
**New Flow-based State:**
- `appsListFlow: StateFlow<List<AppInfo>>` - Reactive list of apps
- `rainbowMapeFlow: StateFlow<List<RainbowMapa>>` - Reactive list of folders
- `iconsFlow: StateFlow<Map<String, Drawable?>>` - Reactive icon map
- `notificationsFlow: StateFlow<List<AppNotification>>` - Reactive notifications

**Computed Flows:**
- `rainbowFilteredFlow: StateFlow<List<EncapsulatedAppInfoWithFolder>>` - Automatically computed filtered apps based on display mode
- `rainbowAllFlow: StateFlow<List<EncapsulatedAppInfoWithFolder>>` - Automatically computed combined list of apps and folders

**New Methods:**
- `initializeDatabaseFlows(appsFlow, foldersFlow)` - Connects database flows to ViewModel state
- `setDisplayMode(mode: DisplayMode)` - Changes between ALL_APPS and FAVORITES_ONLY modes
- `computeRainbowFiltered()` - Private method to compute filtered apps
- `computeRainbowAll()` - Private method to compute all apps and folders

**Backward Compatibility:**
- Old LiveData fields are maintained and kept in sync with Flows for backward compatibility with other parts of the app

### 3. MainActivityCompose
- Added flow initialization in `onCreate()`:
  ```kotlin
  viewModel.initializeDatabaseFlows(
      appsFlow = db.appInfoDao().getAllFlow(),
      foldersFlow = db.rainbowMapaDao().getAllFlow()
  )
  ```

### 4. RainbowPathScreen
**State Collection:**
- Changed from `observeAsState()` to `collectAsStateWithLifecycle()` for better lifecycle awareness
- Now collects directly from Flow-based state:
  ```kotlin
  val appsList by viewModel.appsListFlow.collectAsStateWithLifecycle()
  val rainbowMape by viewModel.rainbowMapeFlow.collectAsStateWithLifecycle()
  val rainbowFilteredFlow by viewModel.rainbowFilteredFlow.collectAsStateWithLifecycle()
  val rainbowAllFlow by viewModel.rainbowAllFlow.collectAsStateWithLifecycle()
  ```

**Automatic UI Updates:**
- Removed manual `updateRainbowFiltered()` and `updateRainbowAll()` calls
- Created new `updateAppsFromFlows()` function that uses pre-computed Flow data
- LaunchedEffect automatically triggers UI updates when flows change
- Display mode changes automatically trigger recomputation via Flow

**Simplified Update Logic:**
- Removed all manual `updateAppsCallback()` calls
- Database changes automatically propagate through Flows to UI
- Comments added: "Flow will automatically update UI when database changes"

## Benefits

### 1. Automatic Reactivity
- Database changes automatically propagate to UI without manual cache invalidation
- No need to call `viewModel.updateRainbowFiltered()` after data changes
- Computed flows (`rainbowFilteredFlow`, `rainbowAllFlow`) automatically update when dependencies change

### 2. Cleaner Code
- Eliminated manual cache invalidation logic
- Removed redundant `updateAppsCallback()` calls throughout the codebase
- Single source of truth in database, flows handle propagation

### 3. Better Performance
- Flows emit changes only when data actually changes
- StateFlow ensures latest value is always available
- `collectAsStateWithLifecycle()` handles lifecycle properly, preventing memory leaks

### 4. Easier Maintenance
- Changes to filtering logic only need to be made in one place (`computeRainbowFiltered`)
- No risk of forgetting to invalidate caches
- Flow transformations are declarative and easier to understand

## How It Works

### Data Flow
```
Database (Room)
    ↓ (getAllFlow())
ViewModel StateFlows (appsListFlow, rainbowMapeFlow)
    ↓ (combine + computeRainbowFiltered)
Computed StateFlows (rainbowFilteredFlow, rainbowAllFlow)
    ↓ (collectAsStateWithLifecycle)
Compose UI (RainbowPathScreen)
    ↓ (updateAppsFromFlows)
RainbowPathView (Custom View)
```

### Update Triggers
1. **Database Change** → Room automatically emits new Flow value
2. **Flow Collection** → ViewModel StateFlow updates
3. **Combine Operator** → Computed flows recalculate
4. **LaunchedEffect** → UI recomposes with new data
5. **RainbowPathView** → Custom view redraws

### Display Mode Changes
1. User toggles favorites → `onlyFavorites` state changes
2. LaunchedEffect detects change → calls `viewModel.setDisplayMode()`
3. Display mode Flow updates → triggers `rainbowFilteredFlow` recomputation
4. UI automatically recomposes with filtered data

## Migration Path

### Phase 1: Flow Infrastructure (Completed)
- ✅ Add Flow methods to DAOs
- ✅ Add Flow state to ViewModel
- ✅ Create computed flows for derived state
- ✅ Initialize flows in MainActivityCompose

### Phase 2: UI Integration (Completed)
- ✅ Update RainbowPathScreen to collect from Flows
- ✅ Replace manual cache updates with Flow-based updates
- ✅ Remove redundant updateAppsCallback() calls

### Phase 3: Cleanup (Future)
- ⏳ Remove old LiveData fields once all features migrated
- ⏳ Remove deprecated methods like `updateRainbowFiltered(Boolean)`
- ⏳ Migrate other activities/fragments to Flow

## Testing Considerations

### Key Areas to Test
1. **Favorite Toggle**: Verify UI updates immediately when toggling favorites
2. **Folder Operations**:
   - Creating folders should show in UI instantly
   - Renaming folders should update immediately
   - Deleting folders should remove from UI
   - Adding apps to folders should update both folder and app list
3. **App Installation/Removal**: New/removed apps should appear/disappear automatically
4. **Search**: Search overlay should show latest data
5. **Navigation**: Entering/exiting folders should work correctly

### Potential Issues
1. **Race Conditions**: Multiple rapid changes might cause UI flicker
   - Solution: StateFlow naturally handles this by emitting latest value
2. **Memory Leaks**: Ensure Flows are properly collected with lifecycle awareness
   - Solution: Use `collectAsStateWithLifecycle()` instead of `collectAsState()`
3. **Folder View**: Folder apps are set manually, not via Flow
   - Solution: Intentional design - folders show static snapshot

## Performance Improvements

### Before (Manual Cache)
- Every data change required explicit cache invalidation
- Multiple redundant database queries
- Risk of stale data if cache not invalidated
- Manual synchronization between different data sources

### After (Flow-based)
- Single database query per Flow, automatically updated
- Computed values cached in StateFlow, recalculated only when needed
- Always fresh data from database
- Automatic synchronization via reactive streams

## Code Examples

### Before: Manual Cache Invalidation
```kotlin
fun addAppToFolder(app: AppInfo, folder: RainbowMapa) {
    folder.apps.add(app)
    rainbowMapaDao.update(folder)

    // Manual cache invalidation required!
    viewModel.updateRainbowMapa(folder)
    viewModel.updateRainbowFiltered(onlyFavorites)
    viewModel.updateRainbowAll()
    updateAppsCallback()
}
```

### After: Automatic Flow Updates
```kotlin
fun addAppToFolder(app: AppInfo, folder: RainbowMapa) {
    folder.apps.add(app)
    rainbowMapaDao.update(folder)

    // Flow automatically updates UI when database changes!
    // No manual cache invalidation needed
}
```

### Before: Computed State
```kotlin
fun updateRainbowFiltered(onlyFavorites: Boolean) {
    rainbowFiltered = if (onlyFavorites) {
        // ... filtering logic ...
    } else {
        // ... all apps logic ...
    }
}

// Must call manually after every data change
updateRainbowFiltered(onlyFavorites)
```

### After: Reactive Computed State
```kotlin
// Automatically recomputes when dependencies change
val rainbowFilteredFlow = combine(
    appsListFlow,
    rainbowMapeFlow,
    displayModeFlow
) { apps, folders, mode ->
    computeRainbowFiltered(apps, folders, mode)
}.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

// Just change display mode - filtering happens automatically
viewModel.setDisplayMode(DisplayMode.FAVORITES_ONLY)
```

## Additional Notes

- Old methods are deprecated but not removed to maintain backward compatibility
- The `setRainbowFilteredValues()` method is still used for folder navigation (intentional)
- Icons are still managed separately as they come from async loading, not database
- Notifications use Flow but are managed via broadcasts, not database
