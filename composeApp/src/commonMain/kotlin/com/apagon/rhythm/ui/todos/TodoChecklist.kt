package com.apagon.rhythm.ui.todos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.TodoSubtask
import com.apagon.rhythm.ui.components.crystalControlColor

/*
 * To-do checklists, ported from Android (TodoRowComposable's SubtaskGroup, SharedComposables'
 * SubtaskRow / EditableSubtaskRow, AddTodoSheet's draft handling). One nesting level: a top-level
 * step can hold items; an item can't hold more.
 */

/** Marks a new item under a group that has no id yet; the DAO's replaceSubtasks re-parents by
 * position on save, so only "is a child" matters. Android's TodoRepository.PENDING_PARENT. */
const val PENDING_PARENT = -1L

/** A top-level step with the items under it. Built from the flat document-ordered list. */
data class SubtaskGroupData(val parent: TodoSubtask, val children: List<TodoSubtask>) {
    /** What the progress badge counts: a group's items, or the group itself when it has none. */
    val leafCount: Int get() = if (children.isEmpty()) 1 else children.size
}

/** Android's `toGroups`: a child joins the group it names, else the latest group; a child with no
 * group before it is promoted rather than dropped. */
fun List<TodoSubtask>.toGroups(): List<SubtaskGroupData> {
    val groups = mutableListOf<SubtaskGroupData>()
    val childrenOf = mutableMapOf<Long, MutableList<TodoSubtask>>()
    forEach { row ->
        if (row.parentId == null) {
            groups += SubtaskGroupData(row, childrenOf.getOrPut(row.id) { mutableListOf() })
        } else {
            val bucket = childrenOf[row.parentId] ?: groups.lastOrNull()?.let { childrenOf[it.parent.id] }
            if (bucket != null) bucket += row else groups += SubtaskGroupData(row.copy(parentId = null), mutableListOf())
        }
    }
    return groups
}

/** The steps that count toward "3/5": a group's items, or the group itself when it has none. */
fun List<TodoSubtask>.leaves(): List<TodoSubtask> {
    val parents = mapNotNull { it.parentId }.toSet()
    return filter { it.id !in parents }
}

/** Android's `SubtaskRow`: one tickable step in a to-do's unfolded checklist. */
@Composable
fun SubtaskRow(
    label: String,
    isDone: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium
) {
    Surface(
        shape = shape,
        color = if (isDone) accentColor.copy(alpha = 0.12f) else crystalControlColor(MaterialTheme.colorScheme.surfaceContainerHighest),
        border = if (isDone) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            if (isDone) {
                Box(Modifier.size(width = 4.dp, height = 24.dp).clip(CircleShape).background(accentColor))
                Spacer(Modifier.width(12.dp))
            }
            Icon(
                if (isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = if (isDone) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isDone) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Android's `SubtaskGroup`: the group's row, then its items indented beneath it. */
@Composable
private fun SubtaskGroup(group: SubtaskGroupData, forceDone: Boolean, onToggleSubtask: (TodoSubtask) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SubtaskRow(group.parent.label, forceDone || group.parent.isDone, MaterialTheme.colorScheme.primary, onClick = { onToggleSubtask(group.parent) })
        group.children.forEach { child ->
            SubtaskRow(
                child.label, forceDone || child.isDone, MaterialTheme.colorScheme.primary,
                onClick = { onToggleSubtask(child) },
                modifier = Modifier.padding(start = 24.dp)
            )
        }
    }
}

/** The "▾ 3/5" line under a to-do's title. */
@Composable
fun SubtaskProgress(subtasks: List<TodoSubtask>, expanded: Boolean) {
    val leaves = subtasks.leaves()
    val doneCount = leaves.count { it.isDone }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp).rotate(if (expanded) 180f else 0f)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            "$doneCount/${leaves.size}",
            style = MaterialTheme.typography.labelSmall,
            color = if (doneCount == leaves.size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The unfolded checklist under a to-do row: open groups, then finished ones behind "N done". */
@Composable
fun TodoChecklistBody(subtasks: List<TodoSubtask>, todoDone: Boolean, onToggleSubtask: (TodoSubtask) -> Unit) {
    var doneExpanded by remember { mutableStateOf(false) }
    val groups = remember(subtasks) { subtasks.toGroups() }
    val (done, open) = groups.partition { todoDone || it.parent.isDone }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 48.dp, end = 12.dp, bottom = 10.dp)) {
        open.forEach { SubtaskGroup(it, forceDone = todoDone, onToggleSubtask = onToggleSubtask) }
        if (done.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { doneExpanded = !doneExpanded }.padding(vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp).rotate(if (doneExpanded) 180f else 0f)
                )
                Spacer(Modifier.width(4.dp))
                Text("${done.sumOf { it.leafCount }} done", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (doneExpanded) done.forEach { SubtaskGroup(it, forceDone = todoDone, onToggleSubtask = onToggleSubtask) }
        }
    }
}

/** Android's `EditableSubtaskRow`: a step you can type in. Enter adds the next step at the same
 * level, Backspace on an empty step removes it, and the indent buttons (shown while focused) move
 * it under the group above or back out. */
@Composable
private fun EditableSubtaskRow(
    label: String,
    isDone: Boolean,
    onLabelChange: (String) -> Unit,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    onBackspaceWhenEmpty: () -> Unit,
    focusRequester: FocusRequester,
    isChild: Boolean,
    canIndent: Boolean,
    canOutdent: Boolean,
    onIndent: () -> Unit,
    onOutdent: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    isFocused: Boolean
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isDone) accent.copy(alpha = 0.12f) else crystalControlColor(MaterialTheme.colorScheme.surfaceContainerHighest),
        border = if (isDone) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.padding(start = if (isChild) 24.dp else 0.dp).fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = onToggleDone, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = if (isDone) "Tick off" else "Not ticked off",
                    tint = if (isDone) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = label,
                onValueChange = onLabelChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isDone) FontWeight.SemiBold else FontWeight.Normal
                ),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { onEnter() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChanged(it.isFocused) }
                    .onKeyEvent { e ->
                        when {
                            e.type != KeyEventType.KeyDown -> false
                            // A desktop keyboard's Enter doesn't fire the IME action on a single-line field.
                            e.key == Key.Enter || e.key == Key.NumPadEnter -> { onEnter(); true }
                            e.key == Key.Backspace && label.isEmpty() -> { onBackspaceWhenEmpty(); true }
                            else -> false
                        }
                    }
            )
            if (isFocused && canOutdent) {
                IconButton(onClick = onOutdent, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.FormatIndentDecrease, contentDescription = "Move out of its group", tint = accent, modifier = Modifier.size(18.dp))
                }
            }
            if (isFocused && canIndent) {
                IconButton(onClick = onIndent, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.FormatIndentIncrease, contentDescription = "Move under the item above", tint = accent, modifier = Modifier.size(18.dp))
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove ${label.ifBlank { "empty item" }}", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun List<TodoSubtask>.groupEndExclusive(index: Int): Int {
    var end = index + 1
    while (end < size && this[end].parentId != null) end++
    return end
}

