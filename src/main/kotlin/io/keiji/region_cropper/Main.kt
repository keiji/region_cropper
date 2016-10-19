package io.keiji.region_cropper

import io.keiji.region_cropper.entity.CandidateList
import io.keiji.region_cropper.entity.Region
import io.keiji.region_cropper.entity.RegionList
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
import org.controlsfx.control.StatusBar
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

    private lateinit var imageFileList: List<File>
    private var imageFileIndex: Int = 0

    private val imageFile: File
        get() {
            return imageFileList[imageFileIndex]
        }

    private lateinit var candidateFileList: List<File>
    private var candidateFileIndex: Int = 0

    private val candidateFile: File
        get() {
            return candidateFileList[candidateFileIndex]
        }

    private lateinit var resultJsonFile: File

    private lateinit var imageData: Image
    private var candidateList: CandidateList? = null

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
                initialize(findAllFiles(path.listFiles().toList()))
            }
        }

        override fun close() {
            stage.close()
        }

        override fun undo() {
            editView.restoreState()
        }

        override fun quickCrop() {
            cropTo(imageFile.parentFile)
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
            nextPicture(reverse = reverse)
        }

        override fun onPreviousFile(reverse: Boolean) {
            prevPicture(reverse = reverse)
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
        dropImage = Image(javaClass.classLoader.getResourceAsStream("drop_image_here.png"))

        val settingPath: File = File(SETTING_FILE_PATH)
        if (!settingPath.exists()) {
            settings = generateDefaultSettings()
            settings.save(settingPath)
        } else {
            settings = Settings.getInstance(SETTING_FILE_PATH)
        }
    }

    fun isImage(name: String): Boolean {
        val lowerName = name.toLowerCase()
        return lowerName.endsWith(".png")
                || lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".gif")
    }

    fun initialize(file: File) {
        imageFileList = file.parentFile.listFiles().filter {
            isImage(it.name)
        }

        Collections.sort(imageFileList, { a, b ->
            a.absolutePath.toLowerCase().compareTo(b.absolutePath.toLowerCase())
        })

        imageFileIndex = if (file.isDirectory) 0 else imageFileList.indexOf(file)

        loadFile(imageFileList[imageFileIndex])
    }

    fun initialize(files: List<File>) {
        imageFileList = files.filter {
            isImage(it.name)
        }

        Collections.sort(imageFileList, { a, b ->
            a.absolutePath.toLowerCase().compareTo(b.absolutePath.toLowerCase())
        })

        imageFileIndex = 0
        loadFile(imageFileList[imageFileIndex])
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
    private lateinit var statusBar: StatusBar

    override fun start(stage: Stage) {
        this.stage = stage

        stage.addEventFilter(KeyEvent.KEY_PRESSED, { event ->
            run {

                when {
                    event.code == KeyCode.ESCAPE -> onReset(event.isControlDown)
                    event.isShortcutDown && event.code == KeyCode.S -> save()
                    event.isShortcutDown && event.code == KeyCode.W -> stage.close()
                    event.isShiftDown && event.code == KeyCode.HOME -> prevPicture(index = 10)
                    event.isShiftDown && event.code == KeyCode.END -> nextPicture(index = 10)
                    event.code == KeyCode.HOME -> prevPicture()
                    event.code == KeyCode.END -> nextPicture()
                    event.isShiftDown && event.code == KeyCode.PAGE_UP -> prevCandidate(index = 10)
                    event.isShiftDown && event.code == KeyCode.PAGE_DOWN -> nextCandidate(index = 10)
                    event.code == KeyCode.PAGE_UP -> prevCandidate()
                    event.code == KeyCode.PAGE_DOWN -> nextCandidate()
                }
            }
        })

        borderPane = FXMLLoader.load(javaClass.classLoader.getResource("main.fxml"))

        val dropView: ImageView = borderPane.lookup("#drop_view") as ImageView
        dropView.image = dropImage

        menuBar = borderPane.lookup("#menuBar") as MenuBar
        statusBar = borderPane.lookup("#statusBar") as StatusBar

        menuController = MainMenuController(menuBar, menuCallback)

        setResizeListeners(borderPane, menuBar)

        val scene = Scene(borderPane, WINDOW_WIDTH, WINDOW_HEIGHT)

        initDragAndDrop(scene)

        statusBar.text = ""
        stage.title = "Region Cropper"
        stage.setScene(scene)
        stage.show()
    }

    private fun save() {
        if (!editView.isUpdated) {
            return
        }
        editView.regionList.save(resultJsonFile)
    }

    private fun nextCandidate(index: Int = 1) {
        if (candidateList === null) {
            return
        }

        candidateFileIndex += index

        if (candidateFileIndex > candidateFileList.size - 1) {
            candidateFileIndex = candidateFileList.size - 1
        }

        candidateList = CandidateList.getInstance(candidateFile.absolutePath)
        editView.candidateList = candidateList

        editView.redraw()

        updateStateusBarText()
    }

    private fun prevCandidate(index: Int = 1) {
        if (candidateList === null) {
            return
        }

        candidateFileIndex -= index

        if (candidateFileIndex < 0) {
            candidateFileIndex = 0
        }

        candidateList = CandidateList.getInstance(candidateFile.absolutePath)
        editView.candidateList = candidateList

        editView.redraw()

        updateStateusBarText()
    }

    private fun showEditView() {
        if (editViewInitialized) {
            return
        }

        editView = EditView(editViewCallback, settings)

        borderPane.center = editView
        editView.let {
            it.settings = settings
            it.height = getEditViewHeight()
            it.width = borderPane.width
            it.requestFocus()

            // 方向キーでフォーカスが外れてしまう不具合のワークアラウンド
            it.focusedProperty().addListener { observableValue, oldProperty, newProperty ->
                run {
                    if (!newProperty) {
                        it.requestFocus()
                    }
                }
            }
        }

        editViewInitialized = true
    }

    private fun getEditViewHeight() = borderPane.height - menuBar.height - statusBar.height

    private fun initDragAndDrop(scene: Scene) {
        scene.onDragOver = object : EventHandler<DragEvent> {
            override fun handle(event: DragEvent) {
                val db = event.getDragboard()
                if (db.hasFiles()) {
                    val files = db.files.filter {
                        it.isDirectory || isImage(it.name)
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
                    val imageFiles = findAllFiles(db.files).filter {
                        isImage(it.name)
                    }

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
                            editView.height = getEditViewHeight()
                            editView.onResize()
                        }
                    }
                })
    }

    fun onReset(isControlDown: Boolean) {
        if (!editViewInitialized) {
            return
        }

        if (isControlDown) {
            showMergeDialog()
        } else {
            showResetDialog()
        }
    }

    private fun cropTo(path: File) {
        if (editView.regionList.regions == null) {
            return
        }

        save()

        val filePath: File
        if (path.isDirectory) {
            filePath = path
        } else {
            filePath = path.parentFile
        }

        var index: Int = 0
        for (region: Region in editView.regionList.regions!!) {
            val labelSetting: Settings.Label = settings.labelSettings[region.label]

            val labelName: String = labelSetting.name ?: String.format("label_%d", labelSetting.number)
            val filePathWithLabel = File(filePath, labelName)
            filePathWithLabel.mkdirs()

            val rect = region.rect
            val writableImage = WritableImage(imageData.pixelReader,
                    rect.left.toInt(), rect.top.toInt(),
                    rect.width().toInt(), rect.height().toInt())
            val file = File(filePathWithLabel, String.format("%s-%d.png", imageFile, index))
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
            it.extensionFilters.add(FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif"))
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

    private fun showMergeDialog() {
        if (candidateList === null) {
            return

        }
        Alert(Alert.AlertType.CONFIRMATION).let {
            val message: String = "Merge latest candidates?"

            it.setContentText(message)
            it.setHeaderText(null)

            val okButton: Button = it.dialogPane.lookupButton(ButtonType.OK) as Button
            okButton.isDefaultButton = false

            val cancelButton: Button = it.dialogPane.lookupButton(ButtonType.CANCEL) as Button
            cancelButton.isDefaultButton = true

            if (it.showAndWait().get() === ButtonType.OK) {
                editView.merge()
            }
        }
    }

    private fun showResetDialog() {
        Alert(Alert.AlertType.CONFIRMATION).let {
            val message: String = if (candidateList === null) "Clear regions?" else "Discard changes?"

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

    private fun prevPicture(index: Int = 1, reverse: Boolean = false) {
        save()

        if (imageFileIndex == 0) {
            return
        }

        imageFileIndex -= index
        imageFileIndex = if (imageFileIndex < 0) 0 else imageFileIndex

        loadFile(imageFileList[imageFileIndex], reverse)
    }

    private fun nextPicture(index: Int = 1, reverse: Boolean = false) {
        save()

        val limit = imageFileList.size - 1
        if (imageFileIndex == limit) {
            return
        }

        imageFileIndex += index
        imageFileIndex = if (imageFileIndex > limit) limit else imageFileIndex

        loadFile(imageFileList[imageFileIndex], reverse)
    }

    private fun loadFile(imageFile: File, reverse: Boolean = false) {
        val nameAndExtension: List<String> = imageFile.name.split(".")
        val fileName = nameAndExtension[0]
        val dir = imageFile.parentFile

        val regionList: RegionList
        resultJsonFile = File(dir, String.format("%s.json", fileName))
        if (resultJsonFile.exists()) {
            regionList = RegionList.getInstance(resultJsonFile.absolutePath)
        } else {
            regionList = RegionList("Region Cropper", imageFile.name, null, createdAt())
        }

        candidateFileList = dir.listFiles().toList().filter { f -> f.name.startsWith(String.format("%s-candidate", fileName)) && f.name.endsWith(".json") }
        Collections.sort(candidateFileList, Collections.reverseOrder())
        candidateFileIndex = 0

        if (candidateFileList.size > 0) {
            candidateList = CandidateList.getInstance(candidateFile.absolutePath)
        } else {
            candidateList = null
        }

        imageData = Image(imageFile.toURI().toString())

        showEditView()

        editView.setData(imageData, regionList, candidateList, reverse)

        updateStateusBarText()
    }

    private fun updateStateusBarText() {
        stage.title = String.format("%s (%,3d/%,3d)",
                imageFile.name, (imageFileIndex + 1), imageFileList.size)

        if (candidateList !== null) {
            statusBar.text = String.format("Candidates: %s (%,3d/%,3d)",
                    candidateFile.name, (candidateFileIndex + 1), candidateFileList.size)
        } else {
            statusBar.text = ""
        }
    }

    private fun createdAt(): String {
        val tz = TimeZone.getTimeZone("UTC")
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S")
        df.setTimeZone(tz)
        return df.format(Date())
    }
}
