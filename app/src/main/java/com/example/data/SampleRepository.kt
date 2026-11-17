package com.example.data

import com.example.data.bio.RicePromoterAnalyzer
import com.example.data.local.RiceSampleEntity
import com.example.data.local.SampleDao
import com.example.data.model.CisElementHit
import com.example.data.model.CisElementSummary
import com.example.data.model.FastaAnalysisResult
import com.example.data.model.SiteWeather
import com.example.data.model.SsrMarkerRecord
import com.example.data.network.OpenMeteoApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

class SampleRepository(
    private val sampleDao: SampleDao,
    private val weatherApi: OpenMeteoApi
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val cisSummaryAdapter = moshi.adapter<List<CisElementSummary>>(
        Types.newParameterizedType(List::class.java, CisElementSummary::class.java)
    )
    private val cisHitAdapter = moshi.adapter<List<CisElementHit>>(
        Types.newParameterizedType(List::class.java, CisElementHit::class.java)
    )
    private val ssrAdapter = moshi.adapter<List<SsrMarkerRecord>>(
        Types.newParameterizedType(List::class.java, SsrMarkerRecord::class.java)
    )

    fun observeSamples(uid: String, varietyFilter: String?): Flow<List<RiceSampleEntity>> {
        return if (varietyFilter.isNullOrBlank() || varietyFilter == VARIETY_ALL) {
            sampleDao.observeAllSamples(uid)
        } else {
            sampleDao.observeSamplesByVariety(uid, varietyFilter)
        }
    }

    fun observeSampleById(id: Long): Flow<RiceSampleEntity?> = sampleDao.observeSampleById(id)

    suspend fun getSampleById(id: Long): RiceSampleEntity? = sampleDao.getSampleById(id)

    suspend fun fetchSiteWeather(latitude: Double, longitude: Double): Result<SiteWeather> {
        return try {
            val response = weatherApi.getCurrentWeather(latitude = latitude, longitude = longitude)
            val current = response.current
                ?: return Result.failure(IllegalStateException("Open-Meteo returned no current weather block."))
            val weather = SiteWeather(
                temperatureC = current.temperature2m ?: Double.NaN,
                humidityPct = current.relativeHumidity2m ?: Double.NaN,
                precipitationMm = current.precipitation ?: 0.0,
                soilMoistureM3 = current.soilMoisture0To1Cm ?: Double.NaN,
                observedAt = current.time ?: "",
                timezone = response.timezoneAbbreviation ?: response.timezone ?: "UTC"
            )
            Result.success(weather)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeFasta(fastaText: String, fileName: String): Result<FastaAnalysisResult> {
        return try {
            Result.success(RicePromoterAnalyzer.analyzeFasta(fastaText, fileName))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveNewSample(
        researcherUid: String,
        sampleId: String,
        riceVariety: String,
        collectionDate: String,
        latitude: Double?,
        longitude: Double?,
        weather: SiteWeather?,
        analysis: FastaAnalysisResult?,
        notes: String
    ): Long {
        val now = System.currentTimeMillis()
        val entity = RiceSampleEntity(
            researcherUid = researcherUid,
            sampleId = sampleId.trim(),
            riceVariety = riceVariety.trim(),
            collectionDate = collectionDate.trim(),
            latitude = latitude,
            longitude = longitude,
            temperatureC = weather?.temperatureC?.takeIf { !it.isNaN() },
            humidityPct = weather?.humidityPct?.takeIf { !it.isNaN() },
            precipitationMm = weather?.precipitationMm?.takeIf { !it.isNaN() },
            soilMoistureM3 = weather?.soilMoistureM3?.takeIf { !it.isNaN() },
            weatherTimestamp = weather?.observedAt,
            weatherTimezone = weather?.timezone,
            fastaFileName = analysis?.fileName,
            sequenceId = analysis?.sequenceId,
            sequenceDescription = analysis?.description,
            sequencePreview = analysis?.sequencePreview,
            sequenceLength = analysis?.sequenceLength ?: 0,
            gcContentPct = analysis?.gcContentPct ?: 0.0,
            countA = analysis?.countA ?: 0,
            countT = analysis?.countT ?: 0,
            countG = analysis?.countG ?: 0,
            countC = analysis?.countC ?: 0,
            cisSummariesJson = cisSummaryAdapter.toJson(analysis?.cisSummaries.orEmpty()),
            cisHitsJson = cisHitAdapter.toJson(analysis?.cisHits.orEmpty()),
            ssrMarkersJson = ssrAdapter.toJson(analysis?.ssrMarkers.orEmpty()),
            totalCisHits = analysis?.totalCisHits ?: 0,
            uniqueCisTypes = analysis?.uniqueCisTypes ?: 0,
            totalSsrMarkers = analysis?.totalSsrMarkers ?: 0,
            mostCommonSsrMotif = analysis?.mostCommonSsrMotif ?: "none",
            avgSsrRepeats = analysis?.avgSsrRepeats ?: 0.0,
            notes = notes.trim(),
            createdAtMillis = now,
            updatedAtMillis = now
        )
        return sampleDao.insertSample(entity)
    }

    suspend fun updateSample(entity: RiceSampleEntity) {
        sampleDao.updateSample(entity.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun deleteSampleById(id: Long) {
        sampleDao.deleteSampleById(id)
    }

    fun decodeCisSummaries(json: String): List<CisElementSummary> {
        if (json.isBlank()) return emptyList()
        return runCatching { cisSummaryAdapter.fromJson(json).orEmpty() }.getOrDefault(emptyList())
    }

    fun decodeCisHits(json: String): List<CisElementHit> {
        if (json.isBlank()) return emptyList()
        return runCatching { cisHitAdapter.fromJson(json).orEmpty() }.getOrDefault(emptyList())
    }

    fun decodeSsrMarkers(json: String): List<SsrMarkerRecord> {
        if (json.isBlank()) return emptyList()
        return runCatching { ssrAdapter.fromJson(json).orEmpty() }.getOrDefault(emptyList())
    }

    fun entityToAnalysisResult(entity: RiceSampleEntity): FastaAnalysisResult? {
        if (entity.sequenceLength <= 0 && entity.fastaFileName.isNullOrBlank()) return null
        return FastaAnalysisResult(
            fileName = entity.fastaFileName ?: "Stored FASTA",
            sequenceId = entity.sequenceId ?: entity.sampleId,
            description = entity.sequenceDescription ?: "",
            sequencePreview = entity.sequencePreview ?: "",
            sequenceLength = entity.sequenceLength,
            gcContentPct = entity.gcContentPct,
            countA = entity.countA,
            countT = entity.countT,
            countG = entity.countG,
            countC = entity.countC,
            cisSummaries = decodeCisSummaries(entity.cisSummariesJson),
            cisHits = decodeCisHits(entity.cisHitsJson),
            ssrMarkers = decodeSsrMarkers(entity.ssrMarkersJson),
            mostCommonSsrMotif = entity.mostCommonSsrMotif,
            avgSsrRepeats = entity.avgSsrRepeats
        )
    }

    companion object {
        const val VARIETY_ALL = "All Varieties"

        val RICE_VARIETIES = listOf(
            "IR64",
            "Nipponbare",
            "Basmati 370",
            "Swarna",
            "Pokkali",
            "N22",
            "Kasalath",
            "Azucena",
            "CO39",
            "Minghui 63",
            "Zhenshan 97",
            "Other"
        )
    }
}
