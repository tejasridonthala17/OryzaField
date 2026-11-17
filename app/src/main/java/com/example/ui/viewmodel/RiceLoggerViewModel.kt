package com.example.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SampleRepository
import com.example.data.local.RiceSampleEntity
import com.example.data.location.FieldLocationProvider
import com.example.data.location.LocationResult
import com.example.data.model.FastaAnalysisResult
import com.example.data.model.SiteWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface AsyncStatus {
    data object Idle : AsyncStatus
    data object Loading : AsyncStatus
    data class Success(val message: String? = null) : AsyncStatus
    data class Error(val message: String) : AsyncStatus
}

data class NewSampleDraftState(
    val sampleId: String = generateDefaultSampleId(),
    val riceVariety: String = SampleRepository.RICE_VARIETIES.first(),
    val customVariety: String = "",
    val collectionDate: String = currentTimestampString(),
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val locationStatus: AsyncStatus = AsyncStatus.Idle,
    val weather: SiteWeather? = null,
    val weatherStatus: AsyncStatus = AsyncStatus.Idle,
    val fastaAnalysis: FastaAnalysisResult? = null,
    val fastaStatus: AsyncStatus = AsyncStatus.Idle,
    val saveMessage: String? = null,
    val saveError: String? = null,
    val isSaving: Boolean = false
) {
    val resolvedVariety: String
        get() = if (riceVariety == "Other" && customVariety.isNotBlank()) {
            customVariety.trim()
        } else {
            riceVariety
        }
}

