package io.keiji.region_cropper.view

import io.keiji.region_cropper.entity.PositionComparator
import io.keiji.region_cropper.entity.CandidateList
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import java.io.File
import java.util.*

private val FACE = Color(0.0, 0.0, 1.0, 1.0)
private val FACE_SELECTED = Color(1.0, 0.0, 0.0, 1.0)

private val NOT_FACE = Color(0.0, 0.0, 0.0, 1.0)
private val NOT_FACE_SELECTED = Color(0.3, 0.3, 0.3, 1.0)

private val DRAGGING = Color(1.0, 1.0, 0.0, 1.0)

private val NOT_SELECTED = CandidateList.Region(
        -1.0, false,
        CandidateList.Region.Rect(0.0f, 0.0f, 0.0f, 0.0f))

private data class Point(var x: Double, var y: Double) {
}

class EditView(val callback: Callback) : Canvas() {

    interface Callback {
        fun onNextFile(reverse: Boolean = false)
        fun onPreviousFile(reverse: Boolean = false)
        fun onShowResetConfirmationDialog()
    }

    enum class Mode {
        Normal,
        Expand,
        Shrink,
    }

    var mode: Mode = Mode.Normal
    var showReticle: Boolean = false

    var draggingRect: CandidateList.Region.Rect? = null

    lateinit var imageData: Image
    lateinit var candidateList: CandidateList

    var selectedCandidate: CandidateList.Region = NOT_SELECTED
    private var selectedIndex: Int = 0

    private var reticle: Point = Point(0.0, 0.0)
    var paddingTop: Double = 0.0

    private val keyLeftImage: Image
    private val keyUpImage: Image
    private val keyRightImage: Image
    private val keyDownImage: Image

    private val tempPoint = Point(0.0, 0.0)

