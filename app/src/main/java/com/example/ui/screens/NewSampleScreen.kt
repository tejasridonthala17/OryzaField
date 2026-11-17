package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SampleRepository
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.AsyncStatus
import com.example.ui.viewmodel.NewSampleDraftState
import com.example.ui.viewmodel.RiceLoggerViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSampleScreen(
    viewModel: RiceLoggerViewModel,
    draft: NewSampleDraftState,
    onOpenWeatherDetails: () -> Unit,
    onOpenSequenceAnalysis: () -> Unit,
    onOpenPromoterAnalysis: () -> Unit,
    onSampleSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var varietyMenuExpanded by remember { mutableStateOf(false) }
    var showManualLocationDialog by remember { mutableStateOf(false) }
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            permissionDeniedOnce = false
            viewModel.captureCurrentGpsLocation(autoFetchWeather = true)
        } else {
            permissionDeniedOnce = true
        }
    }

    val fastaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.analyzeFastaFromUri(context.contentResolver, uri)
        }
    }

    // Automatically capture GPS on initial entry if permission is already granted and no fix yet
    LaunchedEffect(Unit) {
        if (draft.latitude == null && draft.locationStatus is AsyncStatus.Idle) {
            if (viewModel.hasLocationPermission()) {
                viewModel.captureCurrentGpsLocation(autoFetchWeather = true)
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 1: Sample Metadata
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Grass,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Field Sample Identity",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    OutlinedTextField(
                        value = draft.sampleId,
                        onValueChange = viewModel::updateSampleId,
                        label = { Text("Sample ID") },
                        placeholder = { Text("e.g. ORY-IR64-001") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = viewModel::generateNewSampleId,
                                modifier = Modifier.testTag("generate_sample_id_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Autorenew,
                                    contentDescription = "Generate new Sample ID"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sample_id_input")
                    )

                    ExposedDropdownMenuBox(
                        expanded = varietyMenuExpanded,
                        onExpandedChange = { varietyMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = draft.riceVariety,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rice Variety (Oryza sativa)") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = varietyMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("rice_variety_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = varietyMenuExpanded,
                            onDismissRequest = { varietyMenuExpanded = false }
                        ) {
                            SampleRepository.RICE_VARIETIES.forEach { variety ->
                                DropdownMenuItem(
                                    text = { Text(variety) },
                                    onClick = {
                                        viewModel.updateVariety(variety)
                                        varietyMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = draft.riceVariety == "Other") {
                        OutlinedTextField(
                            value = draft.customVariety,
                            onValueChange = viewModel::updateCustomVariety,
                            label = { Text("Custom Cultivar / Landrace Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_variety_input")
                        )
                    }

                    OutlinedTextField(
                        value = draft.collectionDate,
                        onValueChange = viewModel::updateCollectionDate,
                        label = { Text("Collection Date & Time") },
                        leadingIcon = {
                            Icon(Icons.Filled.CalendarToday, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = viewModel::refreshDateToNow) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Set to current date and time"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("collection_date_input")
                    )

                    OutlinedTextField(
                        value = draft.notes,
                        onValueChange = viewModel::updateNotes,
                        label = { Text("Field Notes (Plot, Growth Stage, Stress)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sample_notes_input")
                    )
                }
            }

            // Section 2: GPS Location Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GpsFixed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Site GPS Coordinates",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (draft.locationStatus is AsyncStatus.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    if (draft.latitude != null && draft.longitude != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "LAT %.6f   LON %.6f",
                                            draft.latitude,
                                            draft.longitude
                                        ),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = MonoFontFamily,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    val accText = draft.accuracyMeters?.let {
                                        "Accuracy ±${it.toInt()} m (High-Accuracy Fused)"
                                    } ?: "Manual site coordinates"
                                    Text(
                                        text = accText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "GPS Captured",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else if (draft.locationStatus is AsyncStatus.Error || permissionDeniedOnce) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = (draft.locationStatus as? AsyncStatus.Error)?.message
                                        ?: stringResource(R.string.error_gps_unavailable),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Tap Capture GPS to record high-accuracy paddy coordinates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (viewModel.hasLocationPermission()) {
                                    viewModel.captureCurrentGpsLocation(autoFetchWeather = true)
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("capture_gps_button"),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_capture_gps))
                        }

                        OutlinedButton(
                            onClick = { showManualLocationDialog = true },
                            modifier = Modifier.testTag("manual_gps_button"),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EditLocationAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Enter Coords")
                        }
                    }
                }
            }

            // Section 3: Open-Meteo Weather Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudQueue,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Open-Meteo Site Weather",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (draft.weatherStatus is AsyncStatus.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    val w = draft.weather
                    if (w != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WeatherMiniMetric(
                                label = "Temp",
                                value = "${String.format(Locale.US, "%.1f", w.temperatureC)}°C",
                                modifier = Modifier.weight(1f)
                            )
                            WeatherMiniMetric(
                                label = "Humidity",
                                value = "${w.humidityPct.toInt()}%",
                                modifier = Modifier.weight(1f)
                            )
                            WeatherMiniMetric(
                                label = "Rain",
                                value = "${String.format(Locale.US, "%.1f", w.precipitationMm)} mm",
                                modifier = Modifier.weight(1f)
                            )
                            WeatherMiniMetric(
                                label = "Soil",
                                value = "${String.format(Locale.US, "%.2f", w.soilMoistureM3)}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else if (draft.weatherStatus is AsyncStatus.Error) {
                        Text(
                            text = draft.weatherStatus.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "Weather telemetry is fetched automatically once coordinates are captured.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = viewModel::fetchWeatherForCurrentLocation,
                            enabled = draft.weatherStatus !is AsyncStatus.Loading,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("fetch_weather_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_fetch_weather))
                        }

                        OutlinedButton(
                            onClick = onOpenWeatherDetails,
                            modifier = Modifier.testTag("open_weather_details_button")
                        ) {
                            Text("Weather Details")
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Section 4: Promoter FASTA Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Biotech,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Promoter FASTA & bioKT",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (draft.fastaStatus is AsyncStatus.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    val analysis = draft.fastaAnalysis
                    if (analysis != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${analysis.sequenceId} (${analysis.fileName})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "Length: %d bp  •  GC: %.1f%%  •  Cis: %d hits (%d motifs)  •  SSRs: %d",
                                        analysis.sequenceLength,
                                        analysis.gcContentPct,
                                        analysis.totalCisHits,
                                        analysis.uniqueCisTypes,
                                        analysis.totalSsrMarkers
                                    ),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = MonoFontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    } else if (draft.fastaStatus is AsyncStatus.Error) {
                        Text(
                            text = draft.fastaStatus.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "Attach a promoter FASTA file to scan rice cis-regulatory motifs & SSR microsatellites.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { fastaPickerLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_fasta_from_new_sample_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FileOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_pick_fasta))
                        }

                        OutlinedButton(
                            onClick = {
                                if (draft.fastaAnalysis != null) onOpenPromoterAnalysis()
                                else onOpenSequenceAnalysis()
                            },
                            modifier = Modifier.testTag("open_analysis_from_new_sample_button")
                        ) {
                            Text(if (draft.fastaAnalysis != null) "Promoter Report" else "Sequence Lab")
                        }
                    }
                }
            }

            // Save Feedback & Primary Save Action
            AnimatedVisibility(visible = draft.saveError != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = draft.saveError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            AnimatedVisibility(visible = draft.saveMessage != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = draft.saveMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onSampleSaved) {
                            Text("View Saved")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.saveDraftSample {
                        onSampleSaved()
                    }
                },
                enabled = !draft.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_sample_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (draft.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Save, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_save_sample),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (showManualLocationDialog) {
        ManualCoordinatesDialog(
            initialLat = draft.latitude,
            initialLon = draft.longitude,
            onDismiss = { showManualLocationDialog = false },
            onConfirm = { lat, lon ->
                showManualLocationDialog = false
                viewModel.setManualCoordinates(lat, lon, fetchWeather = true)
            }
        )
    }
}

@Composable
private fun WeatherMiniMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ManualCoordinatesDialog(
    initialLat: Double?,
    initialLon: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    var latText by remember { mutableStateOf(initialLat?.toString() ?: "14.1650") }
    var lonText by remember { mutableStateOf(initialLon?.toString() ?: "121.2414") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Site Coordinates") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "If GPS signal is unavailable indoors or on an emulator, enter site latitude & longitude (e.g. IRRI Los Baños: 14.1650, 121.2414).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it; error = null },
                    label = { Text("Latitude (-90 to 90)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lonText,
                    onValueChange = { lonText = it; error = null },
                    label = { Text("Longitude (-180 to 180)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.trim().toDoubleOrNull()
                    val lon = lonText.trim().toDoubleOrNull()
                    if (lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                        error = "Enter valid numeric coordinates."
                    } else {
                        onConfirm(lat, lon)
                    }
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
