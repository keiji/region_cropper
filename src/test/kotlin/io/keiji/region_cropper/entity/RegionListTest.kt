package io.keiji.region_cropper.entity

import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URL

class RegionListTest {

    var candidateList: CandidateList? = null

    @Before
    fun setUp() {
        val url: URL = CandidateList::class.java.getResource("/mizuasato-BkoKavZCQAAjU7e.json")
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
        assertEquals("mizuasato-BkoKavZCQAAjU7e.jpg", candidateList!!.fileName)
        assertEquals("2016-09-11T13:11:03.074767", candidateList!!.createdAt)

        for (c: CandidateList.Region in candidateList!!.detectedFaces.regions) {
            assertEquals(0.9985449314117432, c.likelihood, 0.0000000000000001)
            assertEquals(true, c.isFace)

            val rect: CandidateList.Region.Rect = c.rect
            assertEquals(519.0F, rect.left)
            assertEquals(5.0F, rect.top)
            assertEquals(598.0F, rect.right)
            assertEquals(78.0F, rect.bottom)
        }
    }
}