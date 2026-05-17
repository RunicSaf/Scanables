package com.runic.scanables

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode as MlBarcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ScanablesApp() }
    }
}

data class ScanableItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val rawValue: String,
    val format: ScanableFormat = ScanableFormat.QR_CODE,
    val categoryId: String? = null,
    val isFavourite: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ScanableCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sortOrder: Int = 0,
    val isCollapsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class DragPreviewState(
    val item: ScanableItem,
    val topLeft: Offset,
    val widthPx: Int,
    val showFavouriteRemoveWarning: Boolean = false
)

data class ImportedScanable(
    val item: ScanableItem,
    val categoryName: String? = null
)

data class ScanablesBackup(
    val items: List<ScanableItem>,
    val categories: List<ScanableCategory>,
    val favouritesCollapsed: Boolean,
    val uncategorizedCollapsed: Boolean
)

enum class ScanableFormat {
    QR_CODE,
    CODE_128,
    EAN_13,
    EAN_8,
    UPC_A,
    UPC_E,
    DATA_MATRIX,
    PDF_417,
    UNKNOWN
}

private const val PREFS_NAME = "scanables_store"
private const val KEY_ITEMS = "items_json"
private const val KEY_CATEGORIES = "categories_json"
private const val KEY_FAV_COLLAPSED = "favourites_collapsed"
private const val KEY_UNCAT_COLLAPSED = "uncategorized_collapsed"
private const val KEY_SWIPE_TUTORIAL_SEEN = "swipe_tutorial_seen"
private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
private const val JSON_TYPE_BACKUP = "scanables_backup_v1"
private const val JSON_TYPE_SCANABLE = "scanables_item_v1"

