package io.keiji.region_cropper

import io.keiji.region_cropper.entity.CandidateList
import io.keiji.region_cropper.entity.Settings
import io.keiji.region_cropper.view.EditView
import javafx.beans.value.ObservableValue
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.input.DragEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import tornadofx.App
import java.io.File
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO

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

const val WINDOW_WIDTH = 1024.0
const val WINDOW_HEIGHT = 768.0

const val LICENSE_FILE_NAME = "licenses.txt"
const val SETTING_FILE_PATH = "./settings.json"

class Main : App() {

    private lateinit var stage: Stage

    private lateinit var fileList: List<File>
    private var fileIndex: Int = 0

    private val file: File
        get() {
            return fileList[fileIndex]
        }


    private lateinit var jsonFile: File

    private lateinit var imageData: Image
    private lateinit var candidateList: CandidateList

    private val menuCallback = object : MainMenuController.Callback {
        override fun openFile() {
            val filePath = showFileChooser()
            if (filePath != null) {
                initialize(filePath)
            }
        }

        override fun openDirectory() {
            val path = showDirectoryChooser()
            if (path != null) {
                initialize(path.listFiles().toList())
            }
        }

        override fun close() {
            stage.close()
        }

        override fun undo() {
            editView.restoreState()
        }

        override fun quickCrop() {
            cropTo(file.parentFile)
        }

        override fun cropTo() {
            val filePath = showFileChooser()
            if (filePath != null) {
                cropTo(filePath)
            }
        }

        override fun licenseDialog() {
            showLicensesDialog()
        }

        override fun aboutDialog() {
            showAboutDialog()
        }
    }

    private val editViewCallback = object : EditView.Callback {
        override fun onNextFile(reverse: Boolean) {
            nextPicture(reverse)
        }

        override fun onPreviousFile(reverse: Boolean) {
            prevPicture(reverse)
        }

        override fun onReset(isControlDown: Boolean) {
            if (isControlDown) {
                editView.reset()
                return
            }
            showResetDialog()
        }
    }

    private lateinit var settings: Settings
    private lateinit var editView: EditView
    private var editViewInitialized: Boolean = false

    private fun generateDefaultSettings(): Settings {
        val labelList: ArrayList<Settings.Label> = ArrayList<Settings.Label>()
        labelList.add(Settings.Label(0, null, false, false, "#000000"))
        labelList.add(Settings.Label(1, null, true, true, "#8FBC8F"))
        labelList.add(Settings.Label(2, null, true, true, "#0000FF"))
        labelList.add(Settings.Label(3, null, true, true, "#DEB887"))
        labelList.add(Settings.Label(4, null, true, true, "#F0FFFF"))
        labelList.add(Settings.Label(5, null, true, true, "#FFA500"))
        labelList.add(Settings.Label(6, null, true, true, "#FFC0CB"))
        labelList.add(Settings.Label(7, null, true, true, "#7FFFD4"))
        labelList.add(Settings.Label(8, null, true, true, "#6B8E23"))
        labelList.add(Settings.Label(9, null, true, true, "#F08080"))
        return Settings(1, "#ff0000", "#ffff00", labelList)
    }

    private lateinit var dropImage: Image

    override fun init() {
        dropImage = Image(javaClass.classLoader.getResourceAsStream("ic_image_black_48dp.png"))

        val settingPath: File = File(SETTING_FILE_PATH)
        if (!settingPath.exists()) {
            settings = generateDefaultSettings()
            settings.save(settingPath)
        } else {
            settings = Settings.getInstance(SETTING_FILE_PATH)
        }
    }

    fun initialize(file: File) {
        fileList = file.parentFile.listFiles().filter(
                {
                    it.name.toLowerCase().endsWith(".png")
                            || it.name.toLowerCase().endsWith(".jpg")
                            || it.name.toLowerCase().endsWith(".jpeg")
                })

        Collections.sort(fileList, { a, b ->
            a.absolutePath.toLowerCase().compareTo(b.absolutePath.toLowerCase())
        })

        fileIndex = if (file.isDirectory) 0 else fileList.indexOf(file)

        loadFile(fileList[fileIndex])
    }

    fun initialize(files: List<File>) {
        fileList = files

        Collections.sort(fileList, { a, b ->
            a.absolutePath.toLowerCase().compareTo(b.absolutePath.toLowerCase())
        })

        fileIndex = 0
        loadFile(fileList[fileIndex])
    }

    private lateinit var menuController: MainMenuController

    private fun findAllFiles(files: List<File>): List<File> {
        val resultFileList = ArrayList<File>()

        for (file in files) {
            if (file.isFile) {
                resultFileList.add(file)
            } else if (file.isDirectory) {
                resultFileList.addAll(findAllFiles(file.listFiles().toList()))
            }
        }

        return resultFileList
    }

    private lateinit var borderPane: BorderPane
    private lateinit var menuBar: MenuBar

