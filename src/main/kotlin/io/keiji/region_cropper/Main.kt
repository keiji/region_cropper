package io.keiji.region_cropper

import io.keiji.region_cropper.entity.CandidateList
import io.keiji.region_cropper.view.EditView
import javafx.application.Application
import javafx.beans.value.ObservableValue
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.image.Image
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import tornadofx.App
import java.io.File

class Main : App() {
    var file_index: Int = 0
    private lateinit var stage: Stage
    private lateinit var fileList: List<String>
    private lateinit var baseDir: File
    private lateinit var jsonFile: File

    private val editViewCallback = object : EditView.Callback {
        override fun onNextFile(reverse: Boolean) {
            nextPicture(reverse)
        }

        override fun onPreviousFile(reverse: Boolean) {
            prevPicture(reverse)
        }

        override fun onShowResetConfirmationDialog() {
            showResetDialog()
        }
    }

    val editView: EditView = EditView(editViewCallback)

    fun initialize(file: File) {
        var filePath: File = file
        baseDir = if (filePath.isDirectory) filePath else filePath.parentFile
        fileList = baseDir.list().filter(
                {
                    it.endsWith(".json")
                })

        if (filePath.isDirectory) {
            filePath = File(baseDir, fileList[file_index])
        } else {
            file_index = fileList.indexOf(filePath.name)
            print(file_index)
        }

        loadFile(filePath)
    }

    override fun start(stage: Stage) {
        this.stage = stage

        val parameters: Application.Parameters = parameters

        val filePath: File?
        if (parameters.unnamed.size > 0) {
            filePath = File(parameters.unnamed[0])
        } else {
            val chooser = DirectoryChooser()
            chooser.let {
                it.setTitle("Select Directory")
            }
            filePath = chooser.showDialog(stage)
        }

        initialize(filePath!!)

        val root = Group()
        editView.setFocusTraversable(true)
        editView.requestFocus()

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

    private fun showResetDialog() {
        Alert(Alert.AlertType.CONFIRMATION).let {
            it.setContentText("編集内容をリセットしてよろしいですか？")
            it.setHeaderText(null)

            val okButton: Button = it.dialogPane.lookupButton(ButtonType.OK) as Button
            okButton.isDefaultButton = false

            val cancelButton: Button = it.dialogPane.lookupButton(ButtonType.CANCEL) as Button
            cancelButton.isDefaultButton = true

            if (it.showAndWait().get() === ButtonType.OK) {
                editView.reset()
            }
        }
    }

    private fun prevPicture(reverse: Boolean = false) {
        editView.save(jsonFile)

        if (file_index == 0) {
            return
        }

        file_index--
        val previousFile: File = File(baseDir, fileList[file_index])
        loadFile(previousFile, reverse)
    }

    private fun nextPicture(reverse: Boolean = false) {
        editView.save(jsonFile)

        if (file_index == fileList.size - 1) {
            return
        }

        file_index++
        val nextFile: File = File(baseDir, fileList[file_index])
        loadFile(nextFile, reverse)
    }

    private fun loadFile(file: File, reverse: Boolean = false) {
        jsonFile = file
        val candidateList = CandidateList.getInstance(file.absolutePath)
        val imageFile: File = File(file.parent, candidateList.fileName)
        val imageData: Image = Image(imageFile.toURI().toString())

        editView.setData(imageData, candidateList, reverse)

        stage.title = String.format("%s - %d/%d", imageFile.name, (file_index + 1), fileList.size)
    }
}
