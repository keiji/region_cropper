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

import io.keiji.region_cropper.entity.*
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import java.util.*

const val LIMIT_SAVE_STATE = 200

private val NOT_SELECTED = Region(-1.0, 0,
        Region.Rect(0.0f, 0.0f, 0.0f, 0.0f))

private data class Point(
        var x: Double,
        var y: Double) {
}

class EditView(val callback: Callback, var settings: Settings) : Canvas() {

    data class History(
            var startIndex: Int = 0,
            var index: Int = 0,
            var histories: Array<History.State> = Array(LIMIT_SAVE_STATE, { History.State(0) })) {

        data class State(var selectedRegionIndex: Int) {
            var isUpdated: Boolean = false
            lateinit var annotationList: AnnotationList
        }

        fun clear() {
            startIndex = 0
            index = 0
        }

        fun push(isUpdated: Boolean, selectedRegionIndex: Int, candidateList: AnnotationList) {
            val state = histories[index]
            state.isUpdated = isUpdated
            state.selectedRegionIndex = selectedRegionIndex
            state.annotationList = candidateList

            index++
            if (index >= histories.size) {
                index %= histories.size
                startIndex = (startIndex + 1) % histories.size
            }
        }

        fun pop(): State? {
            index--
            if (index < startIndex) {
                index = startIndex
                return null
            }

            return histories[index]
        }
    }

    interface Callback {
        fun onNextFile(reverse: Boolean = false)
        fun onPreviousFile(reverse: Boolean = false)
    }

    enum class Mode {
        Normal,
        Expand,
        Shrink,
    }

    var mode: Mode = Mode.Normal

    enum class Focus(val isEnabled: Boolean) {
        Off(false),
        On(true),
        Locked(true),
    }

    var isFocus: Focus = Focus.Off

    private var isInvalidated: Boolean = true

    private val draggingStartPoint = Point(0.0, 0.0)
    private var draggingRect: Region.Rect? = null

    lateinit var imageData: Image
    lateinit var annotationList: AnnotationList

    private val history: History = History()

    var isUpdated: Boolean = false

    var selectedCandidate: Region = NOT_SELECTED
    private var selectedIndex: Int = 0

    private var reticle: Point = Point(0.0, 0.0)

    val cl = javaClass.classLoader
    private val keyLeftImage = Image(cl.getResourceAsStream("ic_keyboard_arrow_left_red.png"))
    private val keyUpImage = Image(cl.getResourceAsStream("ic_keyboard_arrow_up_red.png"))
    private val keyRightImage = Image(cl.getResourceAsStream("ic_keyboard_arrow_right_red.png"))
    private val keyDownImage = Image(cl.getResourceAsStream("ic_keyboard_arrow_down_red.png"))

    private val tempPoint = Point(0.0, 0.0)

    init {
        addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            val shiftValue = if (event.isShiftDown) minRegionSize else minRegionSize * 4.0f

            val labelSetting: Settings.Label = settings.labelSettings[selectedCandidate.label]
            val editable: Boolean = labelSetting.editable && labelSetting.visible && !settings.viewOnly

            when {
                event.isShiftDown && event.code == KeyCode.SPACE -> isFocus = Focus.Locked
                event.code == KeyCode.SPACE -> isFocus = Focus.On
                !editable -> {
                    /* do nothing */
                }
                event.isAltDown -> {
                    mode = Mode.Expand
                    event.consume()
                }
                event.isShortcutDown -> mode = Mode.Shrink
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

                event.isShortcutDown && event.code == KeyCode.Z -> restoreState()
                event.isControlDown && event.code == KeyCode.Z -> restoreState()

                event.code == KeyCode.BACK_SPACE -> deleteRegion()
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

                event.code == KeyCode.DECIMAL -> {
                    if (!selectPrevRegion()) {
                        callback.onPreviousFile(true)
                    }
                }
                event.code == KeyCode.NUMPAD0 -> setLabel(0, true)
                event.code == KeyCode.NUMPAD1 -> setLabel(1, true)
                event.code == KeyCode.NUMPAD2 -> setLabel(2, true)
                event.code == KeyCode.NUMPAD3 -> setLabel(3, true)
                event.code == KeyCode.NUMPAD4 -> setLabel(4, true)
                event.code == KeyCode.NUMPAD5 -> setLabel(5, true)
                event.code == KeyCode.NUMPAD6 -> setLabel(6, true)
                event.code == KeyCode.NUMPAD7 -> setLabel(7, true)
                event.code == KeyCode.NUMPAD8 -> setLabel(8, true)
                event.code == KeyCode.NUMPAD9 -> setLabel(9, true)

                !editable -> {
                    /* do nothing */
                }

                event.code == KeyCode.ADD -> expand(left = -shiftValue, top = -shiftValue, right = shiftValue, bottom = shiftValue)
                event.code == KeyCode.SUBTRACT -> expand(left = shiftValue, top = shiftValue, right = -shiftValue, bottom = -shiftValue)
                event.isShiftDown && event.code == KeyCode.D -> deleteRegion()
                event.isShortcutDown && event.code == KeyCode.LEFT -> expand(right = -shiftValue)
                event.isShortcutDown && event.code == KeyCode.UP -> expand(bottom = -shiftValue)
                event.isShortcutDown && event.code == KeyCode.RIGHT -> expand(left = shiftValue)
                event.isShortcutDown && event.code == KeyCode.DOWN -> expand(top = shiftValue)
                event.isAltDown && event.code == KeyCode.LEFT -> expand(left = -shiftValue)
                event.isAltDown && event.code == KeyCode.UP -> expand(top = -shiftValue)
                event.isAltDown && event.code == KeyCode.RIGHT -> expand(right = shiftValue)
                event.isAltDown && event.code == KeyCode.DOWN -> expand(bottom = shiftValue)
                event.code == KeyCode.LEFT -> move(-shiftValue, 0.0f)
                event.code == KeyCode.UP -> move(0.0f, -shiftValue)
                event.code == KeyCode.RIGHT -> move(shiftValue, 0.0f)
                event.code == KeyCode.DOWN -> move(0.0f, shiftValue)
            }
            redraw()
        }