@Composable
fun ScanablesApp() {
    val context = LocalContext.current
    val items = remember { mutableStateListOf<ScanableItem>() }
    val categories = remember { mutableStateListOf<ScanableCategory>() }

    var favouritesCollapsed by remember { mutableStateOf(false) }
    var uncategorizedCollapsed by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }

    var editingItem by remember { mutableStateOf<ScanableItem?>(null) }
    var pendingScannedValue by remember { mutableStateOf<Pair<String, ScanableFormat>?>(null) }
    var showItemDialog by remember { mutableStateOf(false) }

    var showCategoryDialog by remember { mutableStateOf(false) }
    var categoryBeingEdited by remember { mutableStateOf<ScanableCategory?>(null) }

    var showSwipeTutorial by remember { mutableStateOf(false) }
    var sharingItem by remember { mutableStateOf<ScanableItem?>(null) }

    LaunchedEffect(Unit) {
        val store = ScanablesStore(context)
        items.clear()
        items.addAll(store.loadItems())
        categories.clear()
        categories.addAll(store.loadCategories())
        favouritesCollapsed = store.loadFavouritesCollapsed()
        uncategorizedCollapsed = store.loadUncategorizedCollapsed()
        showSwipeTutorial = store.shouldShowSwipeTutorial()
    }

    fun saveAll() {
        ScanablesStore(context).saveAll(
            items = items.toList(),
            categories = categories.toList(),
            favouritesCollapsed = favouritesCollapsed,
            uncategorizedCollapsed = uncategorizedCollapsed
        )
    }

    fun importOneScanable(imported: ImportedScanable) {
        val categoryName = imported.categoryName?.trim().orEmpty()
        val targetCategoryId = if (categoryName.isNotBlank()) {
            val existingCategory = categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
            if (existingCategory != null) {
                existingCategory.id
            } else {
                val newCategory = ScanableCategory(
                    name = categoryName,
                    sortOrder = nextCategorySortOrder(categories)
                )
                categories.add(newCategory)
                newCategory.id
            }
        } else {
            null
        }

        items.add(
            imported.item.copy(
                id = UUID.randomUUID().toString(),
                categoryId = targetCategoryId,
                sortOrder = nextSortOrderForCategory(items, targetCategoryId),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        saveAll()
    }

    fun exportBackupNow() {
        val uri = saveTextExportToDocumentsScanables(
            context = context,
            fileName = "scanables-backup-${System.currentTimeMillis()}.json",
            text = createScanablesBackupJson(
                items = items.toList(),
                categories = categories.toList(),
                favouritesCollapsed = favouritesCollapsed,
                uncategorizedCollapsed = uncategorizedCollapsed
            )
        )

        if (uri == null) {
            Toast.makeText(context, "Could not export scanables", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Saved to Documents/Scanables", Toast.LENGTH_SHORT).show()
            shareExportedJson(context, uri, "Share Scanables backup")
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val backup = readTextFromUri(context, uri)?.let { parseScanablesBackupJson(it) }
            if (backup == null) {
                Toast.makeText(context, "Could not import backup", Toast.LENGTH_SHORT).show()
            } else {
                items.clear()
                items.addAll(backup.items)
                categories.clear()
                categories.addAll(backup.categories)
                favouritesCollapsed = backup.favouritesCollapsed
                uncategorizedCollapsed = backup.uncategorizedCollapsed
                saveAll()
                Toast.makeText(context, "Backup imported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportScanableNow(item: ScanableItem) {
        val categoryName = categories.firstOrNull { it.id == item.categoryId }?.name
        val uri = saveTextExportToDocumentsScanables(
            context = context,
            fileName = "scanable-${safeFileName(item.name)}-${System.currentTimeMillis()}.json",
            text = createScanableExportJson(item, categoryName)
        )

        if (uri == null) {
            Toast.makeText(context, "Could not export scanable", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Saved to Documents/Scanables", Toast.LENGTH_SHORT).show()
            shareExportedJson(context, uri, "Share scanable")
        }
    }

    val importScanableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val imported = readTextFromUri(context, uri)?.let { parseScanableExportJson(it) }
            if (imported == null) {
                Toast.makeText(context, "Could not import scanable", Toast.LENGTH_SHORT).show()
            } else {
                importOneScanable(imported)
                Toast.makeText(context, "Scanable imported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0F1117),
            surface = Color(0xFF171A22),
            surfaceVariant = Color(0xFF202532),
            primary = Color(0xFF58C7F3),
            secondary = Color(0xFF8ADFF8),
            onBackground = Color.White,
            onSurface = Color.White,
            onPrimary = Color(0xFF061018)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BackHandler(enabled = true) {
                when {
                    showItemDialog -> {
                        showItemDialog = false
                        editingItem = null
                        pendingScannedValue = null
                    }

                    showCategoryDialog -> {
                        showCategoryDialog = false
                        categoryBeingEdited = null
                    }

                    screen != AppScreen.Home -> screen = AppScreen.Home

                    else -> Unit
                }
            }

            when (val activeScreen = screen) {
                AppScreen.Home -> ScanablesHomeScreen(
                    scanables = items.sortedBy { it.sortOrder },
                    categories = categories.sortedBy { it.sortOrder },
                    favouritesCollapsed = favouritesCollapsed,
                    uncategorizedCollapsed = uncategorizedCollapsed,
                    showSwipeTutorial = showSwipeTutorial,
                    onDismissSwipeTutorial = {
                        showSwipeTutorial = false
                        ScanablesStore(context).markSwipeTutorialSeen()
                    },
                    onResetSwipeTutorial = {
                        showSwipeTutorial = true
                        ScanablesStore(context).resetSwipeTutorial()
                    },
                    onImportScanable = {
                        importScanableLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    },
                    onExportScanables = {
                        exportBackupNow()
                    },
                    onImportScanables = {
                        importBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    },
                    onShareItem = { item ->
                        sharingItem = item
                    },
                    onExportItem = { item ->
                        exportScanableNow(item)
                    },
                    onToggleFavourites = {
                        favouritesCollapsed = !favouritesCollapsed
                        saveAll()
                    },
                    onToggleUncategorized = {
                        uncategorizedCollapsed = !uncategorizedCollapsed
                        saveAll()
                    },
                    onToggleCategory = { category ->
                        val index = categories.indexOfFirst { it.id == category.id }
                        if (index >= 0) {
                            categories[index] = category.copy(
                                isCollapsed = !category.isCollapsed,
                                updatedAt = System.currentTimeMillis()
                            )
                            saveAll()
                        }
                    },
                    onAddManual = {
                        editingItem = null
                        pendingScannedValue = null
                        showItemDialog = true
                    },
                    onScan = { screen = AppScreen.Scanner },
                    onAddCategory = {
                        categoryBeingEdited = null
                        showCategoryDialog = true
                    },
                    onEditItem = { item ->
                        editingItem = item
                        pendingScannedValue = null
                        showItemDialog = true
                    },
                    onDeleteItem = { item ->
                        items.removeAll { it.id == item.id }
                        saveAll()
                    },
                    onToggleFavourite = { item ->
                        val index = items.indexOfFirst { it.id == item.id }
                        if (index >= 0) {
                            items[index] = item.copy(
                                isFavourite = !item.isFavourite,
                                updatedAt = System.currentTimeMillis()
                            )
                            saveAll()
                        }
                    },
                    onDisplayItem = { item -> screen = AppScreen.Display(item.id) },
                    onMoveItem = { item, direction ->
                        moveItemWithinCategory(items, item, direction)
                        saveAll()
                    },
                    onMoveItemToCategory = { item, targetCategoryId ->
                        val index = items.indexOfFirst { it.id == item.id }
                        if (index >= 0) {
                            items[index] = item.copy(
                                categoryId = targetCategoryId,
                                sortOrder = nextSortOrderForCategory(items, targetCategoryId),
                                updatedAt = System.currentTimeMillis()
                            )
                            saveAll()
                        }
                    },
                    onAddItemToFavourites = { item ->
                        val index = items.indexOfFirst { it.id == item.id }
                        if (index >= 0) {
                            items[index] = item.copy(
                                isFavourite = true,
                                updatedAt = System.currentTimeMillis()
                            )
                            saveAll()
                        }
                    },
                    onRemoveItemFromFavourites = { item ->
                        val index = items.indexOfFirst { it.id == item.id }
                        if (index >= 0) {
                            items[index] = item.copy(
                                isFavourite = false,
                                updatedAt = System.currentTimeMillis()
                            )
                            saveAll()
                        }
                    },
                    onEditCategory = { category ->
                        categoryBeingEdited = category
                        showCategoryDialog = true
                    },
                    onDeleteCategory = { category ->
                        categories.removeAll { it.id == category.id }
                        val movedToUncategorized = items.map { item ->
                            if (item.categoryId == category.id) {
                                item.copy(categoryId = null, updatedAt = System.currentTimeMillis())
                            } else {
                                item
                            }
                        }
                        items.clear()
                        items.addAll(movedToUncategorized)
                        saveAll()
                    }
                )

                AppScreen.Scanner -> ScannerScreen(
                    onBack = { screen = AppScreen.Home },
                    onScanned = { value, format ->
                        val imported = parseScanableExportJson(value)
                        if (imported != null) {
                            importOneScanable(imported)
                            Toast.makeText(context, "Scanable imported", Toast.LENGTH_SHORT).show()
                            screen = AppScreen.Home
                        } else {
                            pendingScannedValue = value to format
                            editingItem = null
                            showItemDialog = true
                            screen = AppScreen.Home
                        }
                    }
                )

                is AppScreen.Display -> {
                    val item = items.firstOrNull { it.id == activeScreen.itemId }
                    if (item == null) {
                        screen = AppScreen.Home
                    } else {
                        DisplayScanableScreen(
                            item = item,
                            onBack = { screen = AppScreen.Home }
                        )
                    }
                }
            }

            if (showItemDialog) {
                AddEditScanableDialog(
                    existing = editingItem,
                    scannedValue = pendingScannedValue,
                    categories = categories.sortedBy { it.sortOrder },
                    onDismiss = {
                        showItemDialog = false
                        editingItem = null
                        pendingScannedValue = null
                    },
                    onSave = { saved ->
                        val existingIndex = items.indexOfFirst { it.id == saved.id }
                        if (existingIndex >= 0) {
                            items[existingIndex] = saved.copy(updatedAt = System.currentTimeMillis())
                        } else {
                            items.add(
                                saved.copy(
                                    sortOrder = nextSortOrderForCategory(items, saved.categoryId)
                                )
                            )
                        }
                        saveAll()
                        showItemDialog = false
                        editingItem = null
                        pendingScannedValue = null
                    }
                )
            }

            if (showCategoryDialog) {
                AddEditCategoryDialog(
                    existing = categoryBeingEdited,
                    onDismiss = {
                        showCategoryDialog = false
                        categoryBeingEdited = null
                    },
                    onSave = { category ->
                        val existingIndex = categories.indexOfFirst { it.id == category.id }
                        if (existingIndex >= 0) {
                            categories[existingIndex] = category.copy(updatedAt = System.currentTimeMillis())
                        } else {
                            categories.add(category.copy(sortOrder = nextCategorySortOrder(categories)))
                        }
                        saveAll()
                        showCategoryDialog = false
                        categoryBeingEdited = null
                    }
                )
            }

            sharingItem?.let { item ->
                ShareScanableDialog(
                    item = item,
                    categoryName = categories.firstOrNull { it.id == item.categoryId }?.name,
                    onDismiss = { sharingItem = null },
                    onExport = {
                        exportScanableNow(item)
                    }
                )
            }
        }
    }
}

sealed class AppScreen {
    data object Home : AppScreen()
    data object Scanner : AppScreen()
    data class Display(val itemId: String) : AppScreen()
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanablesHomeScreen(
    scanables: List<ScanableItem>,
    categories: List<ScanableCategory>,
    favouritesCollapsed: Boolean,
    uncategorizedCollapsed: Boolean,
    showSwipeTutorial: Boolean,
    onDismissSwipeTutorial: () -> Unit,
    onResetSwipeTutorial: () -> Unit,
    onImportScanable: () -> Unit,
    onExportScanables: () -> Unit,
    onImportScanables: () -> Unit,
    onShareItem: (ScanableItem) -> Unit,
    onExportItem: (ScanableItem) -> Unit,
    onToggleFavourites: () -> Unit,
    onToggleUncategorized: () -> Unit,
    onToggleCategory: (ScanableCategory) -> Unit,
    onAddManual: () -> Unit,
    onScan: () -> Unit,
    onAddCategory: () -> Unit,
    onEditItem: (ScanableItem) -> Unit,
    onDeleteItem: (ScanableItem) -> Unit,
    onToggleFavourite: (ScanableItem) -> Unit,
    onDisplayItem: (ScanableItem) -> Unit,
    onMoveItem: (ScanableItem, Int) -> Unit,
    onMoveItemToCategory: (ScanableItem, String?) -> Unit,
    onAddItemToFavourites: (ScanableItem) -> Unit,
    onRemoveItemFromFavourites: (ScanableItem) -> Unit,
    onEditCategory: (ScanableCategory) -> Unit,
    onDeleteCategory: (ScanableCategory) -> Unit
) {
    var addMenuOpen by remember { mutableStateOf(false) }
    var appMenuOpen by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var onboardingStep by remember(showSwipeTutorial) { mutableStateOf(0) }
    var searchVisible by remember { mutableStateOf(false) }
    var searchFocused by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    fun closeSearch(clearQuery: Boolean = false) {
        focusManager.clearFocus()
        keyboardController?.hide()
        searchFocused = false
        if (clearQuery) searchQuery = ""
        if (searchQuery.isBlank() || clearQuery) searchVisible = false
    }

    LaunchedEffect(searchVisible) {
        if (searchVisible) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val trimmedSearchQuery = searchQuery.trim()
    val visibleScanables = if (trimmedSearchQuery.isBlank()) {
        scanables
    } else {
        val tokens = trimmedSearchQuery
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        scanables.filter { item ->
            val categoryName = categories.firstOrNull { it.id == item.categoryId }?.name.orEmpty()
            val haystack = listOf(
                item.name,
                item.rawValue,
                item.format.name,
                categoryName
            ).joinToString(" ").lowercase()

            tokens.all { token -> haystack.contains(token) }
        }
    }

    var dropBounds by remember { mutableStateOf<Map<String, Rect>>(emptyMap()) }
    var hoveredDropTargetId by remember { mutableStateOf<String?>(null) }
    var draggingFromFavourites by remember { mutableStateOf(false) }
    var favouriteRemovalArmed by remember { mutableStateOf(false) }
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var lastDragPointer by remember { mutableStateOf<Offset?>(null) }
    var dragPreview by remember { mutableStateOf<DragPreviewState?>(null) }
    var rootBounds by remember { mutableStateOf<Rect?>(null) }

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val rootWidthPx = rootBounds?.width ?: screenWidthPx
    val rootHeightPx = rootBounds?.height ?: screenHeightPx
    val screenMarginPx = with(density) { 8.dp.toPx() }
    val edgeScrollZonePx = with(density) { 120.dp.toPx() }
    val maxAutoScrollVelocityPxPerSecond = with(density) { 720.dp.toPx() }
    var autoScrollVelocityPxPerSecond by remember { mutableStateOf(0f) }

    BackHandler(enabled = searchVisible || searchQuery.isNotBlank()) {
        closeSearch(clearQuery = true)
    }

    fun applyHoveredDropTarget(pointer: Offset) {
        if (draggingFromFavourites) {
            hoveredDropTargetId = null
            favouriteRemovalArmed = dropBounds["favourites"]?.contains(pointer) == false
        } else {
            favouriteRemovalArmed = false
            hoveredDropTargetId = dropBounds.entries
                .firstOrNull { (_, rect) -> rect.contains(pointer) }
                ?.key
        }
    }

    fun updateHoveredDropTarget(x: Float, y: Float) {
        val pointer = Offset(x, y)
        lastDragPointer = pointer

        autoScrollVelocityPxPerSecond = when {
            y < edgeScrollZonePx -> {
                val strength = ((edgeScrollZonePx - y) / edgeScrollZonePx).coerceIn(0f, 1f)
                -maxAutoScrollVelocityPxPerSecond * strength * strength
            }

            y > rootHeightPx - edgeScrollZonePx -> {
                val strength = ((y - (rootHeightPx - edgeScrollZonePx)) / edgeScrollZonePx).coerceIn(0f, 1f)
                maxAutoScrollVelocityPxPerSecond * strength * strength
            }

            else -> 0f
        }

        applyHoveredDropTarget(pointer)
    }

    fun clampedDragTopLeft(
        finger: Offset,
        touchOffsetInCard: Offset,
        widthPx: Int
    ): Offset {
        val unclamped = Offset(
            x = finger.x - touchOffsetInCard.x,
            y = finger.y - touchOffsetInCard.y
        )
        val maxLeft = kotlin.math.max(screenMarginPx, rootWidthPx - widthPx - screenMarginPx)
        val maxTop = kotlin.math.max(screenMarginPx, rootHeightPx - 72f - screenMarginPx)
        return Offset(
            x = unclamped.x.coerceIn(screenMarginPx, maxLeft),
            y = unclamped.y.coerceIn(screenMarginPx, maxTop)
        )
    }

    fun showDragPreview(
        item: ScanableItem,
        widthPx: Int,
        touchOffsetInCard: Offset,
        finger: Offset
    ) {
        dragPreview = DragPreviewState(
            item = item,
            topLeft = clampedDragTopLeft(finger, touchOffsetInCard, widthPx),
            widthPx = widthPx,
            showFavouriteRemoveWarning = draggingFromFavourites && favouriteRemovalArmed
        )
    }

    LaunchedEffect(Unit) {
        var lastFrameTime: Long? = null

        while (true) {
            val frameTime = withFrameNanos { it }

            val previousFrameTime = lastFrameTime
            lastFrameTime = frameTime

            if (previousFrameTime == null) continue

            val deltaSeconds = ((frameTime - previousFrameTime) / 1_000_000_000f)
                .coerceIn(0f, 0.05f)

            val velocity = autoScrollVelocityPxPerSecond

            if (velocity != 0f) {
                val consumed = listState.scrollBy(velocity * deltaSeconds)

                lastDragPointer?.let { pointer ->
                    applyHoveredDropTarget(pointer)
                }

                if (consumed == 0f) {
                    autoScrollVelocityPxPerSecond = 0f
                }
            }
        }
    }

    fun finishDrag(item: ScanableItem): Boolean {
        val target = hoveredDropTargetId
        val pointer = lastDragPointer
        val wasDraggingFromFavourites = draggingFromFavourites

        autoScrollVelocityPxPerSecond = 0f
        hoveredDropTargetId = null
        draggingFromFavourites = false
        favouriteRemovalArmed = false
        draggingItemId = null
        lastDragPointer = null
        dragPreview = null

        if (wasDraggingFromFavourites) {
            val stillInsideFavourites = pointer?.let { dropBounds["favourites"]?.contains(it) } == true
            if (!stillInsideFavourites) {
                onRemoveItemFromFavourites(item)
                return true
            }
            return false
        }

        return when (target) {
            "favourites" -> {
                onAddItemToFavourites(item)
                true
            }
            "uncategorized" -> {
                onMoveItemToCategory(item, null)
                true
            }
            null -> false
            else -> {
                onMoveItemToCategory(item, target)
                true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(searchVisible, searchQuery) {
                detectTapGestures {
                    if (searchFocused || searchQuery.isNotBlank()) {
                        closeSearch(clearQuery = false)
                    }
                }
            }
            .onGloballyPositioned { coordinates ->
                rootBounds = coordinates.boundsInRoot()
            }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Scanables",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { searchVisible = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }

                        Box {
                            IconButton(onClick = { appMenuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }

                            DropdownMenu(
                                expanded = appMenuOpen,
                                onDismissRequest = { appMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = {
                                        appMenuOpen = false
                                        showSettingsDialog = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("About Scanables") },
                                    onClick = {
                                        appMenuOpen = false
                                        showAboutDialog = true
                                    }
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                Box {
                    FloatingActionButton(
                        onClick = { addMenuOpen = true },
                        containerColor = Color(0xFF58C7F3),
                        contentColor = Color(0xFF061018)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add scanable")
                    }

                    DropdownMenu(
                        expanded = addMenuOpen,
                        onDismissRequest = { addMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Scan scanable") },
                            leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                            onClick = {
                                addMenuOpen = false
                                onScan()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Enter manually") },
                            onClick = {
                                addMenuOpen = false
                                onAddManual()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Import scanable") },
                            onClick = {
                                addMenuOpen = false
                                onImportScanable()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("New category") },
                            onClick = {
                                addMenuOpen = false
                                onAddCategory()
                            }
                        )
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                if (searchVisible || searchQuery.isNotBlank()) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search") },
                            placeholder = { Text("Search scanables") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { closeSearch(clearQuery = true) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search")
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester)
                                .onFocusChanged { searchFocused = it.isFocused }
                        )
                    }
                }

                item {
                    val favouriteItems = visibleScanables.filter { it.isFavourite }

                    ScanableSection(
                        title = "Favourites",
                        count = favouriteItems.size,
                        collapsed = favouritesCollapsed,
                        fixed = true,
                        onToggle = onToggleFavourites,
                        dropTargetId = "favourites",
                        isDropHovered = hoveredDropTargetId == "favourites",
                        onDropTargetPositioned = { id, rect ->
                            dropBounds = dropBounds + (id to rect)
                        }
                    ) {
                        if (favouriteItems.isEmpty()) {
                            EmptySectionText(if (trimmedSearchQuery.isBlank()) "Favourite scanables for quick access." else "No matching favourites.")
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                favouriteItems.forEach { item ->
                                    ScanableCard(
                                        item = item,
                                        onClick = { onDisplayItem(item) },
                                        onEdit = { onEditItem(item) },
                                        onDelete = { onDeleteItem(item) },
                                        onShare = { onShareItem(item) },
                                        onExport = { onExportItem(item) },
                                        onToggleFavourite = { onToggleFavourite(item) },
                                        onMoveUp = { onMoveItem(item, -1) },
                                        onMoveDown = { onMoveItem(item, 1) },
                                        onDragStarted = { widthPx, _, touchOffset, finger ->
                                            draggingFromFavourites = true
                                            favouriteRemovalArmed = false
                                            draggingItemId = item.id
                                            autoScrollVelocityPxPerSecond = 0f
                                            hoveredDropTargetId = null
                                            lastDragPointer = null
                                            updateHoveredDropTarget(finger.x, finger.y)
                                            showDragPreview(item, widthPx, touchOffset, finger)
                                        },
                                        onDragMoved = { finger, touchOffset, widthPx, _ ->
                                            updateHoveredDropTarget(finger.x, finger.y)
                                            showDragPreview(item, widthPx, touchOffset, finger)
                                        },
                                        onDragFinished = { finishDrag(item) },
                                        onDragCancelled = {
                                            draggingFromFavourites = false
                                            favouriteRemovalArmed = false
                                            draggingItemId = null
                                            autoScrollVelocityPxPerSecond = 0f
                                            hoveredDropTargetId = null
                                            lastDragPointer = null
                                            dragPreview = null
                                        },
                                        showFavouriteRemoveWarning = draggingItemId == item.id && favouriteRemovalArmed
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    val uncategorizedItems = visibleScanables.filter { it.categoryId == null }

                    ScanableSection(
                        title = "Uncategorized",
                        count = uncategorizedItems.size,
                        collapsed = uncategorizedCollapsed,
                        fixed = true,
                        onToggle = onToggleUncategorized,
                        dropTargetId = "uncategorized",
                        isDropHovered = hoveredDropTargetId == "uncategorized",
                        onDropTargetPositioned = { id, rect ->
                            dropBounds = dropBounds + (id to rect)
                        }
                    ) {
                        if (uncategorizedItems.isEmpty()) {
                            EmptySectionText(if (trimmedSearchQuery.isBlank()) "New scanables without a category appear here." else "No matching uncategorized scanables.")
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uncategorizedItems.forEach { item ->
                                    ScanableCard(
                                        item = item,
                                        onClick = { onDisplayItem(item) },
                                        onEdit = { onEditItem(item) },
                                        onDelete = { onDeleteItem(item) },
                                        onShare = { onShareItem(item) },
                                        onExport = { onExportItem(item) },
                                        onToggleFavourite = { onToggleFavourite(item) },
                                        onMoveUp = { onMoveItem(item, -1) },
                                        onMoveDown = { onMoveItem(item, 1) },
                                        onDragStarted = { widthPx, _, touchOffset, finger ->
                                            draggingFromFavourites = false
                                            favouriteRemovalArmed = false
                                            draggingItemId = item.id
                                            autoScrollVelocityPxPerSecond = 0f
                                            hoveredDropTargetId = null
                                            lastDragPointer = null
                                            updateHoveredDropTarget(finger.x, finger.y)
                                            showDragPreview(item, widthPx, touchOffset, finger)
                                        },
                                        onDragMoved = { finger, touchOffset, widthPx, _ ->
                                            updateHoveredDropTarget(finger.x, finger.y)
                                            showDragPreview(item, widthPx, touchOffset, finger)
                                        },
                                        onDragFinished = { finishDrag(item) },
                                        onDragCancelled = {
                                            draggingFromFavourites = false
                                            favouriteRemovalArmed = false
                                            draggingItemId = null
                                            autoScrollVelocityPxPerSecond = 0f
                                            hoveredDropTargetId = null
                                            lastDragPointer = null
                                            dragPreview = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (categories.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF171A22)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("No custom categories yet", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Create your first category below.",
                                    color = Color(0xFFB9C2CC)
                                )
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = onAddCategory) {
                                    Text("Add category")
                                }
                            }
                        }
                    }
                }

                items(categories, key = { it.id }) { category ->
                    val categoryItems = visibleScanables.filter { it.categoryId == category.id }

                    ScanableSection(
                        title = category.name,
                        count = categoryItems.size,
                        collapsed = category.isCollapsed,
                        fixed = false,
                        onToggle = { onToggleCategory(category) },
                        dropTargetId = category.id,
                        isDropHovered = hoveredDropTargetId == category.id,
                        onDropTargetPositioned = { id, rect ->
                            dropBounds = dropBounds + (id to rect)
                        },
                        categoryMenu = {
                            CategoryMenu(
                                onEdit = { onEditCategory(category) },
                                onDelete = { onDeleteCategory(category) }
                            )
                        }
                    ) {
                        if (categoryItems.isEmpty()) {
                            EmptySectionText(if (trimmedSearchQuery.isBlank()) "No scanables in this category yet." else "No matching scanables in this category.")
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                categoryItems.forEach { item ->
                                    ScanableCard(
                                        item = item,
                                        onClick = { onDisplayItem(item) },
                                        onEdit = { onEditItem(item) },
                                        onDelete = { onDeleteItem(item) },
                                        onShare = { onShareItem(item) },
                                        onExport = { onExportItem(item) },
                                        onToggleFavourite = { onToggleFavourite(item) },
                                        onMoveUp = { onMoveItem(item, -1) },
                                        onMoveDown = { onMoveItem(item, 1) },
                                        onDragStarted = { widthPx, _, touchOffset, finger ->
                                            draggingFromFavourites = false
                                            favouriteRemovalArmed = false
                                            draggingItemId = item.id
                                            autoScrollVelocityPxPerSecond = 0f
                                            hoveredDropTargetId = null
                                            lastDragPointer = null
                                            updateHoveredDropTarget(finger.x, finger.y)
                                            showDragPreview(item, widthPx, touchOffset, finger)
                                        },
                                        onDragMoved = { finger, touchOffset, widthPx, _ ->
                                            updateHoveredDropTarget(finger.x, finger.y)
                                            showDragPreview(item, widthPx, touchOffset, finger)
                                        },
                                        onDragFinished = { finishDrag(item) },
                                        onDragCancelled = {
                                            draggingFromFavourites = false
                                            favouriteRemovalArmed = false
                                            draggingItemId = null
                                            autoScrollVelocityPxPerSecond = 0f
                                            hoveredDropTargetId = null
                                            lastDragPointer = null
                                            dragPreview = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        dragPreview?.let { preview ->
            FloatingScanableCard(
                state = preview.copy(
                    showFavouriteRemoveWarning = draggingFromFavourites && favouriteRemovalArmed
                )
            )
        }
    }

    if (showAboutDialog) {
        AboutScanablesDialog(onDismiss = { showAboutDialog = false })
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onExportScanables = onExportScanables,
            onImportScanables = onImportScanables,
            onShowTutorial = onResetSwipeTutorial
        )
    }

    if (showSwipeTutorial) {
        OnboardingTutorialDialog(
            step = onboardingStep,
            onBack = {
                onboardingStep = (onboardingStep - 1).coerceAtLeast(0)
            },
            onNext = {
                if (onboardingStep < 5) {
                    onboardingStep += 1
                } else {
                    onDismissSwipeTutorial()
                }
            },
            onDismiss = onDismissSwipeTutorial
        )
    }
}

@Composable
fun ScanableSection(
    title: String,
    count: Int,
    collapsed: Boolean,
    fixed: Boolean,
    onToggle: () -> Unit,
    categoryMenu: (@Composable () -> Unit)? = null,
    dropTargetId: String? = null,
    isDropHovered: Boolean = false,
    onDropTargetPositioned: (String, Rect) -> Unit = { _, _ -> },
    content: @Composable () -> Unit
) {
    val isFavourites = title == "Favourites"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (dropTargetId != null) {
                    Modifier.onGloballyPositioned { coordinates ->
                        onDropTargetPositioned(
                            dropTargetId,
                            coordinates.boundsInRoot()
                        )
                    }
                } else {
                    Modifier
                }
            )
            .border(
                width = if (isDropHovered) 2.dp else 0.dp,
                color = if (isDropHovered) Color(0xFF58C7F3) else Color.Transparent,
                shape = RoundedCornerShape(22.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isDropHovered) Color(0xFF202B3A) else Color(0xFF171A22)
        ),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = null,
                    tint = if (isDropHovered) Color(0xFF58C7F3) else Color(0xFF40C7F4)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isDropHovered) {
                            if (isFavourites) "Release to add to favourites" else "Release to move here"
                        } else {
                            when {
                                isFavourites -> "$count quick access scanable${if (count == 1) "" else "s"}"
                                title == "Uncategorized" -> "$count uncategorized scanable${if (count == 1) "" else "s"}"
                                else -> "$count scanable${if (count == 1) "" else "s"}"
                            }
                        },
                        color = if (isDropHovered) Color(0xFF58C7F3) else Color(0xFFB9C2CC),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isDropHovered) FontWeight.Bold else FontWeight.Normal
                    )
                }
                categoryMenu?.invoke()
            }

            if (!collapsed) {
                Divider(color = Color(0xFF252A32))
                Column(Modifier.padding(12.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun CategoryMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var open by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Category menu")
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    open = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete category") },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = {
                    open = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
fun ScanableCard(
    item: ScanableItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onToggleFavourite: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragStarted: (Int, Int, Offset, Offset) -> Unit,
    onDragMoved: (Offset, Offset, Int, Int) -> Unit,
    onDragFinished: () -> Boolean,
    onDragCancelled: () -> Unit = {},
    showFavouriteRemoveWarning: Boolean = false
) {
    var menuOpen by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var cardBounds by remember { mutableStateOf<Rect?>(null) }
    var fingerPositionInRoot by remember { mutableStateOf<Offset?>(null) }
    var dragStartOffsetInCard by remember { mutableStateOf(Offset.Zero) }
    var cardWidthPx by remember { mutableStateOf(0) }
    var cardHeightPx by remember { mutableStateOf(0) }

    val swipeThreshold = 140f
    val reorderDragThreshold = 70f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(item.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            offsetX > swipeThreshold -> onEdit()
                            offsetX < -swipeThreshold -> onDelete()
                        }
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (!isDragging) {
                            offsetX = (offsetX + dragAmount).coerceIn(-220f, 220f)
                        }
                    }
                )
            }
    ) {
        val isSwiping = !isDragging && offsetX != 0f

        Row(
            modifier = Modifier
                .matchParentSize()
                .background(
                    color = when {
                        !isSwiping -> Color.Transparent
                        offsetX < 0 -> Color(0xFF3A171A)
                        else -> Color(0xFF173A27)
                    },
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (offsetX < 0) Arrangement.End else Arrangement.Start
        ) {
            if (isSwiping) {
                Text(
                    text = if (offsetX < 0) "Delete" else "Edit",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isDragging) 0f else 1f)
                .onGloballyPositioned { coordinates ->
                    cardBounds = coordinates.boundsInRoot()
                    cardWidthPx = coordinates.size.width
                    cardHeightPx = coordinates.size.height
                }
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(item.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { localOffset ->
                            val bounds = cardBounds ?: return@detectDragGesturesAfterLongPress
                            isDragging = true
                            offsetX = 0f
                            dragOffsetY = 0f
                            dragStartOffsetInCard = localOffset
                            val finger = Offset(bounds.left + localOffset.x, bounds.top + localOffset.y)
                            fingerPositionInRoot = finger
                            onDragStarted(cardWidthPx, cardHeightPx, localOffset, finger)
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffsetY = 0f
                            fingerPositionInRoot = null
                            onDragCancelled()
                        },
                        onDragEnd = {
                            val droppedOnTarget = onDragFinished()
                            if (!droppedOnTarget) {
                                when {
                                    dragOffsetY < -reorderDragThreshold -> onMoveUp()
                                    dragOffsetY > reorderDragThreshold -> onMoveDown()
                                }
                            }
                            isDragging = false
                            dragOffsetY = 0f
                            fingerPositionInRoot = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY += dragAmount.y
                            val currentFinger = fingerPositionInRoot
                            if (currentFinger != null) {
                                val updatedFinger = Offset(
                                    x = currentFinger.x + dragAmount.x,
                                    y = currentFinger.y + dragAmount.y
                                )
                                fingerPositionInRoot = updatedFinger
                                onDragMoved(updatedFinger, dragStartOffsetInCard, cardWidthPx, cardHeightPx)
                            }
                        }
                    )
                }
                .clickable(enabled = !isDragging) { onClick() },
            colors = CardDefaults.cardColors(containerColor = if (isDragging) Color(0xFF273044) else Color(0xFF202532)),
            shape = RoundedCornerShape(18.dp)
        ) {
            ScanableCardRow(
                item = item,
                menuOpen = menuOpen,
                onMenuOpenChange = { menuOpen = it },
                onEdit = onEdit,
                onDelete = onDelete,
                onShare = onShare,
                onExport = onExport,
                onToggleFavourite = onToggleFavourite,
                showMenu = true
            )
        }
    }
}

@Composable
fun FloatingScanableCard(state: DragPreviewState) {
    val density = LocalDensity.current

    Card(
        modifier = Modifier
            .offset {
                IntOffset(
                    state.topLeft.x.roundToInt(),
                    state.topLeft.y.roundToInt()
                )
            }
            .width(with(density) { state.widthPx.toDp() })
            .border(
                width = if (state.showFavouriteRemoveWarning) 2.dp else 0.dp,
                color = if (state.showFavouriteRemoveWarning) Color(0xFFFF6B6B) else Color.Transparent,
                shape = RoundedCornerShape(18.dp)
            )
            .zIndex(99f),
        colors = CardDefaults.cardColors(
            containerColor = if (state.showFavouriteRemoveWarning) Color(0xFF3A2024) else Color(0xFF273044)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        ScanableCardRow(
            item = state.item,
            menuOpen = false,
            onMenuOpenChange = {},
            onEdit = {},
            onDelete = {},
            onShare = {},
            onExport = {},
            onToggleFavourite = {},
            showMenu = false
        )
    }
}

@Composable
fun ScanableCardRow(
    item: ScanableItem,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onToggleFavourite: () -> Unit,
    showMenu: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color(0xFF0F1117), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (item.format == ScanableFormat.QR_CODE) "QR" else "||",
                color = Color(0xFF40C7F4),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.format.name} • ${item.rawValue}",
                color = Color(0xFFB9C2CC),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        }

        IconButton(onClick = onToggleFavourite, enabled = showMenu) {
            Icon(
                imageVector = if (item.isFavourite) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = "Favourite",
                tint = if (item.isFavourite) Color(0xFF40C7F4) else Color(0xFFB9C2CC)
            )
        }

        if (showMenu) {
            Box {
                IconButton(onClick = { onMenuOpenChange(true) }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Item menu")
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { onMenuOpenChange(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            onMenuOpenChange(false)
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            onMenuOpenChange(false)
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onMenuOpenChange(false)
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

fun writeTextToUri(context: Context, uri: Uri, text: String) {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(text.toByteArray(Charsets.UTF_8))
    }
}

fun readTextFromUri(context: Context, uri: Uri): String? =
    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }

fun saveTextExportToDocumentsScanables(
    context: Context,
    fileName: String,
    text: String
): Uri? {
    return try {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOCUMENTS + "/Scanables"
            )
        }

        val uri = context.contentResolver.insert(
            MediaStore.Files.getContentUri("external"),
            values
        ) ?: return null

        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
        } ?: return null

        uri
    } catch (_: Exception) {
        null
    }
}

fun shareExportedJson(context: Context, uri: Uri, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, title))
}

fun safeFileName(name: String): String {
    val cleaned = name
        .lowercase()
        .replace(Regex("[^a-z0-9-_]+"), "-")
        .trim('-')
    return cleaned.ifBlank { "scanable" }
}

fun createScanableExportJson(item: ScanableItem, categoryName: String?): String =
    JSONObject()
        .put("type", JSON_TYPE_SCANABLE)
        .put("version", 1)
        .put("categoryName", categoryName ?: "")
        .put(
            "item",
            JSONObject()
                .put("name", item.name)
                .put("rawValue", item.rawValue)
                .put("format", item.format.name)
                .put("isFavourite", item.isFavourite)
                .put("sortOrder", item.sortOrder)
                .put("createdAt", item.createdAt)
                .put("updatedAt", item.updatedAt)
        )
        .toString()

fun parseScanableExportJson(json: String): ImportedScanable? = try {
    val root = JSONObject(json)
    if (root.optString("type") != JSON_TYPE_SCANABLE) {
        null
    } else {
        val obj = root.getJSONObject("item")
        ImportedScanable(
            item = ScanableItem(
                name = obj.getString("name"),
                rawValue = obj.getString("rawValue"),
                format = obj.optString("format").toScanableFormat(),
                categoryId = null,
                isFavourite = obj.optBoolean("isFavourite", false),
                sortOrder = obj.optInt("sortOrder", 0),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            ),
            categoryName = root.optString("categoryName").takeIf { it.isNotBlank() }
        )
    }
} catch (_: Exception) {
    null
}

fun createScanablesBackupJson(
    items: List<ScanableItem>,
    categories: List<ScanableCategory>,
    favouritesCollapsed: Boolean,
    uncategorizedCollapsed: Boolean
): String =
    JSONObject()
        .put("type", JSON_TYPE_BACKUP)
        .put("version", 1)
        .put("items", items.scanableItemsToJson())
        .put("categories", categories.scanableCategoriesToJson())
        .put("favouritesCollapsed", favouritesCollapsed)
        .put("uncategorizedCollapsed", uncategorizedCollapsed)
        .toString()

fun parseScanablesBackupJson(json: String): ScanablesBackup? = try {
    val root = JSONObject(json)
    if (root.optString("type") != JSON_TYPE_BACKUP) {
        null
    } else {
        ScanablesBackup(
            items = root.getJSONArray("items").toScanableItems(),
            categories = root.getJSONArray("categories").toScanableCategories(),
            favouritesCollapsed = root.optBoolean("favouritesCollapsed", false),
            uncategorizedCollapsed = root.optBoolean("uncategorizedCollapsed", false)
        )
    }
} catch (_: Exception) {
    null
}

fun JSONArray.toScanableItems(): List<ScanableItem> = buildList {
    for (i in 0 until length()) {
        val obj = getJSONObject(i)
        add(
            ScanableItem(
                id = obj.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                name = obj.getString("name"),
                rawValue = obj.getString("rawValue"),
                format = obj.optString("format").toScanableFormat(),
                categoryId = obj.optString("categoryId").takeIf { it.isNotBlank() },
                isFavourite = obj.optBoolean("isFavourite"),
                sortOrder = obj.optInt("sortOrder"),
                createdAt = obj.optLong("createdAt"),
                updatedAt = obj.optLong("updatedAt")
            )
        )
    }
}

fun JSONArray.toScanableCategories(): List<ScanableCategory> = buildList {
    for (i in 0 until length()) {
        val obj = getJSONObject(i)
        add(
            ScanableCategory(
                id = obj.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                name = obj.getString("name"),
                sortOrder = obj.optInt("sortOrder"),
                isCollapsed = obj.optBoolean("isCollapsed"),
                createdAt = obj.optLong("createdAt"),
                updatedAt = obj.optLong("updatedAt")
            )
        )
    }
}


fun inferScanableFormat(value: String): ScanableFormat {
    val cleaned = value.trim()
    val digitsOnly = cleaned.all { it.isDigit() }

    return when {
        cleaned.startsWith("http://", ignoreCase = true) ||
                cleaned.startsWith("https://", ignoreCase = true) ||
                cleaned.contains("\n") ||
                cleaned.contains(" ") -> ScanableFormat.QR_CODE

        digitsOnly && cleaned.length == 13 -> ScanableFormat.EAN_13
        digitsOnly && cleaned.length == 12 -> ScanableFormat.UPC_A
        digitsOnly && cleaned.length == 8 -> ScanableFormat.EAN_8
        digitsOnly && cleaned.length == 6 -> ScanableFormat.UPC_E

        cleaned.length > 80 -> ScanableFormat.QR_CODE

        else -> ScanableFormat.CODE_128
    }
}

@Composable
fun EmptySectionText(text: String) {
    Text(
        text = text,
        color = Color(0xFFB9C2CC),
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
fun OnboardingTutorialDialog(
    step: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val title: String
    val body: String
    val icon: String

    when (step) {
        0 -> {
            icon = "▣"
            title = "Welcome to Scanables"
            body = "Save barcodes and QR codes, then tap a scanable to display it when you need it."
        }

        1 -> {
            icon = "★"
            title = "Favourites"
            body = "Press the star, or drag a scanable into Favourites, to keep it at the top for quick access."
        }

        2 -> {
            icon = "+"
            title = "Categories"
            body = "Create categories to group your scanables. Use the plus button, then choose New category."
        }

        3 -> {
            icon = "↕"
            title = "Drag to organise"
            body = "Long-press and drag a scanable over a category. Release when the category highlights."
        }

        4 -> {
            icon = "→"
            title = "Swipe to edit"
            body = "Swipe a scanable to the right to edit its name, value, category, favourite state, or advanced format."
        }

        else -> {
            icon = "←"
            title = "Swipe to delete"
            body = "Swipe a scanable to the left to delete it. You can reopen this tutorial from the three-dot menu."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color(0xFF171A22),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color(0xFF202532), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = icon,
                            color = Color(0xFF58C7F3),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = body,
                        color = Color(0xFFE7EDF4),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Step ${step + 1} of 6",
                    color = Color(0xFFB9C2CC),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onNext) {
                Text(if (step == 5) "Finish" else "Next")
            }
        },
        dismissButton = {
            Row {
                if (step > 0) {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Skip")
                }
            }
        }
    )
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onExportScanables: () -> Unit,
    onImportScanables: () -> Unit,
    onShowTutorial: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color(0xFF171A22),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Text(
                text = "Settings",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onExportScanables()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export scanables")
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onImportScanables()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import scanables")
                }

                Text(
                    text = "Importing a backup replaces the current scanables and categories on this device.",
                    color = Color(0xFFB9C2CC),
                    style = MaterialTheme.typography.bodySmall
                )

                Divider(color = Color(0xFF252A32))

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onShowTutorial()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Show tutorial")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ShareScanableDialog(
    item: ScanableItem,
    categoryName: String?,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    val shareJson = remember(item, categoryName) { createScanableExportJson(item, categoryName) }
    val qrBitmap = remember(shareJson) { createBarcodeBitmap(shareJson, ScanableFormat.QR_CODE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color(0xFF171A22),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Text(
                text = "Share scanable",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold
                )

                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(22.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Share ${item.name}",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(
                        text = "This scanable is too large to share as a QR code. Export it instead.",
                        color = Color(0xFFB9C2CC)
                    )
                }

                Text(
                    text = "Scan this QR code with Scanables to import this card.",
                    color = Color(0xFFB9C2CC),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onExport) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AboutScanablesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color(0xFF171A22),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Text(
                text = "About Scanables",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Scanables lets you save, organise, and display barcodes and QR codes.",
                    color = Color(0xFFE7EDF4)
                )

                Text(
                    text = "Made by Saf and Nyxen.",
                    color = Color(0xFFB9C2CC),
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Scanables V1.1",
                    color = Color(0xFFB9C2CC),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AddEditScanableDialog(
    existing: ScanableItem?,
    scannedValue: Pair<String, ScanableFormat>?,
    categories: List<ScanableCategory>,
    onDismiss: () -> Unit,
    onSave: (ScanableItem) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var rawValue by remember { mutableStateOf(existing?.rawValue ?: scannedValue?.first ?: "") }

    var format by remember {
        mutableStateOf(
            existing?.format
                ?: scannedValue?.second
                ?: inferScanableFormat(rawValue)
        )
    }

    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var favourite by remember { mutableStateOf(existing?.isFavourite ?: false) }

    var categoryMenuOpen by remember { mutableStateOf(false) }
    var formatMenuOpen by remember { mutableStateOf(false) }
    var advancedOpen by remember { mutableStateOf(false) }

    val selectedCategoryName = categories.firstOrNull { it.id == categoryId }?.name ?: "Uncategorized"

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color(0xFF171A22),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existing == null) "Add scanable" else "Edit scanable",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { favourite = !favourite }) {
                    Icon(
                        imageVector = if (favourite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (favourite) "Remove favourite" else "Add favourite",
                        tint = if (favourite) Color(0xFF58C7F3) else Color(0xFFB9C2CC)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rawValue,
                    onValueChange = {
                        rawValue = it
                        if (existing == null && scannedValue == null) {
                            format = inferScanableFormat(it)
                        }
                    },
                    label = { Text("Code value") },
                    placeholder = { Text("Barcode or QR contents") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF202532)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryMenuOpen = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Category",
                                color = Color(0xFFB9C2CC),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = selectedCategoryName,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF58C7F3)
                        )

                        DropdownMenu(
                            expanded = categoryMenuOpen,
                            onDismissRequest = { categoryMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Uncategorized") },
                                onClick = {
                                    categoryId = null
                                    categoryMenuOpen = false
                                }
                            )

                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        categoryId = category.id
                                        categoryMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF202532)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { advancedOpen = !advancedOpen },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Advanced", fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Detected format: ${format.name}",
                                    color = Color(0xFFB9C2CC),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Icon(
                                imageVector = if (advancedOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF58C7F3)
                            )
                        }

                        if (advancedOpen) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Only change this if the generated code does not scan correctly.",
                                color = Color(0xFFB9C2CC),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(8.dp))
                            Box {
                                AssistChip(
                                    onClick = { formatMenuOpen = true },
                                    label = { Text("Format: ${format.name}") }
                                )

                                DropdownMenu(
                                    expanded = formatMenuOpen,
                                    onDismissRequest = { formatMenuOpen = false }
                                ) {
                                    ScanableFormat.entries
                                        .filter { it != ScanableFormat.UNKNOWN }
                                        .forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option.name) },
                                                onClick = {
                                                    format = option
                                                    formatMenuOpen = false
                                                }
                                            )
                                        }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && rawValue.isNotBlank(),
                onClick = {
                    onSave(
                        ScanableItem(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            rawValue = rawValue.trim(),
                            format = format,
                            categoryId = categoryId,
                            isFavourite = favourite,
                            sortOrder = existing?.sortOrder ?: 0,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddEditCategoryDialog(
    existing: ScanableCategory?,
    onDismiss: () -> Unit,
    onSave: (ScanableCategory) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New category" else "Rename category") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        ScanableCategory(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            sortOrder = existing?.sortOrder ?: 0,
                            isCollapsed = existing?.isCollapsed ?: false,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayScanableScreen(item: ScanableItem, onBack: () -> Unit) {
    val activity = LocalContext.current as? Activity

    DisposableEffect(Unit) {
        val original = activity?.window?.attributes?.screenBrightness
            ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

        activity?.window?.attributes = activity?.window?.attributes?.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        }

        onDispose {
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = original
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        item.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                item.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(24.dp))

            val bitmap = remember(item) { createBarcodeBitmap(item.rawValue, item.format) }

            if (bitmap == null) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1618))) {
                    Text(
                        "Could not generate this format. Try QR_CODE or CODE_128.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = item.rawValue,
                color = Color(0xFFB9C2CC),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onScanned: (String, ScanableFormat) -> Unit
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan barcode / QR") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasPermission) {
                BarcodeCameraPreview(onScanned = onScanned)

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(260.dp)
                        .border(
                            width = 3.dp,
                            color = Color(0xFF58C7F3),
                            shape = RoundedCornerShape(28.dp)
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(18.dp)
                        .background(Color(0xCC171A22), RoundedCornerShape(999.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Scanning…",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Camera permission is needed to scan codes.")

                    Spacer(Modifier.height(12.dp))

                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant permission")
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeCameraPreview(onScanned: (String, ScanableFormat) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scanCompleted by remember { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            val executor = Executors.newSingleThreadExecutor()

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val scanner = BarcodeScanning.getClient()

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            val image = imageProxy.toInputImageOrNull()

                            if (image == null || scanCompleted) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    val barcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                                    val value = barcode?.rawValue

                                    if (!value.isNullOrBlank() && !scanCompleted) {
                                        scanCompleted = true
                                        onScanned(value, mapMlKitFormat(barcode))
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (_: Exception) {
                    // Keep scanner screen alive instead of crashing if camera binding fails.
                }
            }, ContextCompat.getMainExecutor(context))

            previewView
        }
    )
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun ImageProxy.toInputImageOrNull(): InputImage? {
    val mediaImage = image ?: return null

    return InputImage.fromMediaImage(
        mediaImage,
        imageInfo.rotationDegrees
    )
}

fun createBarcodeBitmap(value: String, format: ScanableFormat): Bitmap? {
    return try {
        val zxingFormat = when (format) {
            ScanableFormat.QR_CODE -> BarcodeFormat.QR_CODE
            ScanableFormat.CODE_128 -> BarcodeFormat.CODE_128
            ScanableFormat.EAN_13 -> BarcodeFormat.EAN_13
            ScanableFormat.EAN_8 -> BarcodeFormat.EAN_8
            ScanableFormat.UPC_A -> BarcodeFormat.UPC_A
            ScanableFormat.UPC_E -> BarcodeFormat.UPC_E
            ScanableFormat.DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
            ScanableFormat.PDF_417 -> BarcodeFormat.PDF_417
            ScanableFormat.UNKNOWN -> BarcodeFormat.QR_CODE
        }

        val isSquare = zxingFormat == BarcodeFormat.QR_CODE ||
                zxingFormat == BarcodeFormat.DATA_MATRIX

        val width = if (isSquare) 900 else 1200
        val height = if (isSquare) 900 else 420
        val hints = mapOf(EncodeHintType.MARGIN to 2)

        val matrix = MultiFormatWriter().encode(
            value,
            zxingFormat,
            width,
            height,
            hints
        )

        bitMatrixToBitmap(matrix)
    } catch (_: Exception) {
        null
    }
}

fun bitMatrixToBitmap(matrix: BitMatrix): Bitmap {
    val width = matrix.width
    val height = matrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(
                x,
                y,
                if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            )
        }
    }

    return bitmap
}

fun mapMlKitFormat(barcode: MlBarcode): ScanableFormat = when (barcode.format) {
    MlBarcode.FORMAT_QR_CODE -> ScanableFormat.QR_CODE
    MlBarcode.FORMAT_CODE_128 -> ScanableFormat.CODE_128
    MlBarcode.FORMAT_EAN_13 -> ScanableFormat.EAN_13
    MlBarcode.FORMAT_EAN_8 -> ScanableFormat.EAN_8
    MlBarcode.FORMAT_UPC_A -> ScanableFormat.UPC_A
    MlBarcode.FORMAT_UPC_E -> ScanableFormat.UPC_E
    MlBarcode.FORMAT_DATA_MATRIX -> ScanableFormat.DATA_MATRIX
    MlBarcode.FORMAT_PDF417 -> ScanableFormat.PDF_417
    else -> ScanableFormat.QR_CODE
}

class ScanablesStore(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadItems(): List<ScanableItem> {
        val json = prefs.getString(KEY_ITEMS, null) ?: return emptyList()

        return try {
            val array = JSONArray(json)

            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)

                    add(
                        ScanableItem(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            rawValue = obj.getString("rawValue"),
                            format = obj.optString("format").toScanableFormat(),
                            categoryId = obj.optString("categoryId").takeIf { it.isNotBlank() },
                            isFavourite = obj.optBoolean("isFavourite"),
                            sortOrder = obj.optInt("sortOrder"),
                            createdAt = obj.optLong("createdAt"),
                            updatedAt = obj.optLong("updatedAt")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadCategories(): List<ScanableCategory> {
        val json = prefs.getString(KEY_CATEGORIES, null) ?: return emptyList()

        return try {
            val array = JSONArray(json)

            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)

                    add(
                        ScanableCategory(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            sortOrder = obj.optInt("sortOrder"),
                            isCollapsed = obj.optBoolean("isCollapsed"),
                            createdAt = obj.optLong("createdAt"),
                            updatedAt = obj.optLong("updatedAt")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadFavouritesCollapsed(): Boolean =
        prefs.getBoolean(KEY_FAV_COLLAPSED, false)

    fun loadUncategorizedCollapsed(): Boolean =
        prefs.getBoolean(KEY_UNCAT_COLLAPSED, false)

    fun shouldShowSwipeTutorial(): Boolean =
        !prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)

    fun markSwipeTutorialSeen() {
        prefs.edit()
            .putBoolean(KEY_SWIPE_TUTORIAL_SEEN, true)
            .putBoolean(KEY_ONBOARDING_COMPLETE, true)
            .apply()
    }

    fun resetSwipeTutorial() {
        prefs.edit()
            .putBoolean(KEY_SWIPE_TUTORIAL_SEEN, false)
            .putBoolean(KEY_ONBOARDING_COMPLETE, false)
            .apply()
    }

    fun saveAll(
        items: List<ScanableItem>,
        categories: List<ScanableCategory>,
        favouritesCollapsed: Boolean,
        uncategorizedCollapsed: Boolean
    ) {
        prefs.edit()
            .putString(KEY_ITEMS, items.scanableItemsToJson().toString())
            .putString(KEY_CATEGORIES, categories.scanableCategoriesToJson().toString())
            .putBoolean(KEY_FAV_COLLAPSED, favouritesCollapsed)
            .putBoolean(KEY_UNCAT_COLLAPSED, uncategorizedCollapsed)
            .apply()
    }
}

fun List<ScanableItem>.scanableItemsToJson(): JSONArray = JSONArray().also { array ->
    forEach { item ->
        array.put(
            JSONObject()
                .put("id", item.id)
                .put("name", item.name)
                .put("rawValue", item.rawValue)
                .put("format", item.format.name)
                .put("categoryId", item.categoryId ?: "")
                .put("isFavourite", item.isFavourite)
                .put("sortOrder", item.sortOrder)
                .put("createdAt", item.createdAt)
                .put("updatedAt", item.updatedAt)
        )
    }
}

fun List<ScanableCategory>.scanableCategoriesToJson(): JSONArray = JSONArray().also { array ->
    forEach { category ->
        array.put(
            JSONObject()
                .put("id", category.id)
                .put("name", category.name)
                .put("sortOrder", category.sortOrder)
                .put("isCollapsed", category.isCollapsed)
                .put("createdAt", category.createdAt)
                .put("updatedAt", category.updatedAt)
        )
    }
}

fun String.toScanableFormat(): ScanableFormat =
    ScanableFormat.entries.firstOrNull { it.name == this } ?: ScanableFormat.QR_CODE

fun sampleCategories(): List<ScanableCategory> = emptyList()

fun sampleItems(): List<ScanableItem> = emptyList()

fun nextSortOrderForCategory(items: List<ScanableItem>, categoryId: String?): Int =
    (items.filter { it.categoryId == categoryId }.maxOfOrNull { it.sortOrder } ?: -1) + 1

fun nextCategorySortOrder(categories: List<ScanableCategory>): Int =
    (categories.maxOfOrNull { it.sortOrder } ?: -1) + 1

fun moveItemWithinCategory(
    items: MutableList<ScanableItem>,
    item: ScanableItem,
    direction: Int
) {
    val group = items
        .filter { it.categoryId == item.categoryId }
        .sortedBy { it.sortOrder }
        .toMutableList()

    val index = group.indexOfFirst { it.id == item.id }
    val target = index + direction

    if (index !in group.indices || target !in group.indices) return

    val moved = group.removeAt(index)
    group.add(target, moved)

    group.forEachIndexed { order, updatedItem ->
        val globalIndex = items.indexOfFirst { it.id == updatedItem.id }

        if (globalIndex >= 0) {
            items[globalIndex] = updatedItem.copy(
                sortOrder = order,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
