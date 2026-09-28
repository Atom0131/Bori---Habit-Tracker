package com.apagon.rhythm.ui.util

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.ui.components.CrystalWindowContent
import com.apagon.rhythm.ui.components.crystalControlElevation
import com.apagon.rhythm.ui.components.crystalMenuContainerColor
import com.apagon.rhythm.ui.components.crystalMenuField

/**
 * Desktop adaptation of the Android original's `RhythmDropdownMenu` (`ui/util/RhythmSheet.kt`) —
 * drop-in replacement for stock M3 `DropdownMenu` matching its own `expanded`/`onDismissRequest`/
 * content shape exactly. [crystalMenuField] goes on the outer `modifier`, not inside [content] —
 * see that function's own doc for why the content lambda can never reach the field it needs to.
 */
@Composable
fun RhythmDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.crystalMenuField(),
        shape = MaterialTheme.shapes.medium,
        containerColor = crystalMenuContainerColor(MaterialTheme.colorScheme.surfaceContainer),
        shadowElevation = crystalControlElevation(3.dp),
        tonalElevation = crystalControlElevation(3.dp)
    ) {
        CrystalWindowContent(paintField = false) { Column(content = content) }
    }
}
