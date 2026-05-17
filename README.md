# Scanables

Scanables is a Kotlin + Jetpack Compose Android app for saving, organising, and quickly displaying barcodes and QR codes.

## Current beta features

- Dark Compose UI
- Favourites section pinned at the top
- Uncategorized section under favourites
- Custom categories below that
- Collapsible sections with saved collapsed state
- Add / edit / delete scanables
- Add / rename / delete categories
- Move categories up/down
- Move scanables up/down inside their category
- Favourite/unfavourite scanables
- Camera scanning with ML Kit + CameraX
- Barcode / QR display with ZXing generation
- Local persistence using SharedPreferences JSON
- Android Auto Backup enabled for the app's saved preferences
- Scanables logo included as the app icon/resource

## Notes

This is a working beta, deliberately kept lean. True drag-and-drop is not wired yet; move up/down controls are included so ordering is already functional and the data model is ready for proper drag handles later.

## Open in Android Studio

1. Extract the zip.
2. Open the `ScanablesBeta` folder in Android Studio.
3. Let Gradle sync.
4. Run the `app` configuration on a real Android phone for camera scanning.

The scanner needs a real device camera; emulators may not behave nicely unless a camera is configured.
