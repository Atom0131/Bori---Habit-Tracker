package com.apagon.rhythm.data.model

data class ChecklistProgress(
    val items: List<ChecklistItem>,
    val checkedItemIds: Set<Long>
)
