package io.keiji.region_cropper

import io.keiji.region_cropper.entity.CandidateList
import io.keiji.region_cropper.view.EditView
import javafx.application.Application
import javafx.beans.value.ObservableValue
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Stage
import tornadofx.*
import java.io.File

class Main : App() {
    var index: Int = 0
    private lateinit var fileList: List<String>
    private lateinit var baseDir: File

    val editView: EditView = EditView()

    fun initialize(filePath: String) {
        var file = File(filePath)
        baseDir = if (file.isDirectory) file else file.parentFile
        fileList = baseDir.list().filter(
                {
                    it.endsWith(".json")
                })

        if (file.isDirectory) {
            file = File(baseDir, fileList[index])
        } else {
            index = fileList.indexOf(file.name)
            print(index)
        }

        loadFile(file)
    }

    override fun start(stage: Stage) {

        val parameters: Application.Parameters = parameters
        val filePath = parameters.unnamed[0]
        initialize(filePath)

        stage.title = "Drawing Operations Test"

        val root = Group()
        editView.setFocusTraversable(true);
        editView.requestFocus()
        editView.addEventFilter(KeyEvent.KEY_PRESSED, { event ->
            run {

                val shiftValue = if (event.isShiftDown) 1.0f else 5.0f

                when {
                    event.isShortcutDown -> editView.mode = EditView.Mode.Shrink
                    event.isAltDown -> editView.mode = EditView.Mode.Expand
                }

                when {
                    event.isShiftDown && event.code == KeyCode.ENTER -> {
                        if (!editView.selectPrevRegion()) {
                            prevPicture(reverse = true)
                        }
                    }
                    event.code == KeyCode.ENTER -> {
                        if (!editView.selectNextRegion()) {
                            nextPicture()
                        }
                    }
                    event.isShiftDown && event.code == KeyCode.TAB -> {
                        editView.selectPrevRegion()
                    }
                    event.code == KeyCode.TAB -> {
                        editView.selectNextRegion()
                    }
                    event.isShiftDown && event.code == KeyCode.D -> editView.deleteRegion()
                    event.isShortcutDown && event.code == KeyCode.LEFT -> editView.expandToRight(-shiftValue)
                    event.isShortcutDown && event.code == KeyCode.UP -> editView.expandToBottom(-shiftValue)
                    event.isShortcutDown && event.code == KeyCode.RIGHT -> editView.expandToLeft(-shiftValue)
                    event.isShortcutDown && event.code == KeyCode.DOWN -> editView.expandToTop(-shiftValue)
                    event.isAltDown && event.code == KeyCode.LEFT -> editView.expandToLeft(shiftValue)
                    event.isAltDown && event.code == KeyCode.UP -> editView.expandToTop(shiftValue)
                    event.isAltDown && event.code == KeyCode.RIGHT -> editView.expandToRight(shiftValue)
                    event.isAltDown && event.code == KeyCode.DOWN -> editView.expandToBottom(shiftValue)
                    event.code == KeyCode.LEFT -> editView.moveToLeft(shiftValue)
                    event.code == KeyCode.UP -> editView.moveToTop(shiftValue)
                    event.code == KeyCode.RIGHT -> editView.moveToRight(shiftValue)
                    event.code == KeyCode.DOWN -> editView.moveToBottom(shiftValue)
                    event.code == KeyCode.N -> editView.toggleFace()
                    event.code == KeyCode.END -> nextPicture()
                    event.code == KeyCode.HOME -> prevPicture()
                }
                editView.draw()
            }
        })
        editView.addEventFilter(KeyEvent.KEY_RELEASED, { event ->
            run {
                when {
                    event.isShortcutDown -> editView.mode = EditView.Mode.Shrink
                    event.isAltDown -> editView.mode = EditView.Mode.Expand
                    else -> editView.mode = EditView.Mode.Normal

                }
                editView.draw()
            }
        })

        root.children.add(editView)
        val scene = Scene(root, 1024.0, 768.0)

        stage.widthProperty().addListener(
                {
                    observableValue: ObservableValue<out Number>, oldValue: Number, newValue: Number ->
                    run {
                        editView.width = newValue.toDouble()
                        editView.onResize()
                    }
                })
        stage.heightProperty().addListener(
                {
                    observableValue: ObservableValue<out Number>, oldValue: Number, newValue: Number ->
                    run {
                        editView.height = newValue.toDouble()
                        editView.onResize()
                    }
                })

        stage.setScene(scene)
        stage.show()
    }

    private fun prevPicture(reverse: Boolean = false) {
        editView.save(jsonFile)

        if (index == 0) {
            return
        }

        index--
        val previousFile: File = File(baseDir, fileList[index])
        loadFile(previousFile, reverse)
    }

    private fun nextPicture(reverse: Boolean = false) {
        editView.save(jsonFile)

        if (index == fileList.size - 1) {
            return
        }

        index++
        val nextFile: File = File(baseDir, fileList[index])
        loadFile(nextFile, reverse)
    }

    var jsonFile: File? = null

    private fun loadFile(file: File, reverse: Boolean = false) {
        jsonFile = file
        val candidateList = CandidateList.getInstance(file.absolutePath)
        val imageFile: File = File(file.parent, candidateList.fileName)
        val imageData: Image = Image(imageFile.toURI().toString())

        editView.setData(imageData, candidateList, reverse)
    }
}
