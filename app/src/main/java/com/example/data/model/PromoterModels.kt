package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CisElementSummary(
    val name: String,
    val count: Int,
    val family: String,
    val function: String
)

@JsonClass(generateAdapter = true)
data class CisElementHit(
    val name: String,
    val motif: String,
    val position: Int,
    val strand: String,
    val family: String,
    val function: String
)

@JsonClass(generateAdapter = true)
data class SsrMarkerRecord(
    val id: String,
    val motif: String,
    val motifLength: Int,
    val repeats: Int,
    val start: Int,
    val end: Int,
    val length: Int,
    val type: String,
    val sequence: String
)

data class FastaAnalysisResult(
    val fileName: String,
    val sequenceId: String,
    val description: String,
    val sequencePreview: String,
    val sequenceLength: Int,
    val gcContentPct: Double,
    val countA: Int,
    val countT: Int,
    val countG: Int,
    val countC: Int,
    val cisSummaries: List<CisElementSummary>,
    val cisHits: List<CisElementHit>,
    val ssrMarkers: List<SsrMarkerRecord>,
    val mostCommonSsrMotif: String,
    val avgSsrRepeats: Double
) {
    val totalCisHits: Int get() = cisHits.size
    val uniqueCisTypes: Int get() = cisSummaries.size
    val totalSsrMarkers: Int get() = ssrMarkers.size
}
