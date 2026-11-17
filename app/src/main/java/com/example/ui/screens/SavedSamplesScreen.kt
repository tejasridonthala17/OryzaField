package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SampleRepository
import com.example.data.local.RiceSampleEntity
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.RiceLoggerViewModel
import java.util.Locale

@Composable
fun SavedSamplesScreen(
    viewModel: RiceLoggerViewModel,
    samples: List<RiceSampleEntity>,
    varietyOptions: List<String>,
    selectedVariety: String,
    searchQuery: String,
    onOpenSamplePromoterReport: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingSample by remember { mutableStateOf<RiceSampleEntity?>(null) }
    var deletingSample by remember { mutableStateOf<RiceSampleEntity?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
        ) {
            // Search & Filter Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Search by Sample ID, cultivar, or FASTA ID") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("saved_samples_search_input")
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "Filter by rice variety",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    items(varietyOptions, key = { it }) { variety ->
                        FilterChip(
                            selected = selectedVariety == variety,
                            onClick = { viewModel.setVarietyFilter(variety) },
                            label = { Text(variety) },
                            modifier = Modifier.testTag("variety_filter_$variety")
                        )
                    }
                }
            }

            if (samples.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (selectedVariety == SampleRepository.VARIETY_ALL && searchQuery.isBlank()) {
                                "No Field Samples Logged Yet"
                            } else {
                                "No Samples Match Filter"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Record a rice field sample with GPS, Open-Meteo weather, and bioKT FASTA promoter analysis to see it archived here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("saved_samples_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(samples, key = { it.id }) { sample ->
                        SavedSampleCard(
                            sample = sample,
                            onInspectPromoter = { onOpenSamplePromoterReport(sample.id) },
                            onEdit = { editingSample = sample },
                            onDelete = { deletingSample = sample }
                        )
                    }
                }
            }
        }
    }

    editingSample?.let { target ->
        EditSampleDialog(
            sample = target,
            onDismiss = { editingSample = null },
            onSave = { newId, newVariety, newDate, newNotes, newLat, newLon ->
                viewModel.updateSavedSample(
                    entity = target,
                    newSampleId = newId,
                    newVariety = newVariety,
                    newDate = newDate,
                    newNotes = newNotes,
                    newLatitude = newLat,
                    newLongitude = newLon,
                    onComplete = { editingSample = null }
                )
            }
        )
    }

    deletingSample?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingSample = null },
            title = { Text("Delete Field Sample?") },
            text = {
                Text(
                    "Remove sample '${target.sampleId}' (${target.riceVariety}) permanently from the Room database?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSample(target.id) {
                            deletingSample = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSample = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SavedSampleCard(
    sample: RiceSampleEntity,
    onInspectPromoter: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sample_card_${sample.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = sample.sampleId,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = sample.riceVariety,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = sample.collectionDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_sample_button_${sample.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit sample ${sample.sampleId}"
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_sample_button_${sample.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete sample ${sample.sampleId}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // GPS + Weather strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (sample.latitude != null && sample.longitude != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%.4f, %.4f",
                                    sample.latitude,
                                    sample.longitude
                                ),
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily)
                            )
                        }
                    }
                }

                if (sample.temperatureC != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Thermostat,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%.1f°C • %d%% RH • %.2f m³/m³",
                                    sample.temperatureC,
                                    sample.humidityPct?.toInt() ?: 0,
                                    sample.soilMoistureM3 ?: 0.0
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily)
                            )
                        }
                    }
                }
            }

            // FASTA & Promoter metrics
            if (sample.sequenceLength > 0) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sample.sequenceId ?: sample.fastaFileName ?: "Promoter Sequence",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%d bp • %.1f%% GC • %d Cis hits • %d SSRs",
                                    sample.sequenceLength,
                                    sample.gcContentPct,
                                    sample.totalCisHits,
                                    sample.totalSsrMarkers
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        OutlinedButton(
                            onClick = onInspectPromoter,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Biotech,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Report")
                        }
                    }
                }
            }

            if (sample.notes.isNotBlank()) {
                Text(
                    text = sample.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSampleDialog(
    sample: RiceSampleEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Double?, Double?) -> Unit
) {
    var sampleId by remember { mutableStateOf(sample.sampleId) }
    var variety by remember { mutableStateOf(sample.riceVariety) }
    var date by remember { mutableStateOf(sample.collectionDate) }
    var notes by remember { mutableStateOf(sample.notes) }
    var latText by remember { mutableStateOf(sample.latitude?.toString() ?: "") }
    var lonText by remember { mutableStateOf(sample.longitude?.toString() ?: "") }
    var varietyExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Sample Record") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = sampleId,
                    onValueChange = { sampleId = it },
                    label = { Text("Sample ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = varietyExpanded,
                    onExpandedChange = { varietyExpanded = it }
                ) {
                    OutlinedTextField(
                        value = variety,
                        onValueChange = { variety = it },
                        label = { Text("Rice Variety") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = varietyExpanded)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = varietyExpanded,
                        onDismissRequest = { varietyExpanded = false }
                    ) {
                        SampleRepository.RICE_VARIETIES.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    variety = option
                                    varietyExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Collection Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Field Notes") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sampleId.isNotBlank() && variety.isNotBlank()) {
                        onSave(
                            sampleId,
                            variety,
                            date,
                            notes,
                            latText.trim().toDoubleOrNull(),
                            lonText.trim().toDoubleOrNull()
                        )
                    }
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
