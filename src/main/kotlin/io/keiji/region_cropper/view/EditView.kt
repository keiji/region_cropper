package io.keiji.region_cropper.view

/*
Copyright 2016 Keiji ARIYAMA

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

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

const val MIN_REGION_SIZE = 10.0f

private val SELECTED = Color(1.0, 0.0, 0.0, 1.0)

private val regionColors: Array<Color> = arrayOf(
        Color.BLACK,
        Color.LIGHTGREEN,
        Color.BLUE,
        Color.BURLYWOOD,
        Color.AZURE,
        Color.ORANGE,
        Color.PINK,
        Color.AQUAMARINE,
        Color.OLIVEDRAB,
        Color.LIGHTCORAL
)

private val DRAGGING = Color.YELLOW

private val NOT_SELECTED = CandidateList.Region(
        -1.0, 0,
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
    var isFocus: Boolean = false

    private val draggingStartPoint: Point = Point(0.0, 0.0)
    private var draggingRect: CandidateList.Region.Rect? = null

    lateinit var imageData: Image
    lateinit var candidateList: CandidateList

    var selectedCandidate: CandidateList.Region = NOT_SELECTED
    private var selectedIndex: Int = 0

    private var reticle: Point = Point(0.0, 0.0)

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
                    event.isAltDown -> {
                        mode = Mode.Expand
                        event.consume()
                    }
                    event.isShortcutDown -> mode = Mode.Shrink
                    event.code == KeyCode.SPACE -> isFocus = true
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
                    event.code == KeyCode.END -> callback.onNextFile()
                    event.code == KeyCode.HOME -> callback.onPreviousFile()
                    event.code == KeyCode.DIGIT0 -> setLabel(0)
                    event.code == KeyCode.DIGIT1 -> setLabel(1)
                    event.code == KeyCode.DIGIT2 -> setLabel(2)
                    event.code == KeyCode.DIGIT3 -> setLabel(3)
                    event.code == KeyCode.DIGIT4 -> setLabel(4)
                    event.code == KeyCode.DIGIT5 -> setLabel(5)
                    event.code == KeyCode.DIGIT6 -> setLabel(6)
                    event.code == KeyCode.DIGIT7 -> setLabel(7)
                    event.code == KeyCode.DIGIT8 -> setLabel(8)
                    event.code == KeyCode.DIGIT9 -> setLabel(9)
                }
                draw()
            }
        })

        addEventFilter(KeyEvent.KEY_RELEASED, { event ->
            run {
                when {
                    event.code == KeyCode.SPACE -> isFocus = false
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
                            draggingStartPoint.x = tempPoint.x
                            draggingStartPoint.y = tempPoint.y
                            draggingRect = CandidateList.Region.Rect(0.0f, 0.0f, 0.0f, 0.0f)
                        }
                        draggingRect!!.left = Math.round(Math.min(draggingStartPoint.x, tempPoint.x)).toFloat()
                        draggingRect!!.right = Math.round(Math.max(draggingStartPoint.x, tempPoint.x)).toFloat()
                        draggingRect!!.top = Math.round(Math.min(draggingStartPoint.y, tempPoint.y)).toFloat()
                        draggingRect!!.bottom = Math.round(Math.max(draggingStartPoint.y, tempPoint.y)).toFloat()
                    }
                    event.button == MouseButton.PRIMARY && event.eventType == MouseEvent.MOUSE_RELEASED -> {
                        if (draggingRect != null) {
                            validateRect(draggingRect!!)
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

    private fun setLabel(label: Int) {
        if (selectedCandidate === NOT_SELECTED) {
            return
        }

        selectedCandidate.label = label
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

        if (candidateList.regions == null) {
            candidateList.regions = ArrayList<CandidateList.Region>()

            if (candidateList.detectedFaces != null) {
                for (c: CandidateList.Region in candidateList.detectedFaces.regions) {
                    this.candidateList.regions!!.add(c.copy())
                }
            }
        }

        Collections.sort(candidateList.regions, PositionComparator())

        if (candidateList.regions!!.size == 0) {
            selectedIndex = -1
            selectedCandidate = NOT_SELECTED
        } else {
            selectedIndex = if (!reverse) 0 else (this.candidateList.regions!!.size - 1)
            selectedCandidate = this.candidateList.regions!![selectedIndex]
        }

        onResize()
    }

    fun onResize() {
        val scaleHorizontal = width / imageData.width
        val scaleVertical = height / imageData.height

        scale = Math.min(scaleHorizontal, scaleVertical)

        paddingHorizontal = (width - (imageData.width * scale)) / 2
        paddingVertical = (height - (imageData.height * scale)) / 2

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

        gc.lineWidth = 1.0

        if (isFocus) {
            grayOut(gc)
        } else {
            for (c: CandidateList.Region in candidateList.regions!!) {
                if (c === selectedCandidate) {
                    continue
                }
                drawRegion(c, gc, false)
            }
        }

        drawRegion(selectedCandidate, gc, true)

        drawDraggingRect(gc, draggingRect)

//        gc.fillOval(tempPoint.x * scale, tempPoint.y * scale, 5.0, 5.0)

        gc.restore()
    }

    private fun grayOut(gc: GraphicsContext) {
        gc.save()

        // 背景をグレーアウト
        gc.fill = Color.grayRgb(0, 0.5)
        gc.fillRect(
                0.0,
                0.0,
                imageData.width * scale,
                imageData.height * scale)

        // 現在の選択領域を切り抜いた画像を重ねることで背景をくり抜いたように見せる
        gc.drawImage(
                imageData,
                selectedCandidate.rect.left.toDouble(),
                selectedCandidate.rect.top.toDouble(),
                selectedCandidate.rect.width().toDouble(),
                selectedCandidate.rect.height().toDouble(),
                selectedCandidate.rect.left * scale,
                selectedCandidate.rect.top * scale,
                selectedCandidate.rect.width() * scale,
                selectedCandidate.rect.height() * scale
        )

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
        if (c === NOT_SELECTED) {
            return
        }

        val rect: CandidateList.Region.Rect = c.rect

        gc.stroke = regionColors[c.label]
        gc.strokeRect(
                rect.left * scale,
                rect.top * scale,
                rect.width() * scale,
                rect.height() * scale
        )

        if (!isSelected) {
            return
        }

        gc.stroke = SELECTED
        gc.strokeRect(
                (rect.left - 5) * scale,
                (rect.top - 5) * scale,
                (rect.width() + 10) * scale,
                (rect.height() + 10) * scale
        )

        if (c.label == 0) {
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
        if (selectedIndex >= candidateList.regions!!.size - 1) {
            return false
        } else {
            selectedIndex++
        }

        selectedCandidate = candidateList.regions!![selectedIndex]
        return true
    }

    private fun selectPrevRegion(): Boolean {
        if (selectedIndex <= 0) {
            selectedIndex = 0
            return false
        } else {
            selectedIndex--
        }

        selectedCandidate = candidateList.regions!![selectedIndex]
        return true
    }

    private fun validateRect(rect: CandidateList.Region.Rect) {
        rect.left = Math.max(0.0f, rect.left)
        rect.top = Math.max(0.0f, rect.top)
        rect.right = Math.min(imageData.width.toFloat(), rect.right)
        rect.bottom = Math.min(imageData.height.toFloat(), rect.bottom)

        if (rect.width() < MIN_REGION_SIZE) {
            when {
                rect.left == 0.0f -> rect.right = MIN_REGION_SIZE
                rect.right == imageData.width.toFloat() -> rect.left = imageData.width.toFloat() - MIN_REGION_SIZE
                else -> rect.right = rect.left + MIN_REGION_SIZE
            }
        }
        if (rect.height() < MIN_REGION_SIZE) {
            when {
                rect.top == 0.0f -> rect.bottom = MIN_REGION_SIZE
                rect.bottom == imageData.height.toFloat() -> rect.top = imageData.height.toFloat() - MIN_REGION_SIZE
                else -> rect.bottom = rect.top + MIN_REGION_SIZE
            }
        }
    }

    private fun moveToLeft(size: Float) {
        if (selectedCandidate.label == 0) {
            return
        }
        selectedCandidate.rect.offset(-size, 0f)
        validateRect(selectedCandidate.rect)
    }

    private fun moveToTop(size: Float) {
        if (selectedCandidate.label == 0) {
            return
        }
        selectedCandidate.rect.offset(0f, -size)
        validateRect(selectedCandidate.rect)
    }

    private fun moveToRight(size: Float) {
        if (selectedCandidate.label == 0) {
            return
        }
        selectedCandidate.rect.offset(size, 0f)
        validateRect(selectedCandidate.rect)
    }

    private fun moveToBottom(size: Float) {
        if (selectedCandidate.label == 0) {
            return
        }
        selectedCandidate.rect.offset(0f, size)
        validateRect(selectedCandidate.rect)
    }

    private fun expandToLeft(size: Float) {
        if (selectedCandidate.label == 0) {
            return
        }
        selectedCandidate.rect.left -= size
        validateRect(selectedCandidate.rect)
    }

    private fun expandToTop(size: Float) {
        if (selectedCandidate.label == 0) {
            return
        }
        selectedCandidate.rect.top -= size
        validateRect(selectedCandidate.rect)
    }

    private fun expandToRight(size: Float) {
        if (selectedCandidate.label == 0) {
            return
        }
        selectedCandidate.rect.right += size
        validateRect(selectedCandidate.rect)
    }

    private fun expandToBottom(size: Float) {
        if (selectedCandidate.label == 0) {
            return
        }
        selectedCandidate.rect.bottom += size
        validateRect(selectedCandidate.rect)
    }

    fun save(imageFile: File?) {
        candidateList.save(imageFile)
    }

    private fun deleteRegion() {
        if (selectedCandidate.label == 0) {
            return
        }

        candidateList.regions!!.remove(selectedCandidate)

        if (selectedIndex > candidateList.regions!!.size - 1) {
            selectedIndex -= 1
        }

        if (selectedIndex < 0) {
            selectedCandidate = NOT_SELECTED
            return
        }

        selectedCandidate = candidateList.regions!![selectedIndex]
    }

    private fun addRect(rect: CandidateList.Region.Rect) {
        selectedCandidate = CandidateList.Region(1.0, 1, rect)
        candidateList.regions!!.add(selectedCandidate)

        Collections.sort(candidateList.regions!!, PositionComparator())

        selectedIndex = candidateList.regions!!.indexOf(selectedCandidate)
        validateRect(selectedCandidate.rect)
    }

    private val NEW_RECT_SIZE = 30

    private fun addRegion(x: Float, y: Float) {
        val halfSize = NEW_RECT_SIZE / 2
        selectedCandidate = CandidateList.Region(0.0, 1,
                CandidateList.Region.Rect(x - halfSize, y - halfSize, x + halfSize, y + halfSize))
        candidateList.regions!!.add(selectedCandidate)

        Collections.sort(candidateList.regions!!, PositionComparator())

        selectedIndex = candidateList.regions!!.indexOf(selectedCandidate)
    }

    private fun toggleFace() {
        selectedCandidate.label = selectedCandidate.label
    }

    fun reset(reverse: Boolean = false) {
        if (candidateList.regions == null) {
            candidateList.regions = ArrayList<CandidateList.Region>()
        } else {
            candidateList.regions!!.clear()
        }

        if (candidateList.detectedFaces != null) {
            for (c: CandidateList.Region in candidateList.detectedFaces!!.regions) {
                val candidate: CandidateList.Region = CandidateList.Region(0.0, c.label, c.rect.copy())
                this.candidateList.regions!!.add(candidate)
            }
        }

        Collections.sort(candidateList.regions, PositionComparator())

        if (candidateList.regions!!.size == 0) {
            selectedIndex = -1
            selectedCandidate = NOT_SELECTED
            return
        }

        selectedIndex = if (!reverse) 0 else (this.candidateList.regions!!.size - 1)
        selectedCandidate = this.candidateList.regions!![selectedIndex]
    }

}
