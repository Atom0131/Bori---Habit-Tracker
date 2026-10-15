package com.apagon.rhythm.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.ui.components.crystalCardSurface

/*
 * The desktop Settings kit (2026-10-15), after Windows' settings pattern: a page title, groups of
 * rows under a small header, each row an optional icon, a title, an optional description and a
 * control on the right. A row with an action is clickable across its whole width, with the hand
 * cursor and hover highlight, so a "card" is never a dead picture of a button (the Windows report:
 * the Sync card looked clickable and wasn't). One glass card per group, not per row.
 */

/** The page's title at the top of the content pane. */
@Composable
internal fun SettingsPageHeader(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * A group of rows in one glass card, with an optional [header] above it and [footer] text below.
 * Put [SettingsItem]s in it, separated by [SettingsDivider].
 */
@Composable
internal fun SettingsGroup(
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        header?.let {
            Text(
                it,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth()
                .crystalCardSurface(fill = MaterialTheme.colorScheme.surfaceContainerLow),
            content = content
        )
        footer?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 * One row: [icon], [title], [description] and a [trailing] control. With [onClick] the whole row
 * is the button (hand cursor, hover highlight, Enter when focused); without it, only the trailing
 * control acts.
 */
@Composable
internal fun SettingsItem(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    titleColor: Color = Color.Unspecified,
    descriptionColor: Color = Color.Unspecified,
    trailing: (@Composable () -> Unit)? = null
) {
    val clickable = onClick != null && enabled
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .then(
                if (clickable) Modifier
                    .clickable(role = Role.Button, onClick = onClick!!)
                    .pointerHoverIcon(PointerIcon.Hand)
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (titleColor != Color.Unspecified) titleColor
                        else if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (descriptionColor != Color.Unspecified) descriptionColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.let {
            Spacer(Modifier.width(12.dp))
            it()
        }
    }
}

/** The thin line between rows inside a [SettingsGroup], inset to line up with the text. */
@Composable
internal fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/** The trailing "opens a page" mark, optionally after a short value ("3 archived"). */
@Composable
internal fun SettingsChevron(value: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        value?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
