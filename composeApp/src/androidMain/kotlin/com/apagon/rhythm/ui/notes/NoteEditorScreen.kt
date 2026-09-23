package com.apagon.rhythm.ui.notes

import org.koin.compose.viewmodel.koinViewModel

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
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
    val context = LocalContext.current

    val resolvedFont = resolveNoteFont(fontFamilyKey)
    val noteBodyStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = resolvedFont,
        fontSize = resolveNoteBodySize(fontSizeKey)
    )
    val noteHeaderStyle = MaterialTheme.typography.headlineSmall.copy(
        fontFamily = resolvedFont,
        fontSize = resolveNoteHeaderSize(fontSizeKey),
        fontWeight = FontWeight.Bold
    )

    val listState = rememberLazyListState()
    val toolbarVisible by remember { derivedStateOf { toolbarPinned || listState.firstVisibleItemIndex == 0 } }

    var activeFocusedBlock by remember { mutableIntStateOf(-1) }
    var activeSelection by remember { mutableStateOf(TextRange(0)) }

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }
    var showStyleSheet by remember { mutableStateOf(false) }

    var pendingImageAfterIndex by remember { mutableStateOf(-1) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && pendingImageAfterIndex >= 0) {
            viewModel.addImageBlock(pendingImageAfterIndex, uri.toString())
        }
        pendingImageAfterIndex = -1
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Note", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.saveNote(onNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val md = viewModel.exportMarkdown()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "${title.ifBlank { "note" }}.md")
                            putExtra(Intent.EXTRA_TEXT, md)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share note"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Move to notebook…") },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp)) },
                                onClick = { showOverflowMenu = false; showMoveSheet = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as Markdown") },
                                leadingIcon = { Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showOverflowMenu = false
                                    val md = viewModel.exportMarkdown()
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "${title.ifBlank { "note" }}.md")
                                        putExtra(Intent.EXTRA_TEXT, md)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Export Markdown"))
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Save") },
                                leadingIcon = { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) },
                                onClick = { showOverflowMenu = false; viewModel.saveNote(onNavigateBack) }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(
                visible = toolbarVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                FormattingToolbar(
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
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
                    TagsRow(
                        tags = tags,
                        onAddTag = { viewModel.addTag(it) },
                        onRemoveTag = { viewModel.removeTag(it) }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                itemsIndexed(blocks, key = { _, block -> block.id }) { index, block ->
                    val numberedIndex = if (block.type == BlockType.NUMBERED_LIST) {
                        blocks.take(index + 1)
                            .reversed()
                            .takeWhile { it.type == BlockType.NUMBERED_LIST }
                            .count()
                    } else 0
                    BlockItem(
                        block = block,
                        index = index,
                        numberedIndex = numberedIndex,
                        noteBodyStyle = noteBodyStyle,
                        noteHeaderStyle = noteHeaderStyle,
                        onUpdate = { viewModel.updateBlock(index, it) },
                        onDelete = { viewModel.removeBlock(index) },
                        onAddAfter = { type -> viewModel.addBlock(index, type) },
                        onAddImage = {
                            pendingImageAfterIndex = index
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onFocused = { idx -> activeFocusedBlock = idx },
                        onSelectionChanged = { range -> activeSelection = range }
                    )
                }

                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }

    if (showStyleSheet) {
        NoteStyleSheet(
            currentFontKey = fontFamilyKey,
            currentSizeKey = fontSizeKey,
            onFontSelected = { viewModel.setFontFamily(it) },
            onSizeSelected = { viewModel.setFontSize(it) },
            onDismiss = { showStyleSheet = false }
        )
    }

    // Move sheet
    if (showMoveSheet) {
        val currentNotebookId = currentNote?.notebookId ?: notebookId
        val targetNotebooks = allNotebooks.filter { it.id != currentNotebookId }
        ModalBottomSheet(onDismissRequest = { showMoveSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Move to Notebook",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (targetNotebooks.isEmpty()) {
                    Text(
                        "No other notebooks available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    targetNotebooks.forEach { nb ->
                        Surface(
                            onClick = {
                                showMoveSheet = false
                                viewModel.moveNote(nb.id)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = nb.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormattingToolbar(
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            FormatIconButton(Icons.Default.FormatBold, "Bold", hasActiveBlock) { onFormat("**", "**") }
            FormatIconButton(Icons.Default.FormatItalic, "Italic", hasActiveBlock) { onFormat("_", "_") }
            FormatIconButton(Icons.Default.FormatUnderlined, "Underline", hasActiveBlock) { onFormat("<u>", "</u>") }
            FormatIconButton(Icons.Default.StrikethroughS, "Strikethrough", hasActiveBlock) { onFormat("~~", "~~") }
            FormatIconButton(Icons.Default.Highlight, "Highlight", hasActiveBlock) { onFormat("==", "==") }
            FormatIconButton(Icons.Default.Code, "Inline code", hasActiveBlock) { onFormat("`", "`") }

            VerticalDivider(
                modifier = Modifier.height(22.dp).padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Box {
                Surface(
                    onClick = { if (hasActiveBlock) showBlockTypeMenu = true },
                    shape = MaterialTheme.shapes.small,
                    color = if (showBlockTypeMenu) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.FormatSize,
                        contentDescription = "Block type",
                        modifier = Modifier.padding(10.dp).size(20.dp),
                        tint = if (hasActiveBlock) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                DropdownMenu(expanded = showBlockTypeMenu, onDismissRequest = { showBlockTypeMenu = false }) {
                    listOf(
                        BlockType.TEXT to "Text",
                        BlockType.HEADER to "Header",
                        BlockType.QUOTE to "Quote",
                        BlockType.BULLET_LIST to "Bullet List",
                        BlockType.NUMBERED_LIST to "Numbered List",
                        BlockType.CHECKLIST to "Checklist",
                        BlockType.CODE to "Code"
                    ).forEach { (type, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { onChangeBlockType(type); showBlockTypeMenu = false }
                        )
                    }
                }
            }

            VerticalDivider(
                modifier = Modifier.height(22.dp).padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Surface(
                onClick = onShowStyleSheet,
                shape = MaterialTheme.shapes.small,
                color = Color.Transparent,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Aa",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Surface(
                onClick = onTogglePin,
                shape = MaterialTheme.shapes.small,
                color = if (toolbarPinned) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = if (toolbarPinned) "Unpin toolbar" else "Pin toolbar",
                    modifier = Modifier.padding(10.dp).size(20.dp),
                    tint = if (toolbarPinned) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FormatIconButton(
    icon: ImageVector,
    tooltip: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            modifier = Modifier.padding(10.dp).size(20.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

private val transparentColors
    @Composable get() = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

@Composable
private fun BlockItem(
    block: NoteBlock,
    index: Int,
    numberedIndex: Int = 0,
    noteBodyStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    noteHeaderStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    onUpdate: (NoteBlock) -> Unit,
    onDelete: () -> Unit,
    onAddAfter: (BlockType) -> Unit,
    onAddImage: () -> Unit,
    onFocused: (Int) -> Unit,
    onSelectionChanged: (TextRange) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    when (block.type) {
        BlockType.DIVIDER -> {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f).padding(vertical = 8.dp))
                BlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                    BlockDropdownMenu(expanded = showMenu, onDismiss = { showMenu = false }, onAddAfter = onAddAfter, onAddImage = onAddImage, onDelete = onDelete)
                }
            }
        }
        BlockType.IMAGE -> {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                if (block.content.isNotBlank()) {
                    AsyncImage(
                        model = block.content,
                        contentDescription = null,
                        modifier = Modifier.weight(1f).heightIn(max = 240.dp).clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.weight(1f).height(120.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.medium
                    ) {}
                }
                BlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                    BlockDropdownMenu(expanded = showMenu, onDismiss = { showMenu = false }, onAddAfter = onAddAfter, onAddImage = onAddImage, onDelete = onDelete)
                }
            }
        }
        BlockType.QUOTE -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(color = MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(8.dp))
                    FormattableTextField(
                        block = block, index = index, modifier = Modifier.weight(1f),
                        placeholder = "Quote",
                        textStyle = noteBodyStyle.copy(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        onUpdate = onUpdate, onFocused = onFocused, onSelectionChanged = onSelectionChanged
                    )
                    BlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                        BlockDropdownMenu(expanded = showMenu, onDismiss = { showMenu = false }, onAddAfter = onAddAfter, onAddImage = onAddImage, onDelete = onDelete)
                    }
                }
            }
        }
        BlockType.CODE -> {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Code, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("code", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextField(
                            value = block.content,
                            onValueChange = { onUpdate(block.copy(content = it)) },
                            placeholder = { Text("// write your code here", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)) },
                            modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onFocused(index) },
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrect = false)
                        )
                    }
                }
                BlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                    BlockDropdownMenu(expanded = showMenu, onDismiss = { showMenu = false }, onAddAfter = onAddAfter, onAddImage = onAddImage, onDelete = onDelete)
                }
            }
        }
        BlockType.BULLET_LIST -> {
            var tfv by remember(block.id) { mutableStateOf(TextFieldValue(block.content)) }
            LaunchedEffect(block.content) { if (tfv.text != block.content) tfv = tfv.copy(text = block.content) }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, end = 8.dp))
                TextField(
                    value = tfv,
                    onValueChange = { v -> tfv = v; onUpdate(block.copy(content = v.text)); onSelectionChanged(v.selection) },
                    placeholder = { Text("List item") },
                    modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFocused(index) },
                    textStyle = noteBodyStyle,
                    colors = transparentColors,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                BlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                    BlockDropdownMenu(expanded = showMenu, onDismiss = { showMenu = false }, onAddAfter = onAddAfter, onAddImage = onAddImage, onDelete = onDelete)
                }
            }
        }
        BlockType.NUMBERED_LIST -> {
            var tfv by remember(block.id) { mutableStateOf(TextFieldValue(block.content)) }
            LaunchedEffect(block.content) { if (tfv.text != block.content) tfv = tfv.copy(text = block.content) }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("$numberedIndex.", style = noteBodyStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, end = 8.dp))
                TextField(
                    value = tfv,
                    onValueChange = { v -> tfv = v; onUpdate(block.copy(content = v.text)); onSelectionChanged(v.selection) },
                    placeholder = { Text("List item") },
                    modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFocused(index) },
                    textStyle = noteBodyStyle,
                    colors = transparentColors,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                BlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                    BlockDropdownMenu(expanded = showMenu, onDismiss = { showMenu = false }, onAddAfter = onAddAfter, onAddImage = onAddImage, onDelete = onDelete)
                }
            }
        }
        BlockType.CHECKLIST -> {
            var tfv by remember(block.id) { mutableStateOf(TextFieldValue(block.content)) }
            LaunchedEffect(block.content) { if (tfv.text != block.content) tfv = tfv.copy(text = block.content) }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = block.isChecked, onCheckedChange = { onUpdate(block.copy(isChecked = it)) })
                TextField(
                    value = tfv,
                    onValueChange = { v -> tfv = v; onUpdate(block.copy(content = v.text)); onSelectionChanged(v.selection) },
                    placeholder = { Text("To-do") },
                    modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFocused(index) },
                    textStyle = noteBodyStyle,
                    colors = transparentColors,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                BlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                    BlockDropdownMenu(expanded = showMenu, onDismiss = { showMenu = false }, onAddAfter = onAddAfter, onAddImage = onAddImage, onDelete = onDelete)
                }
            }
        }
        BlockType.TEXT -> {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FormattableTextField(
                    block = block, index = index, modifier = Modifier.weight(1f),
                    placeholder = "Type something...",
                    textStyle = noteBodyStyle,
                    onUpdate = onUpdate, onFocused = onFocused, onSelectionChanged = onSelectionChanged
                )
                BlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                    BlockDropdownMenu(expanded = showMenu, onDismiss = { showMenu = false }, onAddAfter = onAddAfter, onAddImage = onAddImage, onDelete = onDelete)
                }
            }
        }
        BlockType.HEADER -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(26.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.extraSmall)
                )
                Spacer(Modifier.width(10.dp))
                FormattableTextField(
                    block = block, index = index, modifier = Modifier.weight(1f),
                    placeholder = "Heading",
                    textStyle = noteHeaderStyle,
                    onUpdate = onUpdate, onFocused = onFocused, onSelectionChanged = onSelectionChanged
                )
                BlockMenuButton(showMenu = showMenu, onToggle = { showMenu = !showMenu }) {
                    BlockDropdownMenu(expanded = showMenu, onDismiss = { showMenu = false }, onAddAfter = onAddAfter, onAddImage = onAddImage, onDelete = onDelete)
                }
            }
        }
    }
}

