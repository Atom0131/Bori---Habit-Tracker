package com.apagon.rhythm.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import com.apagon.rhythm.platform.FilePicker
import com.apagon.rhythm.platform.ImageBitmapLoader
import com.apagon.rhythm.platform.PhotoStorage
import com.apagon.rhythm.ui.components.crystalCheckboxColors
import com.apagon.rhythm.ui.components.crystalChipSurface
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import com.apagon.rhythm.ui.components.crystalSelectedChipColor
import com.apagon.rhythm.ui.components.crystalSelectedChipContentColor
import com.apagon.rhythm.ui.components.crystalTileSurface
import com.apagon.rhythm.ui.components.crystalTopAppBarColors
import com.apagon.rhythm.ui.components.CrystalIconButton
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.apagon.rhythm.ui.util.RhythmDropdownMenu
import com.apagon.rhythm.ui.util.RhythmSheet
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert

/**
 * Desktop port of NoteEditorScreen.kt. Photo picking goes through Stage 9's
 * FilePicker + PhotoStorage.importPhoto directly (same shape as
 * DesktopEntrySheet's journal photo attach), not through a launcher/Uri.
 * Share/Export goes to the clipboard (LocalClipboardManager) instead of an
 * Intent.ACTION_SEND chooser. Icons.* throughout replaced with text/glyph
 * buttons — material-icons-extended stays androidMain-only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopNoteEditorScreen(
    noteId: Long,
    notebookId: Long,
    template: String? = null,
    viewModel: NoteEditorViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(noteId, notebookId) {
        val t = if (template != null) NoteTemplate.fromName(template) else null
        viewModel.loadNote(noteId, notebookId, t)
    }

    val title by viewModel.title.collectAsState()
    val blocks by viewModel.blocks.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val wordCount by viewModel.wordCount.collectAsState()
    val charCount by viewModel.charCount.collectAsState()
    val toolbarPinned by viewModel.toolbarPinned.collectAsState()
    val allNotebooks by viewModel.allNotebooks.collectAsState()
    val currentNote by viewModel.note.collectAsState()
    val fontFamilyKey by viewModel.fontFamily.collectAsState()
    val fontSizeKey by viewModel.fontSize.collectAsState()
    val clipboard = LocalClipboardManager.current

    val resolvedFont = resolveDesktopNoteFont(fontFamilyKey)
    val noteBodyStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = resolvedFont, fontSize = resolveDesktopNoteBodySize(fontSizeKey))
    val noteHeaderStyle = MaterialTheme.typography.headlineSmall.copy(fontFamily = resolvedFont, fontSize = resolveDesktopNoteHeaderSize(fontSizeKey), fontWeight = FontWeight.Bold)

    val listState = rememberLazyListState()
    val toolbarVisible = toolbarPinned || listState.firstVisibleItemIndex == 0

    var activeFocusedBlock by remember { mutableIntStateOf(-1) }
    var activeSelection by remember { mutableStateOf(TextRange(0)) }

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }
    var showStyleSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = crystalScaffoldColor(),
        contentColor = crystalScaffoldContentColor(),
        topBar = {
            TopAppBar(
                title = { Text("Edit Note", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = { viewModel.saveNote(onNavigateBack) }) { Text("← Back") } },
                actions = {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(viewModel.exportMarkdown())) }) { Text("Copy") }
                    Box {
                        CrystalIconButton(icon = Icons.Default.MoreVert, contentDescription = "More options", onClick = { showOverflowMenu = true })
                        RhythmDropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                            DropdownMenuItem(text = { Text("Move to notebook…") }, onClick = { showOverflowMenu = false; showMoveSheet = true })
                            DropdownMenuItem(
                                text = { Text("Copy as Markdown") },
                                onClick = { showOverflowMenu = false; clipboard.setText(AnnotatedString(viewModel.exportMarkdown())) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Save") }, onClick = { showOverflowMenu = false; viewModel.saveNote(onNavigateBack) })
                        }
                    }
                },
                colors = crystalTopAppBarColors()
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (toolbarVisible) {
                DesktopFormattingToolbar(
                    activeFocusedBlock = activeFocusedBlock,
                    activeSelection = activeSelection,
                    toolbarPinned = toolbarPinned,
                    onFormat = { open, close -> viewModel.applyFormattingToBlock(activeFocusedBlock, activeSelection, open, close) },
                    onChangeBlockType = { type -> viewModel.changeBlockType(activeFocusedBlock, type) },
                    onTogglePin = { viewModel.setToolbarPinned(!toolbarPinned) },
                    onShowStyleSheet = { showStyleSheet = true }
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    TextField(
                        value = title,
                        onValueChange = { viewModel.updateTitle(it) },
                        placeholder = { Text("Untitled", style = MaterialTheme.typography.headlineLarge) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    if (wordCount > 0 || charCount > 0) {
                        Text(
                            text = "$wordCount ${if (wordCount == 1) "word" else "words"} · $charCount ${if (charCount == 1) "char" else "chars"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    DesktopTagsRow(tags = tags, onAddTag = { viewModel.addTag(it) }, onRemoveTag = { viewModel.removeTag(it) })
                    Spacer(Modifier.height(8.dp))
                }

                itemsIndexed(blocks, key = { _, block -> block.id }) { index, block ->
                    val numberedIndex = if (block.type == BlockType.NUMBERED_LIST) {
                        blocks.take(index + 1).reversed().takeWhile { it.type == BlockType.NUMBERED_LIST }.count()
                    } else 0
                    DesktopBlockItem(
                        block = block,
                        index = index,
                        numberedIndex = numberedIndex,
                        noteBodyStyle = noteBodyStyle,
                        noteHeaderStyle = noteHeaderStyle,
                        onUpdate = { viewModel.updateBlock(index, it) },
                        onDelete = { viewModel.removeBlock(index) },
                        onAddAfter = { type -> viewModel.addBlock(index, type) },
                        onAddImage = { viewModel.addImageBlock(index, it) },
                        onFocused = { idx -> activeFocusedBlock = idx },
                        onSelectionChanged = { range -> activeSelection = range }
                    )
                }

                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }

    if (showStyleSheet) {
        DesktopNoteStyleSheet(
            currentFontKey = fontFamilyKey,
            currentSizeKey = fontSizeKey,
            onFontSelected = { viewModel.setFontFamily(it) },
            onSizeSelected = { viewModel.setFontSize(it) },
            onDismiss = { showStyleSheet = false }
        )
    }

    if (showMoveSheet) {
        val currentNotebookId = currentNote?.notebookId ?: notebookId
        val targetNotebooks = allNotebooks.filter { it.id != currentNotebookId }
        RhythmSheet(onDismiss = { showMoveSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Move to Notebook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                if (targetNotebooks.isEmpty()) {
                    Text("No other notebooks available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    targetNotebooks.forEach { nb ->
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .crystalTileSurface()
                                .clickable { showMoveSheet = false; viewModel.moveNote(nb.id) }
                        ) {
                            Text(text = nb.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopFormattingToolbar(
    activeFocusedBlock: Int,
    activeSelection: TextRange,
    toolbarPinned: Boolean,
    onFormat: (String, String) -> Unit,
    onChangeBlockType: (BlockType) -> Unit,
    onTogglePin: () -> Unit,
    onShowStyleSheet: () -> Unit
) {
    var showBlockTypeMenu by remember { mutableStateOf(false) }
    val hasActiveBlock = activeFocusedBlock >= 0

    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 3.dp, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DesktopFormatButton("B", "Bold", hasActiveBlock) { onFormat("**", "**") }
            DesktopFormatButton("I", "Italic", hasActiveBlock) { onFormat("_", "_") }
            DesktopFormatButton("U", "Underline", hasActiveBlock) { onFormat("<u>", "</u>") }
            DesktopFormatButton("S", "Strikethrough", hasActiveBlock) { onFormat("~~", "~~") }
            DesktopFormatButton("H", "Highlight", hasActiveBlock) { onFormat("==", "==") }
            DesktopFormatButton("<>", "Inline code", hasActiveBlock) { onFormat("`", "`") }

            VerticalDivider(modifier = Modifier.height(22.dp).padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Box {
                TextButton(onClick = { if (hasActiveBlock) showBlockTypeMenu = true }, enabled = hasActiveBlock) { Text("¶") }
                RhythmDropdownMenu(expanded = showBlockTypeMenu, onDismissRequest = { showBlockTypeMenu = false }) {
                    listOf(
                        BlockType.TEXT to "Text", BlockType.HEADER to "Header", BlockType.QUOTE to "Quote",
                        BlockType.BULLET_LIST to "Bullet List", BlockType.NUMBERED_LIST to "Numbered List",
                        BlockType.CHECKLIST to "Checklist", BlockType.CODE to "Code"
                    ).forEach { (type, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { onChangeBlockType(type); showBlockTypeMenu = false })
                    }
                }
            }

            VerticalDivider(modifier = Modifier.height(22.dp).padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

            TextButton(onClick = onShowStyleSheet) { Text("Aa", fontWeight = FontWeight.Bold) }

            Spacer(Modifier.weight(1f))

            TextButton(onClick = onTogglePin) {
                Text(if (toolbarPinned) "📌" else "📍", color = if (toolbarPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DesktopFormatButton(label: String, tooltip: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(label, fontWeight = FontWeight.Bold, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
    }
}

private val transparentColors
    @Composable get() = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
    )

@Composable
private fun DesktopBlockItem(
    block: NoteBlock,
    index: Int,
    numberedIndex: Int = 0,
    noteBodyStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    noteHeaderStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    onUpdate: (NoteBlock) -> Unit,
    onDelete: () -> Unit,
    onAddAfter: (BlockType) -> Unit,
    onAddImage: (path: String) -> Unit,
    onFocused: (Int) -> Unit,
    onSelectionChanged: (TextRange) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val filePicker = koinInject<FilePicker>()
    val photoStorage = koinInject<PhotoStorage>()
    val imageLoader = koinInject<ImageBitmapLoader>()
    val coroutineScope = rememberCoroutineScope()

    val pickAndAddImage: () -> Unit = {
        val path = filePicker.pickImagePath()
        if (path != null) {
            coroutineScope.launch {
                val imported = photoStorage.importPhoto(path, "notes/images")
                if (imported != null) onAddImage(imported)
            }
        }
    }

    when (block.type) {
        BlockType.DIVIDER -> {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f).padding(vertical = 8.dp))
                DesktopBlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                    DesktopBlockDropdownMenu(showMenu, { showMenu = false }, onAddAfter, pickAndAddImage, onDelete)
                }
            }
        }
        BlockType.IMAGE -> {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                if (block.content.isNotBlank()) {
                    imageLoader.LoadedImage(
                        path = block.content,
                        modifier = Modifier.weight(1f).heightIn(max = 240.dp).clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(modifier = Modifier.weight(1f).height(120.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = MaterialTheme.shapes.medium) {}
                }
                DesktopBlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                    DesktopBlockDropdownMenu(showMenu, { showMenu = false }, onAddAfter, pickAndAddImage, onDelete)
                }
            }
        }
        BlockType.QUOTE -> {
            Surface(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f), shape = MaterialTheme.shapes.small) {
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(color = MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(8.dp))
                    DesktopFormattableTextField(
                        block = block, index = index, modifier = Modifier.weight(1f), placeholder = "Quote",
                        textStyle = noteBodyStyle.copy(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        onUpdate = onUpdate, onFocused = onFocused, onSelectionChanged = onSelectionChanged
                    )
                    DesktopBlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                        DesktopBlockDropdownMenu(showMenu, { showMenu = false }, onAddAfter, pickAndAddImage, onDelete)
                    }
                }
            }
        }
        BlockType.CODE -> {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)).padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("code", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextField(
                            value = block.content,
                            onValueChange = { onUpdate(block.copy(content = it)) },
                            placeholder = { Text("// write your code here", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)) },
                            modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onFocused(index) },
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            colors = transparentColors,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false)
                        )
                    }
                }
                DesktopBlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                    DesktopBlockDropdownMenu(showMenu, { showMenu = false }, onAddAfter, pickAndAddImage, onDelete)
                }
            }
        }
        BlockType.BULLET_LIST -> DesktopSimpleListRow("•", block, index, noteBodyStyle, "List item", onUpdate, onFocused, onSelectionChanged, showMenu, { showMenu = !showMenu }, onAddAfter, pickAndAddImage, onDelete)
        BlockType.NUMBERED_LIST -> DesktopSimpleListRow("$numberedIndex.", block, index, noteBodyStyle, "List item", onUpdate, onFocused, onSelectionChanged, showMenu, { showMenu = !showMenu }, onAddAfter, pickAndAddImage, onDelete)
        BlockType.CHECKLIST -> {
            var tfv by remember(block.id) { mutableStateOf(TextFieldValue(block.content)) }
            LaunchedEffect(block.content) { if (tfv.text != block.content) tfv = tfv.copy(text = block.content) }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = block.isChecked, onCheckedChange = { onUpdate(block.copy(isChecked = it)) }, colors = crystalCheckboxColors())
                TextField(
                    value = tfv,
                    onValueChange = { v -> tfv = v; onUpdate(block.copy(content = v.text)); onSelectionChanged(v.selection) },
                    placeholder = { Text("To-do") },
                    modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFocused(index) },
                    textStyle = noteBodyStyle, colors = transparentColors,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                DesktopBlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                    DesktopBlockDropdownMenu(showMenu, { showMenu = false }, onAddAfter, pickAndAddImage, onDelete)
                }
            }
        }
        BlockType.TEXT -> {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DesktopFormattableTextField(
                    block = block, index = index, modifier = Modifier.weight(1f), placeholder = "Type something...",
                    textStyle = noteBodyStyle, onUpdate = onUpdate, onFocused = onFocused, onSelectionChanged = onSelectionChanged
                )
                DesktopBlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                    DesktopBlockDropdownMenu(showMenu, { showMenu = false }, onAddAfter, pickAndAddImage, onDelete)
                }
            }
        }
        BlockType.HEADER -> {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(3.dp).height(26.dp).background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.extraSmall))
                Spacer(Modifier.width(10.dp))
                DesktopFormattableTextField(
                    block = block, index = index, modifier = Modifier.weight(1f), placeholder = "Heading",
                    textStyle = noteHeaderStyle, onUpdate = onUpdate, onFocused = onFocused, onSelectionChanged = onSelectionChanged
                )
                DesktopBlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                    DesktopBlockDropdownMenu(showMenu, { showMenu = false }, onAddAfter, pickAndAddImage, onDelete)
                }
            }
        }
    }
}

@Composable
private fun DesktopSimpleListRow(
    marker: String,
    block: NoteBlock,
    index: Int,
    noteBodyStyle: TextStyle,
    placeholder: String,
    onUpdate: (NoteBlock) -> Unit,
    onFocused: (Int) -> Unit,
    onSelectionChanged: (TextRange) -> Unit,
    showMenu: Boolean,
    onToggleMenu: () -> Unit,
    onAddAfter: (BlockType) -> Unit,
    onAddImage: () -> Unit,
    onDelete: () -> Unit
) {
    var tfv by remember(block.id) { mutableStateOf(TextFieldValue(block.content)) }
    LaunchedEffect(block.content) { if (tfv.text != block.content) tfv = tfv.copy(text = block.content) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(marker, style = noteBodyStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, end = 8.dp))
        TextField(
            value = tfv,
            onValueChange = { v -> tfv = v; onUpdate(block.copy(content = v.text)); onSelectionChanged(v.selection) },
            placeholder = { Text(placeholder) },
            modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFocused(index) },
            textStyle = noteBodyStyle, colors = transparentColors,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        DesktopBlockMenuButton(showMenu = showMenu, onToggle = onToggleMenu) {
            DesktopBlockDropdownMenu(showMenu, { onToggleMenu() }, onAddAfter, onAddImage, onDelete)
        }
    }
}

@Composable
private fun DesktopFormattableTextField(
    block: NoteBlock,
    index: Int,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    onUpdate: (NoteBlock) -> Unit,
    onFocused: (Int) -> Unit,
    onSelectionChanged: (TextRange) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var requestFocusOnMount by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember(block.id) { mutableStateOf(TextFieldValue(block.content)) }
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlightColor = MaterialTheme.colorScheme.tertiaryContainer

    LaunchedEffect(block.content) {
        if (textFieldValue.text != block.content) textFieldValue = textFieldValue.copy(text = block.content)
    }
    LaunchedEffect(requestFocusOnMount) {
        if (requestFocusOnMount) { focusRequester.requestFocus(); requestFocusOnMount = false }
    }

    Box(modifier = modifier) {
        if (isFocused) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { tfv -> textFieldValue = tfv; onUpdate(block.copy(content = tfv.text)); onSelectionChanged(tfv.selection) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).focusRequester(focusRequester)
                    .onFocusChanged { fs -> isFocused = fs.isFocused; if (fs.isFocused) onFocused(index) },
                textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        } else {
            val rendered = remember(block.content, codeBackground, highlightColor) {
                if (block.content.isEmpty()) AnnotatedString("") else parseInlineMarkdown(block.content, codeBackground, highlightColor)
            }
            if (rendered.text.isEmpty()) {
                Text(
                    text = placeholder, style = textStyle, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { isFocused = true; requestFocusOnMount = true }
                )
            } else {
                Text(
                    text = rendered, style = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { isFocused = true; requestFocusOnMount = true }
                )
            }
        }
    }
}

@Composable
private fun DesktopTagsRow(tags: List<String>, onAddTag: (String) -> Unit, onRemoveTag: (String) -> Unit) {
    var inputText by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        tags.forEach { tag ->
            InputChip(
                selected = false, onClick = {}, label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                trailingIcon = { TextButton(onClick = { onRemoveTag(tag) }) { Text("×") } }
            )
        }

        BasicTextField(
            value = inputText,
            onValueChange = { raw ->
                val trimmed = raw.trimEnd()
                if (trimmed.endsWith(",") || trimmed.endsWith(" ")) {
                    val tag = trimmed.dropLast(1).trim()
                    if (tag.isNotEmpty()) onAddTag(tag)
                    inputText = ""
                } else {
                    inputText = raw
                }
            },
            modifier = Modifier.widthIn(min = 80.dp).onFocusChanged { isFocused = it.isFocused },
            textStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
            decorationBox = { inner ->
                if (inputText.isEmpty() && !isFocused) {
                    Text("Add tag…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                inner()
            }
        )
    }
}

@Composable
private fun DesktopBlockMenuButton(showMenu: Boolean, onToggle: () -> Unit, dropdownContent: @Composable () -> Unit) {
    Box {
        TextButton(onClick = onToggle) { Text("+") }
        dropdownContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopNoteStyleSheet(
    currentFontKey: String,
    currentSizeKey: String,
    onFontSelected: (String) -> Unit,
    onSizeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    RhythmSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Text Style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Text("SIZE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("small" to "S · Small", "normal" to "M · Normal", "large" to "L · Large").forEach { (key, label) ->
                    val selected = currentSizeKey == key
                    Box(
                        modifier = Modifier
                            .crystalChipSurface(
                                fill = if (selected) crystalSelectedChipColor(MaterialTheme.colorScheme.primary)
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            .clickable(onClick = { onSizeSelected(key) })
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                label,
                                color = if (selected) crystalSelectedChipContentColor(MaterialTheme.colorScheme.onPrimary) else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopBlockDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAddAfter: (BlockType) -> Unit,
    onAddImage: () -> Unit,
    onDelete: () -> Unit
) {
    RhythmDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Text") }, onClick = { onAddAfter(BlockType.TEXT); onDismiss() })
        DropdownMenuItem(text = { Text("Header") }, onClick = { onAddAfter(BlockType.HEADER); onDismiss() })
        DropdownMenuItem(text = { Text("Checklist") }, onClick = { onAddAfter(BlockType.CHECKLIST); onDismiss() })
        DropdownMenuItem(text = { Text("Bullet List") }, onClick = { onAddAfter(BlockType.BULLET_LIST); onDismiss() })
        DropdownMenuItem(text = { Text("Numbered List") }, onClick = { onAddAfter(BlockType.NUMBERED_LIST); onDismiss() })
        DropdownMenuItem(text = { Text("Quote") }, onClick = { onAddAfter(BlockType.QUOTE); onDismiss() })
        DropdownMenuItem(text = { Text("Code") }, onClick = { onAddAfter(BlockType.CODE); onDismiss() })
        DropdownMenuItem(text = { Text("Divider") }, onClick = { onAddAfter(BlockType.DIVIDER); onDismiss() })
        DropdownMenuItem(text = { Text("Photo") }, onClick = { onAddImage(); onDismiss() })
        HorizontalDivider()
        DropdownMenuItem(text = { Text("Delete Block", color = MaterialTheme.colorScheme.error) }, onClick = { onDelete(); onDismiss() })
    }
}
