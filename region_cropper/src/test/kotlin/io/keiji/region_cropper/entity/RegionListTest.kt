package io.keiji.region_cropper.entity

import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URL

class RegionListTest {

    var candidateList: AnnotationList? = null

    @Before
    fun setUp() {
        val url: URL = AnnotationList::class.java.getResource("/mizuasato-BkoKavZCQAAjU7e.json")
        candidateList = AnnotationList.getInstance(url.path)
    }

    @After
    fun tearDown() {

    }

    @Test
    fun test() {
        assertNotNull(candidateList)
        assertEquals("Megane Co", candidateList!!.generator)
        assertEquals("mizuasato-BkoKavZCQAAjU7e.jpg", candidateList!!.fileName)
        assertEquals("2016-09-11T13:11:03.074767", candidateList!!.createdAt)

        assertEquals(7, candidateList!!.regions.size)

        candidateList!!.regions[0].let {
            assertEquals(0.9999978542327881, it.probability, 0.0000000000000001)
            assertEquals(true, it.label)

            it.rect.let {
                assertEquals(216.0F, it.left)
                assertEquals(137.0F, it.top)
                assertEquals(367.0F, it.right)
                assertEquals(334.0F, it.bottom)
            }
        }
    }
}