package io.keiji.region_cropper

import io.keiji.region_cropper.entity.CandidateList
import io.keiji.region_cropper.view.EditView
import javafx.beans.value.ObservableValue
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Stage
import tornadofx.*
import java.io.File

val filePath = "/Users/keiji_ariyama/Desktop/megane/twitter/1/_namori_"

class Main : App() {
    var index: Int = 0
    var fileList: List<String>? = null
    var baseDir: File? = null

    val canvas: EditView

    init {
        canvas = EditView()
    }

    override fun start(stage: Stage) {
        stage.title = "Drawing Operations Test"

        val root = Group()
        canvas.setFocusTraversable(true);
        canvas.requestFocus()
        canvas.addEventFilter(KeyEvent.KEY_PRESSED, { event ->
            run {

                val shiftValue = if (event.isShiftDown) 5.0f else 1.0f

                when {
                    event.isShiftDown && event.code == KeyCode.ENTER -> {
                        if (!canvas.selectPrevRegion()) {
                            prevPicture(reverse = true)
                        }
                    }
                    event.isShiftDown && event.code == KeyCode.D -> canvas.deleteRegion()
                    event.isShortcutDown && event.code == KeyCode.LEFT -> canvas.expandToRight(-shiftValue)
                    event.isShortcutDown && event.code == KeyCode.UP -> canvas.expandToBottom(-shiftValue)
                    event.isShortcutDown && event.code == KeyCode.RIGHT -> canvas.expandToLeft(-shiftValue)
                    event.isShortcutDown && event.code == KeyCode.DOWN -> canvas.expandToTop(-shiftValue)
                    event.isAltDown && event.code == KeyCode.LEFT -> canvas.expandToLeft(shiftValue)
                    event.isAltDown && event.code == KeyCode.UP -> canvas.expandToTop(shiftValue)
                    event.isAltDown && event.code == KeyCode.RIGHT -> canvas.expandToRight(shiftValue)
                    event.isAltDown && event.code == KeyCode.DOWN -> canvas.expandToBottom(shiftValue)
                    !event.isShiftDown && event.code == KeyCode.ENTER -> {
                        if (!canvas.selectNextRegion()) {
                            nextPicture()
                        }
                    }
                    event.code == KeyCode.LEFT -> canvas.moveToLeft(shiftValue)
                    event.code == KeyCode.UP -> canvas.moveToTop(shiftValue)
                    event.code == KeyCode.RIGHT -> canvas.moveToRight(shiftValue)
                    event.code == KeyCode.DOWN -> canvas.moveToBottom(shiftValue)
                    event.code == KeyCode.N -> canvas.toggleFace()
                    event.code == KeyCode.END -> nextPicture()
                    event.code == KeyCode.HOME -> prevPicture()
                }
                canvas.draw()
            }
        })

        root.children.add(canvas)
        val scene = Scene(root)

        stage.widthProperty().addListener(
                {
                    observableValue: ObservableValue<out Number>, oldValue: Number, newValue: Number ->
                    run {
                        canvas.width = newValue.toDouble()
                        canvas.draw()
                    }
                })
        stage.heightProperty().addListener(
                {
                    observableValue: ObservableValue<out Number>, oldValue: Number, newValue: Number ->
                    run {
                        canvas.height = newValue.toDouble()
                        canvas.draw()
                    }
                })

        stage.setScene(scene)
        stage.show()

        var file = File(filePath)
        baseDir = if (file.isDirectory) file else file.parentFile
        fileList = baseDir!!.list().filter(
                {
                    it.endsWith(".json")
                })

        if (fileList == null) {
            return
        }

        if (file.isDirectory) {
            file = File(baseDir, fileList!![index])
        } else {
            index = fileList!!.indexOf(file.name)
            print(index)
        }

        show(canvas, file)
    }

    private fun prevPicture(reverse: Boolean = false) {
        canvas.save(jsonFile)

        if (index == 0) {
            return
        }

        index--
        val previousFile: File = File(baseDir!!, fileList!![index])
        show(canvas, previousFile, reverse)
    }

    private fun nextPicture(reverse: Boolean = false) {
        canvas.save(jsonFile)

        if (index == fileList!!.size - 1) {
            return
        }

        index++
        val nextFile: File = File(baseDir!!, fileList!![index])
        show(canvas, nextFile, reverse)
    }

    var jsonFile: File? = null

    private fun show(canvas: EditView, file: File, reverse: Boolean = false) {
        jsonFile = file
        val candidateList = CandidateList.getInstance(file.absolutePath)
        val imageFile: File = File(file.parent, candidateList.fileName)
        val imageData: Image = Image(imageFile.toURI().toString())

        canvas.setData(imageData, candidateList, reverse)
    }
}