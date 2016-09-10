package io.keiji.region_cropper.view

import io.keiji.region_cropper.entity.CandidateList
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import tornadofx.View
import java.io.File
import java.util.*

class EditView : Canvas() {
    var imageData: Image? = null
    var candidateList: CandidateList? = null

    var selectedCandidate: CandidateList.Candidate? = null
    private var selectedIndex: Int = 0

    init {

        addEventFilter(MouseEvent.ANY, { event ->
            run {
//                println(event)
            }
        })
    }

    fun setData(image: Image, candidateList: CandidateList, reverse: Boolean) {
        println("setData")
        imageData = image
        this.candidateList = candidateList

        if (this.candidateList!!.regions == null) {
            this.candidateList!!.regions = ArrayList<CandidateList.Candidate>()

            for (c: CandidateList.Candidate in candidateList!!.candidates) {
                val cRect = c.rect
                val rect = CandidateList.Candidate.Rect(cRect.left, cRect.top, cRect.right, cRect.bottom)
                val candidate: CandidateList.Candidate = CandidateList.Candidate(c.likelihood, c.isFace, rect)
                this.candidateList!!.regions.add(candidate)
            }
        }

        selectedIndex = if (!reverse) 0 else (this.candidateList!!.regions.size - 1)
        selectedCandidate = this.candidateList!!.regions[selectedIndex]
        draw()
    }

    fun draw() {
        if (imageData == null || candidateList == null) {
            println("null")
            return
        }

        println("not null")

        graphicsContext2D.clearRect(0.0, 0.0, width, height)

        val scaleHorizontal = width / imageData!!.width
        val scaleVertical = height / imageData!!.height
        val scale = Math.min(scaleHorizontal, scaleVertical)

        val leftMargin = (width - (imageData!!.width * scale)) / 2
        val topMargin = (height - 24 /* タイトルバーのサイズ */ - ((imageData!!.height) * scale)) / 2

        graphicsContext2D.drawImage(imageData,
                leftMargin,
                topMargin,
                imageData!!.width * scale,
                imageData!!.height * scale)

        graphicsContext2D.lineWidth = 2.0

        for (c: CandidateList.Candidate in candidateList!!.regions) {

            if (selectedCandidate == c) {
                graphicsContext2D.stroke = Color(0.0, 0.0, 1.0, 1.0).let {
                    it.darker()
                }
            } else if (!c.isFace) {
                graphicsContext2D.stroke = Color(0.0, 0.0, 0.0, 1.0).let {
                    it.brighter()
                }
            } else {
                graphicsContext2D.stroke = Color(1.0, 0.0, 0.0, 1.0).let {
                    it.darker()
                }
            }

            val rect = c.rect
            graphicsContext2D.strokeRect(
                    leftMargin + rect.left * scale,
                    topMargin + rect.top * scale,
                    rect.width() * scale,
                    rect.height() * scale
            )
        }

        println("draw finished.")
    }

    fun selectNextRegion(): Boolean {
        if (selectedIndex >= candidateList!!.regions.size - 1) {
            return false
        } else {
            selectedIndex++
        }

        selectedCandidate = candidateList!!.regions[selectedIndex]
        return true
    }

    fun selectPrevRegion(): Boolean {
        if (selectedIndex <= 0) {
            selectedIndex = 0
            return false
        } else {
            selectedIndex--
        }

        selectedCandidate = candidateList!!.regions[selectedIndex]
        return true
    }

    fun moveToLeft(size: Float) {
        selectedCandidate!!.rect.offset(-size, 0f)
    }

    fun moveToTop(size: Float) {
        selectedCandidate!!.rect.offset(0f, -size)
    }

    fun moveToRight(size: Float) {
        selectedCandidate!!.rect.offset(size, 0f)
    }

    fun moveToBottom(size: Float) {
        selectedCandidate!!.rect.offset(0f, size)
    }

    fun expandToLeft(size: Float) {
        selectedCandidate!!.rect.left -= size
    }

    fun expandToTop(size: Float) {
        selectedCandidate!!.rect.top -= size
    }

    fun expandToRight(size: Float) {
        selectedCandidate!!.rect.right += size
    }

    fun expandToBottom(size: Float) {
        selectedCandidate!!.rect.bottom += size
    }

    fun save(imageFile: File?) {
        candidateList!!.save(imageFile)
    }

    fun deleteRegion() {
        candidateList!!.regions.removeAt(selectedIndex)
        selectedIndex = 0
        selectedCandidate = null
    }

    fun addRegion(x: Float, y: Float) {
        selectedCandidate = CandidateList.Candidate(0.0, true,
                CandidateList.Candidate.Rect(x - 15, y - 15, x + 15, y + 15))
        candidateList!!.regions.add(selectedCandidate!!)
        selectedIndex = candidateList!!.regions.size - 1
    }

    fun toggleFace() {
        selectedCandidate!!.isFace = !selectedCandidate!!.isFace
    }
}