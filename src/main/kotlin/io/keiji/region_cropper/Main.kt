package io.keiji.region_cropper

import io.keiji.region_cropper.entity.CandidateList
import io.keiji.region_cropper.view.EditView
import javafx.application.Application
import javafx.beans.value.ObservableValue
import javafx.embed.swing.SwingFXUtils
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import tornadofx.App
import java.io.File
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO

const val WINDOW_WIDTH = 1024.0
const val WINDOW_HEIGHT = 768.0

const val LICENSE_FILE_NAME = "licenses.txt"

class Main : App() {

    private lateinit var stage: Stage
    private lateinit var fileList: List<String>
    private var fileIndex: Int = 0

    private lateinit var fileName: String
    private lateinit var baseDir: File
    private lateinit var jsonFile: File

    private lateinit var imageData: Image
    private lateinit var candidateList: CandidateList

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
            filePath = File(baseDir, fileList[fileIndex])
        } else {
            fileIndex = fileList.indexOf(filePath.name)
            print(fileIndex)
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

        setResizeListeners(root, menuBar)

        val scene = Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT)
        stage.setScene(scene)
        stage.show()

        root.center = editView
        editView.let {
            it.height = root.height - menuBar.height
            it.width = root.width
            it.onResize()
            it.setFocusTraversable(true)
            it.requestFocus()
        }

    }

    private fun setResizeListeners(root: BorderPane, menuBar: MenuBar) {
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
                        editView.height = root.height - menuBar.height
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

        // Crop
        menuBar.menus[1].let {
            // Crop Immediately
            it.items[0].onAction = EventHandler<ActionEvent> {
                cropTo(baseDir)
            }

            // Crop to
            it.items[1].onAction = EventHandler<ActionEvent> {
                val filePath = showDirectoryChooser()
                if (filePath != null) {
                    cropTo(filePath)
                }
            }
        }

        // Help
        menuBar.menus[2].let {

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

    private fun cropTo(path: File) {
        if (candidateList.regions == null) {
            return
        }

        candidateList.save(jsonFile)

        val filePath: File
        if (path.isDirectory) {
            filePath = path
        } else {
            filePath = path.parentFile
        }

        var index: Int = 0
        for (region: CandidateList.Region in candidateList.regions!!) {
            val rect = region.rect
            val writableImage = WritableImage(imageData.pixelReader,
                    rect.left.toInt(), rect.top.toInt(),
                    rect.width().toInt(), rect.height().toInt())
            val file = File(filePath, String.format("%s-%d.png", fileName, index))
            ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", file);
            index++
        }

        showCropCompleteDialog(path)
    }

    private fun showLicensesDialog() {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = "Open Source Licenses"
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

    private fun showCropCompleteDialog(outputFilePath: File) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = "Crop Complete"
        alert.headerText = String.format("Saved pictures to %s", outputFilePath.absolutePath)

        alert.showAndWait()
    }

    private fun showAboutDialog() {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = "About"
        alert.headerText =
                "RegionCropper (Megane Co)"
        alert.contentText =
                "Copyright 2016 Keiji Ariyama\n" +
                        "GitHub: https://github.com/keiji\n" +
                        "Web: https://blog.keiji.io"

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
            it.setContentText("Discard changes?")
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

        if (fileIndex == 0) {
            return
        }

        fileIndex--
        val previousFile: File = File(baseDir, fileList[fileIndex])
        loadFile(previousFile, reverse)
    }

    private fun nextPicture(reverse: Boolean = false) {
        editView.save(jsonFile)

        if (fileIndex == fileList.size - 1) {
            return
        }

        fileIndex++
        val nextFile: File = File(baseDir, fileList[fileIndex])
        loadFile(nextFile, reverse)
    }

    private fun loadFile(imageFile: File, reverse: Boolean = false) {
        val nameAndExtension: List<String> = imageFile.name.split(".")
        fileName = nameAndExtension[0]

        jsonFile = File(baseDir, String.format("%s.json", fileName))
        if (jsonFile.exists()) {
            candidateList = CandidateList.getInstance(jsonFile.absolutePath)
        } else {
            candidateList = CandidateList("Region Cropper", imageFile.name, null, ArrayList<CandidateList.Region>(), createdAt())
        }

        imageData = Image(imageFile.toURI().toString())

        editView.setData(imageData, candidateList, reverse)

        stage.title = String.format("%s - %d/%d", imageFile.name, (fileIndex + 1), fileList.size)
    }

    private fun createdAt(): String {
        val tz = TimeZone.getTimeZone("UTC")
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S")
        df.setTimeZone(tz)
        return df.format(Date())
    }
}
