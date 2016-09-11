package io.keiji.region_cropper.view

import io.keiji.region_cropper.entity.PositionComparator
import io.keiji.region_cropper.entity.CandidateList
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.input.MouseButton
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

private val GRAY = Color(0.3, 0.3, 0.3, 1.0).let {
    it.brighter()
}

private val RED = Color(1.0, 0.0, 0.0, 1.0).let {
    it.darker()
}

private val NOT_SELECTED = CandidateList.Region(
        -1.0, false,
        CandidateList.Region.Rect(0.0f, 0.0f, 0.0f, 0.0f))

private data class Point(var x: Double, var y: Double) {
}

class EditView() : Canvas() {

    lateinit var imageData: Image
    lateinit var candidateList: CandidateList

    lateinit var selectedCandidate: CandidateList.Region
    private var selectedIndex: Int = 0

    private var reticle: Point = Point(0.0, 0.0)

    private val keyUpImage: Image

    init {
        keyUpImage = Image(javaClass.getClassLoader().getResourceAsStream("ic_keyboard_arrow_up_black_36dp.png"));

        addEventFilter(MouseEvent.ANY, { event ->
            run {
                when {
                    event.button == MouseButton.PRIMARY && event.eventType == MouseEvent.MOUSE_CLICKED -> {
                        val point = Point(0.0, 0.0)
                        convertLogicalPoint(event.sceneX, event.sceneY, point)
                        addRegion(point.x.toFloat(), point.y.toFloat())
                    }
                    else -> {
                        convertLogicalPoint(event.sceneX, event.sceneY, reticle)
                    }
                }
                draw()
            }
        })
    }

    var scale: Double = 1.0
    var paddingHorizontal: Double = 0.0
    var paddingVertical: Double = 0.0

    private fun convertLogicalPoint(x: Double, y: Double, point: Point) {
        val xPos: Double = (x - paddingHorizontal) / scale
        val yPos: Double = (y - paddingVertical) / scale

        point.x = xPos
        point.y = yPos
    }

    fun setData(image: Image, candidateList: CandidateList, reverse: Boolean) {
        imageData = image
        this.candidateList = candidateList

        if (this.candidateList.faces == null) {
            this.candidateList.faces = ArrayList<CandidateList.Region>()

            for (c: CandidateList.Region in candidateList.detectedFaces.regions) {
                val candidate: CandidateList.Region = CandidateList.Region(0.0, c.isFace, c.rect.copy())
                this.candidateList.faces!!.add(candidate)
            }
        }

        Collections.sort(candidateList.faces, PositionComparator())

        if (candidateList.faces!!.size == 0) {
            selectedIndex = -1
            return
        }

        selectedIndex = if (!reverse) 0 else (this.candidateList.faces!!.size - 1)
        selectedCandidate = this.candidateList.faces!![selectedIndex]

        onResize()
    }

    fun onResize() {
        val scaleHorizontal = width / imageData.width
        val scaleVertical = height / imageData.height

        scale = Math.min(scaleHorizontal, scaleVertical)

        paddingHorizontal = (width - (imageData.width * scale)) / 2
        paddingVertical = (height - 24 /* タイトルバーのサイズ */ - ((imageData.height) * scale)) / 2

        draw()
    }

    fun draw() {
        val gc = graphicsContext2D
        gc.clearRect(0.0, 0.0, width, height)

        gc.save()

        gc.translate(paddingHorizontal, paddingVertical)

        gc.drawImage(imageData,
                0.0,
                0.0,
                imageData.width * scale,
                imageData.height * scale)

        gc.lineWidth = 2.0

        for (c: CandidateList.Region in candidateList.faces!!) {

            when {
                !c.isFace && selectedCandidate == c -> gc.stroke = GRAY
                selectedCandidate == c -> gc.stroke = BLUE
                !c.isFace -> gc.stroke = BLACK
                else -> gc.stroke = RED
            }

            val rect = c.rect
            gc.strokeRect(
                    rect.left * scale,
                    rect.top * scale,
                    rect.width() * scale,
                    rect.height() * scale
            )
        }

//        gc.fillOval(
//                reticle.x * scale,
//                reticle.y * scale,
//                10.0, 10.0)

        gc.restore()
    }

    fun selectNextRegion(): Boolean {
        if (selectedIndex >= candidateList.faces!!.size - 1) {
            return false
        } else {
            selectedIndex++
        }

        selectedCandidate = candidateList.faces!![selectedIndex]
        return true
    }

    fun selectPrevRegion(): Boolean {
        if (selectedIndex <= 0) {
            selectedIndex = 0
            return false
        } else {
            selectedIndex--
        }

        selectedCandidate = candidateList.faces!![selectedIndex]
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

        candidateList.faces!!.remove(selectedCandidate)

        if (selectedIndex > candidateList.faces!!.size - 1) {
            selectedIndex -= 1
        }

        if (selectedIndex < 0) {
            selectedCandidate = NOT_SELECTED
            return
        }

        selectedCandidate = candidateList.faces!![selectedIndex]
    }

    fun addRegion(x: Float, y: Float) {
        selectedCandidate = CandidateList.Region(0.0, true,
                CandidateList.Region.Rect(x - 15, y - 15, x + 15, y + 15))
        candidateList.faces!!.add(selectedCandidate)
        Collections.sort(candidateList.faces!!, PositionComparator())

        selectedIndex = candidateList.faces!!.indexOf(selectedCandidate)
    }

    fun toggleFace() {
        selectedCandidate.isFace = !selectedCandidate.isFace
    }
}