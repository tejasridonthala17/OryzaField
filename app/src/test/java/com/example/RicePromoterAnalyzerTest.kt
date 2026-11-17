package com.example

import com.example.data.bio.RicePromoterAnalyzer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RicePromoterAnalyzerTest {

    @Test
    fun analyzeFasta_computesGcLengthCisAndSsrs() = runTest {
        // 1000-like synthetic rice promoter containing ABRE (ACGTGG), W-box (TTGACT),
        // TATA-box (TATAAAT), and an AT dinucleotide SSR (AT x 7 = 14 bp)
        val promoterFasta = """
            >OsDREB1A_promoter Oryza sativa japonica promoter
            GCGCGCGCGCACGTGGTTGACTTATAAATATATATATATATATATACACACGTGTGACGTACGTAGCTAGCTAG
        """.trimIndent()

        val result = RicePromoterAnalyzer.analyzeFasta(promoterFasta, "OsDREB1A.fasta")

        assertEquals("OsDREB1A_promoter", result.sequenceId)
        assertEquals("OsDREB1A.fasta", result.fileName)
        assertTrue(result.sequenceLength > 50)

        // Verify GC% = (G + C) / (A + T + G + C) * 100
        val expectedGc = (result.countG + result.countC).toDouble() /
            (result.countA + result.countT + result.countG + result.countC) * 100.0
        assertEquals(expectedGc, result.gcContentPct, 0.0001)

        // Verify bioKT scanRicePromoter() detected ABRE / W-box / TATA-box
        val cisNames = result.cisSummaries.map { it.name }.toSet()
        assertTrue("Expected ABRE or ABRE-like in $cisNames", cisNames.contains("ABRE") || cisNames.contains("ABRE-like"))
        assertTrue("Expected W-box in $cisNames", cisNames.contains("W-box"))
        assertTrue("Expected TATA-box in $cisNames", cisNames.contains("TATA-box"))

        // Verify bioKT detectSSRs() detected AT repeat
        assertTrue("Expected at least 1 SSR marker", result.ssrMarkers.isNotEmpty())
        assertTrue(result.ssrMarkers.any { it.motif == "AT" && it.repeats >= 6 })
    }

    @Test
    fun analyzeFasta_exact45PercentGcCase() = runTest {
        // 450 G/C and 550 A/T -> exact 45.0%
        val seq = "G".repeat(225) + "C".repeat(225) + "A".repeat(275) + "T".repeat(275)
        val fasta = ">test_1000bp\n$seq"
        val result = RicePromoterAnalyzer.analyzeFasta(fasta, "test.fa")
        assertEquals(1000, result.sequenceLength)
        assertEquals(45.0, result.gcContentPct, 0.0001)
    }
}
