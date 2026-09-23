package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

// Stage 3's minimal desktop Habit screen — a plain list/add/complete UI
// against Compose Multiplatform's common artifacts (not androidx.compose),
// so this same file is a candidate to share with Android later rather than
// a throwaway. Deliberately not a port of the Android HabitListScreen.kt
// (which genuinely depends on real androidx.compose.* + BackHandler and
// isn't directly shareable) — only its list-grouping/tap-to-complete shape
// is borrowed.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopHabitScreen(viewModel: DesktopHabitViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var newHabitName by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Rhythm — ${state.date}") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newHabitName,
                    onValueChange = { newHabitName = it },
                    label = { Text("New habit") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    viewModel.addHabit(newHabitName)
                    newHabitName = ""
                }) {
                    Text("Add")
                }
            }

            if (state.habits.isEmpty()) {
                Text(
                    "No habits scheduled for today yet — add one above.",
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
                    items(state.habits, key = { it.id }) { habit ->
                        val isDone = habit.id in state.completedHabitIds
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isDone,
                                onCheckedChange = { viewModel.toggleCompletion(habit.id, isDone) }
                            )
                            Text(habit.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