private fun List<TodoSubtask>.hasChildren(index: Int): Boolean = index + 1 < size && this[index + 1].parentId != null

/**
 * Android's "CHECKLIST (OPTIONAL)" editor block. [drafts] is the flat document-ordered list the
 * caller keeps and passes back on save.
 */
@Composable
fun TodoChecklistEditor(drafts: List<TodoSubtask>, onDraftsChange: (List<TodoSubtask>) -> Unit, todoId: Long) {
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }
    var pendingFocusIndex by remember { mutableStateOf<Int?>(null) }
    var focusedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(pendingFocusIndex, drafts.size) {
        val index = pendingFocusIndex ?: return@LaunchedEffect
        if (index in drafts.indices) runCatching { focusRequesters[index]?.requestFocus() }
        pendingFocusIndex = null
    }

    fun addAt(index: Int, asChild: Boolean = false) {
        onDraftsChange(drafts.toMutableList().also {
            it.add(index, TodoSubtask(todoId = todoId, label = "", parentId = if (asChild) PENDING_PARENT else null))
        })
        pendingFocusIndex = index
    }

    /** Removes a row, and if it is a group, the items under it. */
    fun deleteAt(index: Int) {
        val draft = drafts[index]
        onDraftsChange(
            if (draft.parentId != null) drafts.toMutableList().also { it.removeAt(index) }
            else { val end = drafts.groupEndExclusive(index); drafts.filterIndexed { i, _ -> i < index || i >= end } }
        )
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("CHECKLIST (OPTIONAL)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        drafts.forEachIndexed { index, draft ->
            val isChild = draft.parentId != null
            fun replace(row: TodoSubtask) = onDraftsChange(drafts.toMutableList().also { it[index] = row })
            EditableSubtaskRow(
                label = draft.label,
                isDone = draft.isDone,
                onLabelChange = { replace(draft.copy(label = it)) },
                onToggleDone = { replace(draft.copy(isDone = !draft.isDone)) },
                onDelete = { deleteAt(index) },
                onEnter = { addAt(index + 1, asChild = isChild) },
                onBackspaceWhenEmpty = { deleteAt(index); pendingFocusIndex = (index - 1).takeIf { it >= 0 } },
                focusRequester = focusRequesters.getOrPut(index) { FocusRequester() },
                isChild = isChild,
                canIndent = !isChild && index > 0 && !drafts.hasChildren(index),
                canOutdent = isChild,
                onIndent = { replace(draft.copy(parentId = PENDING_PARENT)) },
                onOutdent = { replace(draft.copy(parentId = null)) },
                onFocusChanged = { focused -> if (focused) focusedIndex = index else if (focusedIndex == index) focusedIndex = null },
                isFocused = focusedIndex == index
            )
        }
        Surface(
            onClick = { addAt(drafts.size) },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Add item", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** Drops blank steps before saving, as Android does. Items under a group left blank are promoted
 * to top level rather than dropped or re-filed under the group before it. */
fun List<TodoSubtask>.cleanedForSave(): List<TodoSubtask> {
    var groupSurvived = false
    return buildList {
        this@cleanedForSave.forEach { row ->
            val label = row.label.trim()
            if (row.parentId == null) {
                groupSurvived = label.isNotBlank()
                if (groupSurvived) add(row.copy(label = label))
            } else if (label.isNotBlank()) {
                add(if (groupSurvived) row.copy(label = label) else row.copy(label = label, parentId = null))
            }
        }
    }
}
