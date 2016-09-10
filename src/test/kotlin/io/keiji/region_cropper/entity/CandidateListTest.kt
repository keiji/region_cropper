package io.keiji.region_cropper.entity

import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URL

class CandidateListTest {

    var candidateList: CandidateList? = null

    @Before
    fun setUp() {
        val url: URL = CandidateList::class.java.getResource("/rariemonn765-4IfIU45dAiUFmVsw.json")
        candidateList = CandidateList.getInstance(url.path)
    }

    @After
    fun tearDown() {

    }

    @Test
    fun test() {
        if (candidateList == null) {
            fail()
        }

        assertEquals("Megane Co", candidateList!!.generator)
        assertEquals("rariemonn765-4IfIU45dAiUFmVsw.jpg", candidateList!!.fileName)
        assertEquals("20160906153112", candidateList!!.modelVersion)
        assertEquals(null, candidateList!!.engineVersion)
        assertEquals("selective_search", candidateList!!.mode)
        assertEquals(1, candidateList!!.candidates.size)
        assertEquals("2016-09-06T10:11:06.313337", candidateList!!.createdAt)

        for (c: CandidateList.Candidate in candidateList!!.candidates) {
            assertEquals(0.9985449314117432, c.likelihood, 0.0000000000000001)
            assertEquals(true, c.isFace)

            val rect: CandidateList.Candidate.Rect = c.rect
            assertEquals(519.0F, rect.left)
            assertEquals(5.0F, rect.top)
            assertEquals(598.0F, rect.right)
            assertEquals(78.0F, rect.bottom)
        }
    }
}