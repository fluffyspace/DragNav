# Left alone on purpose

- Hidden apps stay visible inside folders and in folder search results (decided: folder membership is explicit).
- Folders store snapshots of `AppInfo` (label/favorite can go stale). Proper fix: store package names only, needs a DB migration.
- Fling feel after C2 (fling is now driven only by `RainbowPathView`); tune `flingFriction` / `minFlingVelocity` if needed.
- `modeli/Action.kt` is now unused (only the deleted ActionsScreen used it).

Notes on the pinned-shortcut feature (not tested on a device):
- A pinned shortcut is shown as a normal item (key `shortcut:<pkg>/<id>`). Long-press menu: favorite, add/remove folder, remove shortcut.
- It disappears when its app is uninstalled; unpinning from the other app's side is not observed.
