package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rice_samples")
data class RiceSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val researcherUid: String,
    val sampleId: String,
    val riceVariety: String,
    val collectionDate: String,
    val latitude: Double?,
    val longitude: Double?,
    val temperatureC: Double?,
    val humidityPct: Double?,
    val precipitationMm: Double?,
    val soilMoistureM3: Double?,
    val weatherTimestamp: String?,
    val weatherTimezone: String?,
    val fastaFileName: String?,
    val sequenceId: String?,
    val sequenceDescription: String?,
    val sequencePreview: String?,
    val sequenceLength: Int,
    val gcContentPct: Double,
    val countA: Int,
    val countT: Int,
    val countG: Int,
    val countC: Int,
    val cisSummariesJson: String,
    val cisHitsJson: String,
    val ssrMarkersJson: String,
    val totalCisHits: Int,
    val uniqueCisTypes: Int,
    val totalSsrMarkers: Int,
    val mostCommonSsrMotif: String,
    val avgSsrRepeats: Double,
    val notes: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
