package com.example.data.bio

import biokt.DNASequence
import biokt.SeqIO
import biokt.rice.CisElementScanner
import biokt.rice.SSRDetector
import biokt.rice.detectSSRs
import biokt.rice.scanRicePromoter
import com.example.data.model.CisElementHit
import com.example.data.model.CisElementSummary
import com.example.data.model.FastaAnalysisResult
import com.example.data.model.SsrMarkerRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RicePromoterAnalyzer {

    suspend fun analyzeFasta(
        fastaText: String,
        fileName: String
    ): FastaAnalysisResult = withContext(Dispatchers.Default) {
        val trimmed = fastaText.trim()
        require(trimmed.isNotEmpty()) { "Selected FASTA file is empty." }

        val records = SeqIO.parseString(trimmed, "fasta")
        require(records.isNotEmpty()) {
            "No FASTA records found. Ensure the file begins with a '>' header line."
        }

        val firstRecord = records.first()
        val dnaSeq: DNASequence = when (val seq = firstRecord.sequence) {
            is DNASequence -> seq
            else -> {
                val cleaned = seq.sequence
                    .uppercase()
                    .replace(Regex("[\\s-]+"), "")
                DNASequence(cleaned, firstRecord.id, firstRecord.description)
            }
        }

        require(dnaSeq.length > 0) { "FASTA record '${firstRecord.id}' contains no nucleotide bases." }

        val countA = dnaSeq.count('A')
        val countT = dnaSeq.count('T')
        val countG = dnaSeq.count('G')
        val countC = dnaSeq.count('C')
        val atgcTotal = countA + countT + countG + countC

        // Formula: GC% = (G + C) / (A + T + G + C) * 100
        val gcPct = if (atgcTotal > 0) {
            ((countG + countC).toDouble() / atgcTotal.toDouble()) * 100.0
        } else {
            dnaSeq.gcContent()
        }

        // bioKT rice promoter cis-element summary & detailed hits
        val cisCounts: Map<String, Int> = dnaSeq.scanRicePromoter()
        val cisRawHits = CisElementScanner.scan(dnaSeq)
        val elementMeta = CisElementScanner.ELEMENTS.associateBy { it.name }

        val cisSummaries = cisCounts.entries.map { (name, count) ->
            val meta = elementMeta[name]
            CisElementSummary(
                name = name,
                count = count,
                family = meta?.family ?: "Rice Promoter",
                function = meta?.function ?: "Cis-regulatory motif"
            )
        }

        val cisHits = cisRawHits.map { hit ->
            CisElementHit(
                name = hit.name,
                motif = hit.motif,
                position = hit.position,
                strand = hit.strand.toString(),
                family = hit.family,
                function = hit.function
            )
        }

        // bioKT rice SSR microsatellite detection
        val rawSsrs = dnaSeq.detectSSRs()
        val ssrSummary = SSRDetector.summarise(rawSsrs)
        val ssrRecords = rawSsrs.map { ssr ->
            SsrMarkerRecord(
                id = ssr.id,
                motif = ssr.motif,
                motifLength = ssr.motifLength,
                repeats = ssr.repeats,
                start = ssr.start,
                end = ssr.end,
                length = ssr.length,
                type = ssr.type,
                sequence = ssr.sequence
            )
        }

        val mostCommonMotif = ssrSummary["most_common_motif"] as? String ?: "none"
        val avgRepeats = (ssrSummary["avg_repeats"] as? Double) ?: 0.0

        FastaAnalysisResult(
            fileName = fileName,
            sequenceId = firstRecord.id.ifBlank { "Promoter_Seq" },
            description = firstRecord.description,
            sequencePreview = dnaSeq.sequence.take(600),
            sequenceLength = dnaSeq.length,
            gcContentPct = gcPct,
            countA = countA,
            countT = countT,
            countG = countG,
            countC = countC,
            cisSummaries = cisSummaries,
            cisHits = cisHits,
            ssrMarkers = ssrRecords,
            mostCommonSsrMotif = mostCommonMotif,
            avgSsrRepeats = avgRepeats
        )
    }
}
