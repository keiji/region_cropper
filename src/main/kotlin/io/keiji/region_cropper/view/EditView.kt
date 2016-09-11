package io.keiji.region_cropper.view

import io.keiji.region_cropper.entity.CandidateComparator
import io.keiji.region_cropper.entity.CandidateList
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import java.io.File
import java.util.*

private val BLUE = Color(0.0, 0.0, 1.0, 1.0).let {
    it.darker()
}

private val BLACK = Color(0.0, 0.0, 0.0, 1.0).let {
    it.brighter()
}

private val RED = Color(1.0, 0.0, 0.0, 1.0).let {
    it.darker()
}

class EditView() : Canvas() {

    lateinit var imageData: Image
    lateinit var candidateList: CandidateList

    lateinit var selectedCandidate: CandidateList.Candidate
    private var selectedIndex: Int = 0

    init {
        addEventFilter(MouseEvent.ANY, { event ->
            run {
//                println(event)
            }
        })
    }

    fun setData(image: Image, candidateList: CandidateList, reverse: Boolean) {
        imageData = image
        this.candidateList = candidateList

        if (this.candidateList.regions == null) {
            this.candidateList.regions = ArrayList<CandidateList.Candidate>()

            for (c: CandidateList.Candidate in candidateList.candidates) {
                val cRect = c.rect
                val rect = CandidateList.Candidate.Rect(cRect.left, cRect.top, cRect.right, cRect.bottom)
                val candidate: CandidateList.Candidate = CandidateList.Candidate(c.likelihood, c.isFace, rect)
                this.candidateList.regions!!.add(candidate)
            }
        }

        Collections.sort(candidateList.regions, CandidateComparator())

        selectedIndex = if (!reverse) 0 else (this.candidateList.regions!!.size - 1)
        selectedCandidate = this.candidateList.regions!![selectedIndex]
    }

    var scale: Double = 1.0
    var paddingHorizontal: Double = 0.0
    var paddingVertical: Double = 0.0

    fun draw() {
        graphicsContext2D.clearRect(0.0, 0.0, width, height)

        val scaleHorizontal = width / imageData.width
        val scaleVertical = height / imageData.height

        scale = Math.min(scaleHorizontal, scaleVertical)

        paddingHorizontal = (width - (imageData.width * scale)) / 2
        paddingVertical = (height - 24 /* タイトルバーのサイズ */ - ((imageData.height) * scale)) / 2

        graphicsContext2D.drawImage(imageData,
                paddingHorizontal,
                paddingVertical,
                imageData.width * scale,
                imageData.height * scale)

        graphicsContext2D.lineWidth = 2.0

        for (c: CandidateList.Candidate in candidateList.regions!!) {

            when {
                selectedCandidate == c -> graphicsContext2D.stroke = BLUE
                !c.isFace -> graphicsContext2D.stroke = BLACK
                else -> graphicsContext2D.stroke = RED
            }

            val rect = c.rect
            graphicsContext2D.strokeRect(
                    paddingHorizontal + rect.left * scale,
                    paddingVertical + rect.top * scale,
                    rect.width() * scale,
                    rect.height() * scale
            )
        }
    }

    fun selectNextRegion(): Boolean {
        if (selectedIndex >= candidateList.regions!!.size - 1) {
            return false
        } else {
            selectedIndex++
        }

        selectedCandidate = candidateList.regions!![selectedIndex]
        return true
    }

    fun selectPrevRegion(): Boolean {
        if (selectedIndex <= 0) {
            selectedIndex = 0
            return false
        } else {
            selectedIndex--
        }

        selectedCandidate = candidateList.regions!![selectedIndex]
        return true
    }

    fun moveToLeft(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.offset(-size, 0f)
    }

    fun moveToTop(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.offset(0f, -size)
    }

    fun moveToRight(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.offset(size, 0f)
    }

    fun moveToBottom(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.offset(0f, size)
    }

    fun expandToLeft(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.left -= size
    }

    fun expandToTop(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.top -= size
    }

    fun expandToRight(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.right += size
    }

    fun expandToBottom(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.bottom += size
    }

    fun save(imageFile: File?) {
        candidateList.save(imageFile)
    }

    fun deleteRegion() {
        if (!selectedCandidate.isFace) {
            return
        }
        candidateList.regions!!.remove(selectedCandidate)
        selectedIndex = 0
    }

    fun addRegion(x: Float, y: Float) {
        selectedCandidate = CandidateList.Candidate(0.0, true,
                CandidateList.Candidate.Rect(x - 15, y - 15, x + 15, y + 15))
        candidateList.regions!!.add(selectedCandidate)
        selectedIndex = candidateList.regions!!.size - 1
    }

    fun toggleFace() {
        selectedCandidate.isFace = !selectedCandidate.isFace
    }
}