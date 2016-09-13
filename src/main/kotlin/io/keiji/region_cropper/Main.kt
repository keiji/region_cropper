package io.keiji.region_cropper

import io.keiji.region_cropper.entity.CandidateList
import io.keiji.region_cropper.view.EditView
import javafx.application.Application
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import tornadofx.*
import java.io.File
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

class Main : App() {

    val LICENSE_FILE_NAME = "licenses.txt"

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
                    it.toLowerCase().endsWith(".png")
                            || it.toLowerCase().endsWith(".jpg")
                            || it.toLowerCase().endsWith(".jpeg")
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
            filePath = showDirectoryChooser()
        }

        if (filePath == null) {
            stage.close()
            return
        }

        initialize(file = filePath)

        val root: BorderPane = FXMLLoader.load(javaClass.classLoader.getResource("main.fxml"));
        val menuBar: MenuBar = root.lookup("#menuBar") as MenuBar

        initMenu(menuBar, stage)

        root.center = editView

        editView.paddingTop = menuBar.height
        editView.setFocusTraversable(true)
        editView.requestFocus()

        setResizeListeners(root)

        val scene = Scene(root, 1024.0, 768.0)
        stage.setScene(scene)
        stage.show()
    }

    private fun setResizeListeners(root: BorderPane) {
        /*
         * canvasのリサイズを検知できない課題がある
         * また、VBoxなどを設置した場合はリサイズ時に縮小イベントをキャッチしない課題がある
         */
        root.widthProperty().addListener(
                {
                    observableValue: ObservableValue<out Number>, oldValue: Number, newValue: Number ->
                    run {
                        editView.width = root.width
                        editView.onResize()
                    }
                })
        root.heightProperty().addListener(
                {
                    observableValue: ObservableValue<out Number>, oldValue: Number, newValue: Number ->
                    run {
                        editView.height = root.height
                        editView.onResize()
                    }
                })
    }

    private fun initMenu(menuBar: MenuBar, stage: Stage) {
        // File
        menuBar.menus[0].let {
            // Open Directory
            it.items[0].onAction = EventHandler<ActionEvent> {
                val filePath = showDirectoryChooser()
                if (filePath != null) {
                    initialize(filePath)
                }
            }

            // Separator

            // Quit
            it.items[2].onAction = EventHandler<ActionEvent> {
                stage.close()
            }
        }

        // Help
        menuBar.menus[1].let {

            // Licenses
            it.items[0].onAction = EventHandler<ActionEvent> {
                showLicensesDialog()
            }

            // Separator

            // About
            it.items[2].onAction = EventHandler<ActionEvent> {
                showAboutDialog()
            }
        }
    }

    private fun showLicensesDialog() {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = "オープンソースライセンス"
        alert.headerText = null
        alert.contentText = null

        val text: String = javaClass.classLoader.getResourceAsStream(LICENSE_FILE_NAME)
                .reader(Charset.forName("UTF-8"))
                .readText()

        val textArea = TextArea(text)
        textArea.isEditable = false
        textArea.isWrapText = true

        textArea.maxWidth = java.lang.Double.MAX_VALUE
        textArea.maxHeight = java.lang.Double.MAX_VALUE
        GridPane.setVgrow(textArea, Priority.ALWAYS)
        GridPane.setHgrow(textArea, Priority.ALWAYS)

        val expContent = GridPane()
        expContent.setMaxWidth(java.lang.Double.MAX_VALUE)
        expContent.add(textArea, 0, 0)

        alert.dialogPane.content = expContent

        alert.show()
    }

    private fun showAboutDialog() {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = "About"
        alert.headerText = null
        alert.contentText = "Megane Co - RegionCropper\n" +
                "Copyright Keiji Ariyama 2016"

        alert.showAndWait()
    }

    private fun showDirectoryChooser(): File? {
        val chooser = DirectoryChooser()
        chooser.let {
            it.setTitle("Select Directory")
        }
        return chooser.showDialog(stage)
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

    private fun loadFile(imageFile: File, reverse: Boolean = false) {
        val nameAndExtension: List<String> = imageFile.name.split(".")
        val name = nameAndExtension[0]

        jsonFile = File(baseDir, String.format("%s.json", name))
        val candidateList: CandidateList
        if (jsonFile.exists()) {
            candidateList = CandidateList.getInstance(jsonFile.absolutePath)
        } else {
            candidateList = CandidateList("Region Cropper", imageFile.name, null, ArrayList<CandidateList.Region>(), createdAt())
        }

        val imageData: Image = Image(imageFile.toURI().toString())

        editView.setData(imageData, candidateList, reverse)

        stage.title = String.format("%s - %d/%d", imageFile.name, (file_index + 1), fileList.size)
    }

    private fun createdAt(): String {
        val tz = TimeZone.getTimeZone("UTC")
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S")
        df.setTimeZone(tz)
        return df.format(Date())
    }
}