class RiceLoggerViewModel(
    private val repository: SampleRepository,
    private val locationProvider: FieldLocationProvider,
    private val researcherUid: String
) : ViewModel() {

    private val _draft = MutableStateFlow(NewSampleDraftState())
    val draft: StateFlow<NewSampleDraftState> = _draft.asStateFlow()

    private val _selectedVarietyFilter = MutableStateFlow(SampleRepository.VARIETY_ALL)
    val selectedVarietyFilter: StateFlow<String> = _selectedVarietyFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val savedSamples: StateFlow<List<RiceSampleEntity>> = kotlinx.coroutines.flow.combine(
        _selectedVarietyFilter.flatMapLatest { variety ->
            repository.observeSamples(researcherUid, variety)
        },
        _searchQuery
    ) { list, query ->
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) list
        else list.filter {
            it.sampleId.lowercase(Locale.ROOT).contains(q) ||
                it.riceVariety.lowercase(Locale.ROOT).contains(q) ||
                (it.sequenceId?.lowercase(Locale.ROOT)?.contains(q) == true)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private val _allVarietiesForFilter = repository.observeSamples(researcherUid, SampleRepository.VARIETY_ALL)
        .map { samples ->
            val used = samples.map { it.riceVariety }.distinct().sorted()
            listOf(SampleRepository.VARIETY_ALL) + (SampleRepository.RICE_VARIETIES + used).distinct()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = listOf(SampleRepository.VARIETY_ALL) + SampleRepository.RICE_VARIETIES
        )
    val filterVarietyOptions: StateFlow<List<String>> = _allVarietiesForFilter

    fun updateSampleId(value: String) {
        _draft.update { it.copy(sampleId = value, saveError = null, saveMessage = null) }
    }

    fun generateNewSampleId() {
        _draft.update {
            it.copy(
                sampleId = generateDefaultSampleId(),
                collectionDate = currentTimestampString(),
                saveError = null,
                saveMessage = null
            )
        }
    }

    fun updateVariety(variety: String) {
        _draft.update { it.copy(riceVariety = variety, saveError = null, saveMessage = null) }
    }

    fun updateCustomVariety(value: String) {
        _draft.update { it.copy(customVariety = value, saveError = null) }
    }

    fun updateCollectionDate(value: String) {
        _draft.update { it.copy(collectionDate = value) }
    }

    fun refreshDateToNow() {
        _draft.update { it.copy(collectionDate = currentTimestampString()) }
    }

    fun updateNotes(value: String) {
        _draft.update { it.copy(notes = value) }
    }

    fun hasLocationPermission(): Boolean = locationProvider.hasLocationPermission()

    fun captureCurrentGpsLocation(autoFetchWeather: Boolean = true) {
        viewModelScope.launch {
            _draft.update { it.copy(locationStatus = AsyncStatus.Loading) }
            when (val result = locationProvider.getCurrentCoordinates()) {
                is LocationResult.Success -> {
                    _draft.update {
                        it.copy(
                            latitude = result.latitude,
                            longitude = result.longitude,
                            accuracyMeters = result.accuracyMeters,
                            locationStatus = AsyncStatus.Success("GPS locked")
                        )
                    }
                    if (autoFetchWeather) {
                        fetchWeatherForCoordinates(result.latitude, result.longitude)
                    }
                }
                LocationResult.PermissionDenied -> {
                    _draft.update {
                        it.copy(
                            locationStatus = AsyncStatus.Error(
                                "Location permission is required to capture field GPS coordinates."
                            )
                        )
                    }
                }
                is LocationResult.Unavailable -> {
                    _draft.update {
                        it.copy(locationStatus = AsyncStatus.Error(result.reason))
                    }
                }
            }
        }
    }

    fun setManualCoordinates(latitude: Double, longitude: Double, fetchWeather: Boolean = true) {
        _draft.update {
            it.copy(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = null,
                locationStatus = AsyncStatus.Success("Manual coordinates applied")
            )
        }
        if (fetchWeather) {
            fetchWeatherForCoordinates(latitude, longitude)
        }
    }

    fun fetchWeatherForCurrentLocation() {
        val lat = _draft.value.latitude
        val lon = _draft.value.longitude
        if (lat == null || lon == null) {
            _draft.update {
                it.copy(
                    weatherStatus = AsyncStatus.Error("Capture GPS coordinates before fetching Open-Meteo weather.")
                )
            }
            return
        }
        fetchWeatherForCoordinates(lat, lon)
    }

    private fun fetchWeatherForCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            _draft.update { it.copy(weatherStatus = AsyncStatus.Loading) }
            val result = repository.fetchSiteWeather(lat, lon)
            result.fold(
                onSuccess = { weather ->
                    _draft.update {
                        it.copy(
                            weather = weather,
                            weatherStatus = AsyncStatus.Success("Updated ${weather.observedAt}")
                        )
                    }
                },
                onFailure = { err ->
                    _draft.update {
                        it.copy(
                            weatherStatus = AsyncStatus.Error(
                                err.localizedMessage ?: "Failed to fetch weather from Open-Meteo."
                            )
                        )
                    }
                }
            )
        }
    }

    fun analyzeFastaFromUri(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _draft.update { it.copy(fastaStatus = AsyncStatus.Loading) }
            val readResult = withContext(Dispatchers.IO) {
                runCatching {
                    val displayName = resolveDisplayName(contentResolver, uri) ?: "promoter.fasta"
                    val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                        it.readText()
                    } ?: error("Unable to open selected FASTA file.")
                    displayName to text
                }
            }

            readResult.fold(
                onSuccess = { (fileName, text) ->
                    val analysis = repository.analyzeFasta(text, fileName)
                    analysis.fold(
                        onSuccess = { result ->
                            _draft.update {
                                it.copy(
                                    fastaAnalysis = result,
                                    fastaStatus = AsyncStatus.Success(
                                        "Analyzed ${result.sequenceLength} bp (${result.totalCisHits} cis hits, ${result.totalSsrMarkers} SSRs)"
                                    )
                                )
                            }
                        },
                        onFailure = { err ->
                            _draft.update {
                                it.copy(
                                    fastaStatus = AsyncStatus.Error(
                                        err.localizedMessage ?: "Failed to parse FASTA DNA sequence."
                                    )
                                )
                            }
                        }
                    )
                },
                onFailure = { err ->
                    _draft.update {
                        it.copy(
                            fastaStatus = AsyncStatus.Error(
                                err.localizedMessage ?: "Failed to read selected file."
                            )
                        )
                    }
                }
            )
        }
    }

    fun saveDraftSample(onSaved: (Long) -> Unit = {}) {
        val current = _draft.value
        if (current.sampleId.isBlank()) {
            _draft.update { it.copy(saveError = "Please enter a Sample ID before saving.") }
            return
        }
        if (current.resolvedVariety.isBlank()) {
            _draft.update { it.copy(saveError = "Please select or enter a rice variety.") }
            return
        }

        viewModelScope.launch {
            _draft.update { it.copy(isSaving = true, saveError = null, saveMessage = null) }
            try {
                val id = repository.saveNewSample(
                    researcherUid = researcherUid,
                    sampleId = current.sampleId,
                    riceVariety = current.resolvedVariety,
                    collectionDate = current.collectionDate,
                    latitude = current.latitude,
                    longitude = current.longitude,
                    weather = current.weather,
                    analysis = current.fastaAnalysis,
                    notes = current.notes
                )
                _draft.update {
                    it.copy(
                        isSaving = false,
                        saveMessage = "Saved '${current.sampleId}' to Room database."
                    )
                }
                onSaved(id)
            } catch (e: Exception) {
                _draft.update {
                    it.copy(
                        isSaving = false,
                        saveError = e.localizedMessage ?: "Failed to save sample."
                    )
                }
            }
        }
    }

    fun resetDraftForNextSample() {
        _draft.value = NewSampleDraftState()
    }

    fun setVarietyFilter(variety: String) {
        _selectedVarietyFilter.value = variety
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        // Trigger re-filter by toggling or let's make search reactive properly
    }

    fun observeSampleById(id: Long) = repository.observeSampleById(id)

    fun entityToAnalysis(entity: RiceSampleEntity): FastaAnalysisResult? =
        repository.entityToAnalysisResult(entity)

    fun updateSavedSample(
        entity: RiceSampleEntity,
        newSampleId: String,
        newVariety: String,
        newDate: String,
        newNotes: String,
        newLatitude: Double?,
        newLongitude: Double?,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateSample(
                entity.copy(
                    sampleId = newSampleId.trim(),
                    riceVariety = newVariety.trim(),
                    collectionDate = newDate.trim(),
                    notes = newNotes.trim(),
                    latitude = newLatitude,
                    longitude = newLongitude
                )
            )
            onComplete()
        }
    }

    fun deleteSample(id: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteSampleById(id)
            onDeleted()
        }
    }

    fun clearSaveBanner() {
        _draft.update { it.copy(saveMessage = null, saveError = null) }
    }

    private fun resolveDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        return uri.lastPathSegment
    }
}

private fun currentTimestampString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    return sdf.format(Date())
}

private fun generateDefaultSampleId(): String {
    val sdf = SimpleDateFormat("MMdd-HHmm", Locale.US)
    val suffix = (10..99).random()
    return "ORY-${sdf.format(Date())}-$suffix"
}