        addEventFilter(KeyEvent.KEY_RELEASED) { event ->
            when {
                isFocus != Focus.Locked && event.code == KeyCode.SPACE -> isFocus = Focus.Off
                event.isShortcutDown -> mode = Mode.Shrink
                event.isAltDown -> mode = Mode.Expand
                else -> mode = Mode.Normal
            }
            redraw()
        }

        addEventFilter(MouseEvent.ANY) { event ->
            convertLogicalPoint(event.x, event.y, tempPoint)

            cursor = when (event.eventType) {
                MouseEvent.MOUSE_ENTERED -> Cursor.CROSSHAIR
                MouseEvent.MOUSE_EXITED -> Cursor.DEFAULT
                else -> Cursor.DEFAULT
            }

            when {
                settings.viewOnly -> {
                    /* do nothing */
                }
                event.button == MouseButton.PRIMARY && event.eventType == MouseEvent.MOUSE_DRAGGED -> {
                    if (draggingRect == null) {
                        draggingStartPoint.x = tempPoint.x
                        draggingStartPoint.y = tempPoint.y
                        draggingRect = Region.Rect(0.0f, 0.0f, 0.0f, 0.0f)
                    }

                    draggingRect?.let {
                        it.left = Math.min(draggingStartPoint.x, tempPoint.x).toFloat()
                        it.right = Math.max(draggingStartPoint.x, tempPoint.x).toFloat()
                        it.top = Math.min(draggingStartPoint.y, tempPoint.y).toFloat()
                        it.bottom = Math.max(draggingStartPoint.y, tempPoint.y).toFloat()
                    }

                    isInvalidated = true
                }
                event.button == MouseButton.PRIMARY && event.eventType == MouseEvent.MOUSE_RELEASED -> {
                    draggingRect?.let {
                        addRect(it)
                        isInvalidated = true
                    }
                    draggingRect = null
                }
                else -> convertLogicalPoint(event.sceneX, event.sceneY, reticle)
            }
            draw()
        }
    }

    private fun onUpdate() {
        isUpdated = true
        history.push(isUpdated, selectedIndex, annotationList.deepCopy())
    }

    fun restoreState() {
        val state: History.State? = history.pop()
        state ?: return

        isUpdated = state.isUpdated
        selectedIndex = state.selectedRegionIndex
        annotationList = state.annotationList

        if (selectedIndex >= annotationList.regions.size
                || (selectedIndex < 0 && annotationList.regions.isNotEmpty())) {
            selectedIndex = annotationList.regions.size - 1
        }

        selectedCandidate = if (selectedIndex < 0) NOT_SELECTED else annotationList.regions[selectedIndex]

        redraw()
    }

    private fun setLabel(label: Int, moveNext: Boolean = false) {
        if (selectedCandidate === NOT_SELECTED) {
            return
        }

        onUpdate()

        selectedCandidate.label = label

        if (moveNext) {
            selectNextRegion()
        }
    }

    var scale: Double = 1.0
    var paddingHorizontal: Double = 0.0
    var paddingVertical: Double = 0.0

    private fun convertLogicalPoint(x: Double, y: Double, point: Point) {
        val xPos: Double = (x - paddingHorizontal) / scale / imageData.width
        val yPos: Double = (y - paddingVertical) / scale / imageData.height

        point.x = xPos
        point.y = yPos
    }

    fun setData(image: Image, annotationList: AnnotationList, reverse: Boolean) {
        isUpdated = false
        history.clear()

        imageData = image
        this.annotationList = annotationList

        Collections.sort(annotationList.regions, PositionComparator())

        selectedIndex = -1
        selectedCandidate = NOT_SELECTED

        selectNextRegion()

        onResize()
    }

    fun onResize() {
        val scaleHorizontal = width / imageData.width
        val scaleVertical = height / imageData.height

        scale = Math.min(scaleHorizontal, scaleVertical)

        paddingHorizontal = (width - (imageData.width * scale)) / 2
        paddingVertical = (height - (imageData.height * scale)) / 2

        redraw()
    }

    fun redraw() {
        isInvalidated = true
        draw()
    }

    private fun draw() {
        if (!isInvalidated) {
            return
        }

        isInvalidated = false

        graphicsContext2D.let { gc ->
            gc.clearRect(0.0, 0.0, width, height)

            gc.save()

            gc.translate(paddingHorizontal, paddingVertical)
            gc.drawImage(imageData,
                    0.0,
                    0.0,
                    imageData.width * scale,
                    imageData.height * scale)

            gc.lineWidth = 2.0

            if (settings.viewOnly) {
                gc.save()

                gc.lineWidth = 1.0
                gc.setLineDashes(5.0, 2.0)

                gc.restore()
                return
            }

            for (c: Region in annotationList.regions) {
                if (c === selectedCandidate) {
                    continue
                }
                drawRegion(c, gc)
            }

            if (isFocus.isEnabled) {
                grayOut(gc)
            }

            drawRegion(selectedCandidate, gc, isSelected = true)

            draggingRect?.let {
                drawDraggingRect(gc, it)
            }

            gc.restore()
        }
    }

    private fun grayOut(gc: GraphicsContext) {
        gc.save()

        // 背景をグレーアウト
        val color: Color = settings.labelSettings[selectedCandidate.label].webColor.darker()
        gc.fill = Color.rgb(toColor(color.red), toColor(color.green), toColor(color.blue), 0.5)
        gc.fillRect(
                0.0,
                0.0,
                imageData.width * scale,
                imageData.height * scale)

        // 現在の選択領域を切り抜いた画像を重ねることで背景をくり抜いたように見せる
        gc.drawImage(
                imageData,
                selectedCandidate.rect.left * imageData.width,
                selectedCandidate.rect.top * imageData.height,
                selectedCandidate.rect.width * imageData.width,
                selectedCandidate.rect.height * imageData.height,
                selectedCandidate.rect.left * scale * imageData.width,
                selectedCandidate.rect.top * scale * imageData.height,
                selectedCandidate.rect.width * scale * imageData.width,
                selectedCandidate.rect.height * scale * imageData.height
        )

        gc.restore()
    }

    private fun toColor(color: Double): Int {
        return (color * 255).toInt()
    }

    private fun drawDraggingRect(gc: GraphicsContext, draggingRect: Region.Rect) {
        gc.stroke = settings.draggingRegionStrokeWebColor
        gc.strokeRect(
                draggingRect.left.toDouble() * scale * imageData.width,
                draggingRect.top.toDouble() * scale * imageData.height,
                draggingRect.width.toDouble() * scale * imageData.width,
                draggingRect.height.toDouble() * scale * imageData.height)
    }

    private val margin: Double = 2.0

    private fun drawRegion(c: Region, gc: GraphicsContext, isSelected: Boolean = false) {
        if (c === NOT_SELECTED) {
            return
        }

        val visible = settings.labelSettings[c.label].visible
        if (!visible) {
            return
        }

        val rect: Region.Rect = c.rect.copy().also {
            it.left = it.left * imageData.width.toFloat()
            it.top = it.top * imageData.height.toFloat()
            it.right = it.right * imageData.width.toFloat()
            it.bottom = it.bottom * imageData.height.toFloat()
        }

        val color = settings.labelSettings[c.label].webColor

        gc.stroke = color
        gc.strokeRect(
                rect.left * scale,
                rect.top * scale,
                rect.width * scale,
                rect.height * scale
        )

        if (!isSelected) {
            return
        }

        gc.stroke = settings.selectedRegionStrokeWebColor
        gc.strokeRect(
                (rect.left) * scale - 2,
                (rect.top) * scale - 2,
                (rect.width) * scale + 4,
                (rect.height) * scale + 4
        )

        val horizontalCenter = (rect.left + rect.width / 2) * scale - keyUpImage.width / 2
        val verticalCenter = (rect.top + rect.height / 2) * scale - keyLeftImage.height / 2

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
        if (settings.viewOnly) {
            return false
        }

        if (selectedIndex == annotationList.regions.size - 1) {
            return false
        }

        selectedIndex++
        selectedIndex = Math.min(selectedIndex, annotationList.regions.size - 1)

        if (selectedIndex >= 0) {
            selectedCandidate = annotationList.regions[selectedIndex]
        } else {
            selectedCandidate = NOT_SELECTED
        }

        if (!settings.labelSettings[selectedCandidate.label].visible) {
            return selectNextRegion()
        }
        return true
    }

    private fun selectPrevRegion(): Boolean {
        if (settings.viewOnly) {
            return false
        }

        if (selectedIndex == 0) {
            return false
        }

        selectedIndex--
        selectedIndex = Math.max(selectedIndex, 0)

        if (selectedIndex >= 0) {
            selectedCandidate = annotationList.regions[selectedIndex]
        } else {
            selectedCandidate = NOT_SELECTED
        }

        if (!settings.labelSettings[selectedCandidate.label].visible) {
            return selectPrevRegion()
        }
        return true
    }

    private fun validateRect(rect: Region.Rect) {
        rect.left = Math.max(0.0f, rect.left)
        rect.top = Math.max(0.0f, rect.top)
        rect.right = Math.min(1.0f, rect.right)
        rect.bottom = Math.min(1.0f, rect.bottom)

        if (rect.width < minRegionSize) {
            when {
                rect.left == 0.0f -> rect.right = minRegionSize
                rect.right == imageData.width.toFloat() -> rect.left = 1.0f - minRegionSize
                else -> rect.right = rect.left + minRegionSize
            }
        }
        if (rect.height < minRegionSize) {
            when {
                rect.top == 0.0f -> rect.bottom = minRegionSize
                rect.bottom == imageData.height.toFloat() -> rect.top = 1.0f - minRegionSize
                else -> rect.bottom = rect.top + minRegionSize
            }
        }
    }

    private val minRegionSize
        get() = Math.min(
                1.0f / imageData.width.toFloat(),
                1.0f / imageData.height.toFloat()
        )

    private fun move(x: Float, y: Float) {
        onUpdate()

        selectedCandidate.rect.offset(x, y)
        validateRect(selectedCandidate.rect)
    }

    private fun expand(left: Float = 0.0f, top: Float = 0.0f, right: Float = 0.0f, bottom: Float = 0.0f) {
        onUpdate()

        selectedCandidate.rect.left += left
        selectedCandidate.rect.top += top
        selectedCandidate.rect.right += right
        selectedCandidate.rect.bottom += bottom

        validateRect(selectedCandidate.rect)
    }

    private fun deleteRegion() {
        onUpdate()

        annotationList.regions.remove(selectedCandidate)

        selectedIndex--

        if (selectedIndex < 0) {
            selectedCandidate = NOT_SELECTED
            return
        }

        selectedCandidate = annotationList.regions[selectedIndex]
    }

    private fun addRect(rect: Region.Rect) {
        onUpdate()

        selectedCandidate = Region(1.0, settings.defaultLabelNumber, rect)
        annotationList.regions.add(selectedCandidate)

        Collections.sort(annotationList.regions, PositionComparator())

        selectedIndex = annotationList.regions.indexOf(selectedCandidate)
        validateRect(selectedCandidate.rect)
    }

    fun reset(reverse: Boolean = false) {
        onUpdate()

        annotationList.regions.clear()

        Collections.sort(annotationList.regions, PositionComparator())

        if (annotationList.regions.isEmpty()) {
            selectedIndex = -1
            selectedCandidate = NOT_SELECTED
            return
        }

        selectedIndex = if (!reverse) 0 else (this.annotationList.regions.size - 1)
        selectedCandidate = this.annotationList.regions[selectedIndex]
        isUpdated = false
    }
}