@Composable
private fun FormattableTextField(
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
        if (textFieldValue.text != block.content) {
            textFieldValue = textFieldValue.copy(text = block.content)
        }
    }
    LaunchedEffect(requestFocusOnMount) {
        if (requestFocusOnMount) {
            focusRequester.requestFocus()
            requestFocusOnMount = false
        }
    }

    Box(modifier = modifier) {
        if (isFocused) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { tfv ->
                    textFieldValue = tfv
                    onUpdate(block.copy(content = tfv.text))
                    onSelectionChanged(tfv.selection)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { fs ->
                        isFocused = fs.isFocused
                        if (fs.isFocused) onFocused(index)
                    },
                textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        } else {
            val rendered: AnnotatedString = remember(block.content, codeBackground, highlightColor) {
                if (block.content.isEmpty()) AnnotatedString("")
                else parseInlineMarkdown(block.content, codeBackground, highlightColor)
            }
            if (rendered.text.isEmpty()) {
                Text(
                    text = placeholder,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { isFocused = true; requestFocusOnMount = true }
                )
            } else {
                Text(
                    text = rendered,
                    style = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { isFocused = true; requestFocusOnMount = true }
                )
            }
        }
    }
}

@Composable
private fun TagsRow(
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tags.forEach { tag ->
            InputChip(
                selected = false,
                onClick = {},
                label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                trailingIcon = {
                    IconButton(onClick = { onRemoveTag(tag) }, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove tag", modifier = Modifier.size(14.dp))
                    }
                }
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
            modifier = Modifier
                .widthIn(min = 80.dp)
                .onFocusChanged { isFocused = it.isFocused },
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
private fun BlockMenuButton(showMenu: Boolean, onToggle: () -> Unit, dropdownContent: @Composable () -> Unit) {
    Box {
        IconButton(onClick = onToggle) {
            Icon(Icons.Default.Add, contentDescription = "Add block", modifier = Modifier.size(20.dp))
        }
        dropdownContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteStyleSheet(
    currentFontKey: String,
    currentSizeKey: String,
    onFontSelected: (String) -> Unit,
    onSizeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Text Style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Text("FONT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "default" to "Default",
                    "classic" to "Classic",
                    "round"   to "Round",
                    "mono"    to "Mono"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = currentFontKey == key,
                        onClick = { onFontSelected(key) },
                        label = { Text(label, fontFamily = resolveNoteFont(key)) }
                    )
                }
            }

            Text("SIZE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "small"  to "S · Small",
                    "normal" to "M · Normal",
                    "large"  to "L · Large"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = currentSizeKey == key,
                        onClick = { onSizeSelected(key) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAddAfter: (BlockType) -> Unit,
    onAddImage: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Text") }, onClick = { onAddAfter(BlockType.TEXT); onDismiss() })
        DropdownMenuItem(text = { Text("Header") }, onClick = { onAddAfter(BlockType.HEADER); onDismiss() })
        DropdownMenuItem(text = { Text("Checklist") }, onClick = { onAddAfter(BlockType.CHECKLIST); onDismiss() })
        DropdownMenuItem(text = { Text("Bullet List") }, leadingIcon = { Icon(Icons.Default.FormatListBulleted, null, modifier = Modifier.size(18.dp)) }, onClick = { onAddAfter(BlockType.BULLET_LIST); onDismiss() })
        DropdownMenuItem(text = { Text("Numbered List") }, leadingIcon = { Icon(Icons.Default.FormatListNumbered, null, modifier = Modifier.size(18.dp)) }, onClick = { onAddAfter(BlockType.NUMBERED_LIST); onDismiss() })
        DropdownMenuItem(text = { Text("Quote") }, leadingIcon = { Icon(Icons.Default.FormatQuote, null, modifier = Modifier.size(18.dp)) }, onClick = { onAddAfter(BlockType.QUOTE); onDismiss() })
        DropdownMenuItem(text = { Text("Code") }, leadingIcon = { Icon(Icons.Default.Code, null, modifier = Modifier.size(18.dp)) }, onClick = { onAddAfter(BlockType.CODE); onDismiss() })
        DropdownMenuItem(text = { Text("Divider") }, leadingIcon = { Icon(Icons.Default.HorizontalRule, null, modifier = Modifier.size(18.dp)) }, onClick = { onAddAfter(BlockType.DIVIDER); onDismiss() })
        DropdownMenuItem(text = { Text("Photo") }, leadingIcon = { Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp)) }, onClick = { onAddImage(); onDismiss() })
        HorizontalDivider()
        DropdownMenuItem(text = { Text("Delete Block", color = MaterialTheme.colorScheme.error) }, onClick = { onDelete(); onDismiss() })
    }
}