    override fun start(stage: Stage) {
        this.stage = stage

        stage.addEventFilter(KeyEvent.KEY_PRESSED, { event ->
            run {
                when {
                    event.isShortcutDown && event.code == KeyCode.S -> candidateList.save(jsonFile)
                    event.isShortcutDown && event.code == KeyCode.W -> stage.close()
                }
            }
        })

        borderPane = FXMLLoader.load(javaClass.classLoader.getResource("main.fxml"))

        val dropView: ImageView = borderPane.lookup("#drop_view") as ImageView
        dropView.image = dropImage

        menuBar = borderPane.lookup("#menuBar") as MenuBar

        menuController = MainMenuController(menuBar, menuCallback)

        setResizeListeners(borderPane, menuBar)

        val scene = Scene(borderPane, WINDOW_WIDTH, WINDOW_HEIGHT)

        initDragAndDrop(scene)

        stage.title = "Region Cropper"
        stage.setScene(scene)
        stage.show()
    }

    private fun showEditView() {
        if (editViewInitialized) {
            return
        }

        editView = EditView(editViewCallback, settings)

        borderPane.center = editView
        editView.let {
            it.settings = settings
            it.height = borderPane.height - menuBar.height
            it.width = borderPane.width
            it.setFocusTraversable(true)
            it.requestFocus()
        }

        editViewInitialized = true
    }

    private fun initDragAndDrop(scene: Scene) {
        scene.onDragOver = object : EventHandler<DragEvent> {
            override fun handle(event: DragEvent) {
                val db = event.getDragboard()
                if (db.hasFiles()) {
                    val files = db.files.filter {
                        it.isDirectory
                                || it.name.toLowerCase().endsWith(".png")
                                || it.name.toLowerCase().endsWith(".jpg")
                                || it.name.toLowerCase().endsWith(".jpeg")
                    }

                    if (files.size > 0) {
                        event.acceptTransferModes(TransferMode.COPY)
                    }
                } else {
                    event.consume()
                }
            }
        }

        scene.onDragDropped = object : EventHandler<DragEvent> {
            override fun handle(event: DragEvent) {
                val db = event.getDragboard()
                var success = false
                if (db.hasFiles()) {
                    val imageFiles = findAllFiles(db.files).filter({
                        it.name.toLowerCase().endsWith(".png")
                                || it.name.toLowerCase().endsWith(".jpg")
                                || it.name.toLowerCase().endsWith(".jpeg")

                    })

                    if (imageFiles.size == 1) {
                        success = true
                        initialize(imageFiles[0])
                    } else if (imageFiles.size > 1) {
                        success = true
                        initialize(imageFiles)
                    }
                }
                event.setDropCompleted(success)
                event.consume()
            }
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
                        if (editViewInitialized) {
                            editView.width = root.width
                            editView.onResize()
                        }
                    }
                })
        root.heightProperty().addListener(
                {
                    observableValue: ObservableValue<out Number>, oldValue: Number, newValue: Number ->
                    run {
                        if (editViewInitialized) {
                            editView.height = root.height - menuBar.height
                            editView.onResize()
                        }
                    }
                })
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
            val labelSetting: Settings.Label = settings.labelSettings[region.label]

            val labelName: String = labelSetting.name ?: String.format("label_%d", labelSetting.number)
            val filePathWithLabel = File(filePath, labelName)
            filePathWithLabel.mkdirs()

            val rect = region.rect
            val writableImage = WritableImage(imageData.pixelReader,
                    rect.left.toInt(), rect.top.toInt(),
                    rect.width().toInt(), rect.height().toInt())
            val file = File(filePathWithLabel, String.format("%s-%d.png", file, index))
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

    private fun showFileChooser(): File? {
        val chooser = FileChooser()
        chooser.let {
            it.setTitle("Select Files")
            it.extensionFilters.add(FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg"))
        }
        return chooser.showOpenDialog(stage)
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
            val message: String
            if (candidateList.detectedFaces != null) {
                message = "Clear regions?"
            } else {
                message = "Discard changes?"
            }

            it.setContentText(message)
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
        candidateList.save(jsonFile)

        if (fileIndex == 0) {
            return
        }

        fileIndex--
        loadFile(fileList[fileIndex], reverse)
    }

    private fun nextPicture(reverse: Boolean = false) {
        candidateList.save(jsonFile)

        if (fileIndex == fileList.size - 1) {
            return
        }

        fileIndex++
        loadFile(fileList[fileIndex], reverse)
    }

    private fun loadFile(imageFile: File, reverse: Boolean = false) {
        val nameAndExtension: List<String> = imageFile.name.split(".")
        val fileName = nameAndExtension[0]

        jsonFile = File(file.parentFile, String.format("%s.json", fileName))
        if (jsonFile.exists()) {
            candidateList = CandidateList.getInstance(jsonFile.absolutePath)
        } else {
            candidateList = CandidateList("Region Cropper", imageFile.name, null, ArrayList<CandidateList.Region>(), createdAt())
        }

        imageData = Image(imageFile.toURI().toString())

        showEditView()

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