    init {
        val cl = javaClass.classLoader
        keyLeftImage = Image(cl.getResourceAsStream("ic_keyboard_arrow_left_red.png"));
        keyUpImage = Image(cl.getResourceAsStream("ic_keyboard_arrow_up_red.png"));
        keyRightImage = Image(cl.getResourceAsStream("ic_keyboard_arrow_right_red.png"));
        keyDownImage = Image(cl.getResourceAsStream("ic_keyboard_arrow_down_red.png"));

        addEventFilter(KeyEvent.KEY_PRESSED, { event ->
            run {

                val shiftValue = if (event.isShiftDown) 1.0f else 5.0f

                when {
                    event.code == KeyCode.SPACE -> showReticle = true
                    event.isShortcutDown -> mode = Mode.Shrink
                    event.isAltDown -> {
                        mode = Mode.Expand
                        event.consume()
                    }
                }

                when {
                    event.isShiftDown && event.code == KeyCode.ENTER -> {
                        if (!selectPrevRegion()) {
                            callback.onPreviousFile(true)
                        }
                    }
                    event.code == KeyCode.ENTER -> {
                        if (!selectNextRegion()) {
                            callback.onNextFile()
                        }
                    }
                    event.isShiftDown && event.code == KeyCode.TAB -> {
                        selectPrevRegion()
                    }
                    event.code == KeyCode.TAB -> {
                        selectNextRegion()
                    }
                    event.isShiftDown && event.code == KeyCode.D -> deleteRegion()
                    event.isShortcutDown && event.code == KeyCode.LEFT -> expandToRight(-shiftValue)
                    event.isShortcutDown && event.code == KeyCode.UP -> expandToBottom(-shiftValue)
                    event.isShortcutDown && event.code == KeyCode.RIGHT -> expandToLeft(-shiftValue)
                    event.isShortcutDown && event.code == KeyCode.DOWN -> expandToTop(-shiftValue)
                    event.isAltDown && event.code == KeyCode.LEFT -> expandToLeft(shiftValue)
                    event.isAltDown && event.code == KeyCode.UP -> expandToTop(shiftValue)
                    event.isAltDown && event.code == KeyCode.RIGHT -> expandToRight(shiftValue)
                    event.isAltDown && event.code == KeyCode.DOWN -> expandToBottom(shiftValue)
                    event.code == KeyCode.ESCAPE -> callback.onShowResetConfirmationDialog()
                    event.code == KeyCode.BACK_SPACE -> deleteRegion()
                    event.code == KeyCode.LEFT -> moveToLeft(shiftValue)
                    event.code == KeyCode.UP -> moveToTop(shiftValue)
                    event.code == KeyCode.RIGHT -> moveToRight(shiftValue)
                    event.code == KeyCode.DOWN -> moveToBottom(shiftValue)
                    event.code == KeyCode.N -> toggleFace()
                    event.code == KeyCode.END -> callback.onNextFile()
                    event.code == KeyCode.HOME -> callback.onPreviousFile()
                }
                draw()
            }
        })

        addEventFilter(KeyEvent.KEY_RELEASED, { event ->
            run {
                when {
                    event.code == KeyCode.SPACE -> showReticle = false
                    event.isShortcutDown -> mode = Mode.Shrink
                    event.isAltDown -> mode = Mode.Expand
                    else -> mode = Mode.Normal
                }
                draw()
            }
        })

        addEventFilter(MouseEvent.ANY, { event ->
            run {
                convertLogicalPoint(event.x, event.y, tempPoint)
                when {
                    event.button == MouseButton.PRIMARY && event.eventType == MouseEvent.MOUSE_DRAGGED -> {
                        if (draggingRect == null) {
                            draggingRect = CandidateList.Region.Rect(tempPoint.x.toFloat(),
                                    tempPoint.y.toFloat(),
                                    tempPoint.x.toFloat(),
                                    tempPoint.y.toFloat())
                        }

                        draggingRect!!.right = tempPoint.x.toFloat()
                        draggingRect!!.bottom = tempPoint.y.toFloat()
                    }
                    event.button == MouseButton.PRIMARY && event.eventType == MouseEvent.MOUSE_RELEASED -> {
                        if (draggingRect != null) {
                            addRect(draggingRect!!)
                            draggingRect = null
                        }
                    }
                    else -> convertLogicalPoint(event.sceneX, event.sceneY, reticle)
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

        if (candidateList.faces == null) {
            candidateList.faces = ArrayList<CandidateList.Region>()

            if (candidateList.detectedFaces != null) {
                for (c: CandidateList.Region in candidateList.detectedFaces.regions) {
                    val candidate: CandidateList.Region = CandidateList.Region(0.0, c.isFace, c.rect.copy())
                    this.candidateList.faces!!.add(candidate)
                }
            }
        }

        Collections.sort(candidateList.faces, PositionComparator())

        if (candidateList.faces!!.size == 0) {
            selectedIndex = -1
            selectedCandidate = NOT_SELECTED
        } else {
            selectedIndex = if (!reverse) 0 else (this.candidateList.faces!!.size - 1)
            selectedCandidate = this.candidateList.faces!![selectedIndex]
        }

        onResize()
    }

    fun onResize() {
        val scaleHorizontal = width / imageData.width
        val scaleVertical = height / imageData.height

        scale = Math.min(scaleHorizontal, scaleVertical)

        paddingHorizontal = (width - (imageData.width * scale)) / 2
        paddingVertical = (height - ((imageData.height - paddingTop) * scale)) / 2

        draw()
    }

    fun draw() {
        val gc = graphicsContext2D
        gc.clearRect(0.0, 0.0, width, height)

        gc.save()

        gc.translate(paddingHorizontal, paddingTop + paddingVertical)

        gc.drawImage(imageData,
                0.0,
                0.0,
                imageData.width * scale,
                imageData.height * scale)

        gc.lineWidth = 1.0

        for (c: CandidateList.Region in candidateList.faces!!) {
            if (c == selectedCandidate) {
                continue
            }
            drawRegion(c, gc, false)
        }

        drawRegion(selectedCandidate, gc, true)

        if (showReticle) {
            when {
                !selectedCandidate.isFace -> gc.stroke = NOT_FACE_SELECTED
                else -> gc.stroke = FACE_SELECTED
            }

            gc.strokeLine(selectedCandidate.rect.centerX() * scale,
                    0.0,
                    selectedCandidate.rect.centerX() * scale,
                    imageData.height * scale)
            gc.strokeLine(0.0,
                    selectedCandidate.rect.centerY() * scale,
                    imageData.width * scale,
                    selectedCandidate.rect.centerY() * scale)
        }

        drawDraggingRect(gc, draggingRect)

//        gc.fillOval(tempPoint.x * scale, tempPoint.y * scale, 5.0, 5.0)

        gc.restore()
    }

    private fun drawDraggingRect(gc: GraphicsContext, draggingRect: CandidateList.Region.Rect?) {
        if (draggingRect == null) {
            return
        }

        gc.stroke = DRAGGING
        gc.strokeRect(draggingRect.left.toDouble() * scale,
                draggingRect.top.toDouble() * scale,
                draggingRect.width().toDouble() * scale,
                draggingRect.height().toDouble() * scale)
    }

    private val margin: Double = 2.0

    private fun drawRegion(c: CandidateList.Region, gc: GraphicsContext, isSelected: Boolean) {
        val rect: CandidateList.Region.Rect = c.rect

        when {
            !c.isFace && selectedCandidate == c -> gc.stroke = NOT_FACE_SELECTED
            selectedCandidate == c -> gc.stroke = FACE_SELECTED
            !c.isFace -> gc.stroke = NOT_FACE
            else -> gc.stroke = FACE
        }

        gc.strokeRect(
                rect.left * scale,
                rect.top * scale,
                rect.width() * scale,
                rect.height() * scale
        )

        if (!isSelected || !c.isFace) {
            return
        }

        val horizontalCenter = (rect.left + rect.width() / 2) * scale - keyUpImage.width / 2
        val verticalCenter = (rect.top + rect.height() / 2) * scale - keyLeftImage.height / 2

        val topY = (rect.top - margin) * scale - keyUpImage.height
        val bottomY = (rect.bottom + margin) * scale
        val leftX = (rect.left - margin) * scale - keyLeftImage.width
        val rightX = (rect.right + margin) * scale

        when (mode) {
            Mode.Expand -> {
                gc.drawImage(keyUpImage, horizontalCenter, topY)
                gc.drawImage(keyDownImage, horizontalCenter, bottomY)
                gc.drawImage(keyLeftImage, leftX, verticalCenter)
                gc.drawImage(keyRightImage, rightX, verticalCenter)
            }
            Mode.Shrink -> {
                gc.drawImage(keyDownImage, horizontalCenter, topY)
                gc.drawImage(keyUpImage, horizontalCenter, bottomY)
                gc.drawImage(keyRightImage, leftX, verticalCenter)
                gc.drawImage(keyLeftImage, rightX, verticalCenter)
            }
            else -> {
            }
        }
    }

    private fun selectNextRegion(): Boolean {
        if (selectedIndex >= candidateList.faces!!.size - 1) {
            return false
        } else {
            selectedIndex++
        }

        selectedCandidate = candidateList.faces!![selectedIndex]
        return true
    }

    private fun selectPrevRegion(): Boolean {
        if (selectedIndex <= 0) {
            selectedIndex = 0
            return false
        } else {
            selectedIndex--
        }

        selectedCandidate = candidateList.faces!![selectedIndex]
        return true
    }

    private fun moveToLeft(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.offset(-size, 0f)
    }

    private fun moveToTop(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.offset(0f, -size)
    }

    private fun moveToRight(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.offset(size, 0f)
    }

    private fun moveToBottom(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.offset(0f, size)
    }

    private fun expandToLeft(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.left -= size
    }

    private fun expandToTop(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.top -= size
    }

    private fun expandToRight(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.right += size
    }

    private fun expandToBottom(size: Float) {
        if (!selectedCandidate.isFace) {
            return
        }
        selectedCandidate.rect.bottom += size
    }

    fun save(imageFile: File?) {
        candidateList.save(imageFile)
    }

    private fun deleteRegion() {
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

    private fun addRect(rect: CandidateList.Region.Rect) {
        selectedCandidate = CandidateList.Region(0.0, true, rect)
        candidateList.faces!!.add(selectedCandidate)

        Collections.sort(candidateList.faces!!, PositionComparator())

        selectedIndex = candidateList.faces!!.indexOf(selectedCandidate)
    }

    private val NEW_RECT_SIZE = 30

    private fun addRegion(x: Float, y: Float) {
        val halfSize = NEW_RECT_SIZE / 2
        selectedCandidate = CandidateList.Region(0.0, true,
                CandidateList.Region.Rect(x - halfSize, y - halfSize, x + halfSize, y + halfSize))
        candidateList.faces!!.add(selectedCandidate)

        Collections.sort(candidateList.faces!!, PositionComparator())

        selectedIndex = candidateList.faces!!.indexOf(selectedCandidate)
    }

    private fun toggleFace() {
        selectedCandidate.isFace = !selectedCandidate.isFace
    }

    fun reset(reverse: Boolean = false) {
        if (candidateList.faces == null) {
            candidateList.faces = ArrayList<CandidateList.Region>()
        } else {
            candidateList.faces!!.clear()
        }

        if (candidateList.detectedFaces != null) {
            for (c: CandidateList.Region in candidateList.detectedFaces!!.regions) {
                val candidate: CandidateList.Region = CandidateList.Region(0.0, c.isFace, c.rect.copy())
                this.candidateList.faces!!.add(candidate)
            }
        }

        Collections.sort(candidateList.faces, PositionComparator())

        if (candidateList.faces!!.size == 0) {
            selectedIndex = -1
            selectedCandidate = NOT_SELECTED
            return
        }

        selectedIndex = if (!reverse) 0 else (this.candidateList.faces!!.size - 1)
        selectedCandidate = this.candidateList.faces!![selectedIndex]
    }

}
