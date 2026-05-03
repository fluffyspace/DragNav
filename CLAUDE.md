# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DragNav is an alternative Android launcher (home screen) app written in Kotlin. It provides multiple UI modes for app navigation and organization, including a circle-based interface, rainbow layouts, a rainbow-path mode, and a phone keypad style.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

## Architecture

The app is a **single-Activity Jetpack Compose** application using **Navigation Compose**. There are **no Fragments** — every screen is a `@Composable`. Gesture- and canvas-heavy launcher surfaces are still implemented as custom Android `View`s and embedded in Composables via `AndroidView`.

### Entry Point
- `MainActivityCompose` (extends `AppCompatActivity`, `@AndroidEntryPoint`) is the only launcher activity. It calls `setContent { … }` with `AppNavigation`, owns the `ViewModel`, edge-to-edge insets, app loading, broadcast registration, and database wiring.
- Two small auxiliary activities exist outside the main flow: `ColorPickerActivity` and `AddShortcutActivity`.

### Navigation
- `compose/AppNavigation.kt` builds a `NavHost`. Routes are defined as type-safe sealed objects in `navigation/NavRoutes.kt`:
  `Main`, `Search`, `Activities`, `Actions`, `Settings`, `SettingsCircle`, `SettingsRainbow`, `SettingsGeneral`, `SettingsHelp`, `SettingsNotifications`, `SettingsExcludedApps`.
- The `Main` route dispatches to one of several launcher composables based on `MainActivityCompose.uiDesignMode`.

### UI Modes (`UiDesignEnum`)
- `CIRCLE`, `CIRCLE_RIGHT_HAND`, `CIRCLE_LEFT_HAND` → `CircleScreen` (with handedness flags)
- `RAINBOW_RIGHT`, `RAINBOW_LEFT` → `RainbowScreen`
- `KEYPAD` → `KeypadScreen`
- `RAINBOW_PATH` → `RainbowPathScreen`

### Compose Screens (`compose/`)
- Launcher surfaces: `MainScreen`, `CircleScreen`, `RainbowScreen`, `KeypadScreen`, `RainbowPathScreen`
- Navigation destinations: `SearchScreen`, `ActivitiesScreen` (app list), `ActionsScreen`
- Settings: `SettingsScreen`, `GeneralSettingsScreen`, `CircleSettingsScreen`, `RainbowSettingsScreen`, `NotificationSettingsScreen`, `HelpSettingsScreen`, `ExcludedAppsScreen`
- Dialogs: `EditItemDialog`, `ShortcutDialog`, `FolderNameDialog`
- Subpackages: `compose/notifications/` (notification composables), `compose/rainbowpath/` (rainbow-path event listener)

### Custom Views (embedded via `AndroidView`)
Gesture- and Canvas-driven widgets that predate the Compose migration:
- `CircleView`, `Rainbow`, `PhoneKeypadView` — the three legacy launcher surfaces, still used inside the corresponding screens
- `BottomMenuView`
- `rainbow/RainbowPathView`, `rainbow/SearchOverlay`, `rainbow/SearchOverlayMaterial`, `rainbow/PathSettingsDialog`, `rainbow/MaterialActionsDialog`

### Data Layer (`baza/`)
- **Room** (`AppDatabase.kt`, version 11, `exportSchema = true`).
- Entities: `KrugSAplikacijama`, `AppInfo`, `RainbowMapa`, `IconCacheEntry` (all under `modeli/`).
- DAOs: `krugSAplikacijamaDao`, `rainbowMapaDao`, `appInfoDao`, `iconCacheDao`. Concrete DAO files: `Daovi.kt`, `IconCacheDao.kt`. `Converters.kt` holds Room type converters.
- `KrugSAplikacijama` represents a menu item — either an app shortcut or a folder; folders nest children via the `polja` ID list.

### State Management
- `ViewModel.kt` (Hilt-injected via `viewModels()`) holds the LiveData consumed by Compose:
  - `sviKrugovi` — all menu items
  - `currentMenu` — currently displayed folder
  - `trenutnoPrikazanaPolja` — items shown in the current view
- Launcher composables observe these LiveData via `observeAsState`.

### Other Components
- `AppListener`: `BroadcastReceiver` for `PACKAGE_ADDED` / `PACKAGE_REMOVED`, registered by the activity.
- `AppLoader`, `AppManager`, `IconCache`, `IconLoadTask`: app-info and icon pipeline.
- `NotificationListener` + `NotificationData` + `NotificationUtils`: status-bar notification capture, surfaced to launcher composables.
- `RemoveAppService`, `ModelLauncherCallbacks`, `TopExceptionHandler`, `Constants`, `PreferenceKeys`, `SearchUtils`.
- `DragNavApplication` (`@HiltAndroidApp`): application class.

## Key Patterns

- **Hilt** for DI (`@HiltAndroidApp` on the application, `@AndroidEntryPoint` on activities).
- **Coroutines** with `lifecycleScope` for async DB and icon loading; DB work runs on `Dispatchers.IO`.
- **Navigation Compose** with sealed-class route objects (`NavRoute`) for type-safe navigation. (No Fragment Navigation / SafeArgs anymore.)
- **AndroidView interop**: legacy custom Views embedded inside Composable screens for gesture/canvas-heavy surfaces.
- **Mixed Croatian/English naming** throughout: `krug` = circle, `polja` = fields, `sviKrugovi` = all circles, `baza` = database, `modeli` = models, `Daovi` = DAOs.
- **Edge-to-edge** with `enableEdgeToEdge()` and `WindowInsets.systemBars` / `ime` padding handled in Compose.
- **JitPack** repository used for external dependencies (Pikolo color picker, Skydoves ColorPickerView).
