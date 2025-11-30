# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DragNav is an alternative Android launcher (home screen) app written in Kotlin. It provides multiple UI modes for app navigation and organization, including a circle-based interface, rainbow layout, and phone keypad style.

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

### UI Modes
The launcher supports 4 UI design modes defined in `UiDesignEnum`:
- `CIRCLE` - Default circular navigation (MainFragment)
- `CIRCLE_RIGHT_HAND` - Right-handed circle variant (MainFragmentRightHand)
- `RAINBOW` - Rainbow-style layout (MainFragmentRainbow)
- `KEYPAD` - Phone keypad style (MainFragmentTipke)

All main fragments implement `MainFragmentInterface` for consistent behavior.

### Data Layer
- **Room Database** (`baza/AppDatabase.kt`): SQLite persistence with version migrations
- **Entities**:
  - `KrugSAplikacijama`: Represents a menu item (app shortcut or folder). Contains `polja` (child item IDs) for folder hierarchy.
  - `AppInfo`: Cached app information including package name, label, color, and usage frequency
- **DAOs**: `KrugSAplikacijamaDao` and `AppInfoDao` for database operations

### State Management
- `ViewModel.kt`: Holds LiveData for apps list and icons map
- `viewModel.sviKrugovi`: All menu items (folders and shortcuts)
- `viewModel.currentMenu`: Currently displayed folder
- `viewModel.trenutnoPrikazanaPolja`: Items shown in current view

### Key Components
- `MainActivity`: Central coordinator, handles fragment transactions, app loading, database operations
- `CircleView` / `Rainbow` / `PhoneKeypadView`: Custom views for each UI mode
- `AppListener`: BroadcastReceiver for app install/uninstall events
- `EventBus`: Used for communication between components (MessageEvent for app launches, drag-drop)

### Navigation Flow
`MainActivity` manages layout states via `Layouts` enum:
- `LAYOUT_MAIN` - Main launcher view
- `LAYOUT_SEARCH` - SearchFragment
- `LAYOUT_ACTIVITIES` - ActivitiesFragment (app list)
- `LAYOUT_ACTIONS` - ActionsFragment
- `LAYOUT_SETTINGS` - SettingsActivity

## Key Patterns

- Coroutines with `lifecycleScope` for async database and icon loading operations
- Data binding enabled for views
- Navigation component with SafeArgs for settings navigation
- JitPack repository used for external dependencies (Pikolo color picker, Skydoves ColorPickerView)
