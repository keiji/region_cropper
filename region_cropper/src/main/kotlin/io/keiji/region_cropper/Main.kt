package io.keiji.region_cropper

import io.keiji.region_cropper.entity.Region
import io.keiji.region_cropper.entity.AnnotationList
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

    private val imageFileList = ArrayList<File>()

    private var imageFileIndex: Int = 0

    private val imageFile: File
        get() {
            return imageFileList[imageFileIndex]
        }

    private lateinit var aanotationFile: File
    private lateinit var imageData: Image

    private val menuCallback = object : MainMenuController.Callback {
        override fun openFile() {
            showFileChooser()?.let {
                initialize(listOf(it))
            }
        }

        override fun openDirectory() {
            showDirectoryChooser()?.let {
                initialize(findAllFiles(it.listFiles().toList()))
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
            showFileChooser()?.let {
                cropTo(it)
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
        val labelList = ArrayList<Settings.Label>().also {
            it.add(Settings.Label(0, null, false, false, "#000000"))
            it.add(Settings.Label(1, null, true, true, "#8FBC8F"))
            it.add(Settings.Label(2, null, true, true, "#0000FF"))
            it.add(Settings.Label(3, null, true, true, "#DEB887"))
            it.add(Settings.Label(4, null, true, true, "#F0FFFF"))
            it.add(Settings.Label(5, null, true, true, "#FFA500"))
            it.add(Settings.Label(6, null, true, true, "#FFC0CB"))
            it.add(Settings.Label(7, null, true, true, "#7FFFD4"))
            it.add(Settings.Label(8, null, true, true, "#6B8E23"))
            it.add(Settings.Label(9, null, true, true, "#F08080"))
        }
        return Settings(1, "#ff0000", "#ffff00", labelList)
    }

    private val dropImage = Image(javaClass.classLoader.getResourceAsStream("drop_image_here.png"))

    override fun init() {
        val settingPath = File(SETTING_FILE_PATH)
        if (!settingPath.exists()) {
            settings = generateDefaultSettings()
            settings.save(settingPath)
        }

        settings = Settings.getInstance(SETTING_FILE_PATH)
    }

    fun isImage(name: String): Boolean {
        val lowerName = name.toLowerCase()
        return lowerName.endsWith(".png")
                || lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".gif")
    }

    fun initialize(files: List<File>) {
        imageFileList.let {
            it.clear()
            it.addAll(files.filter { isImage(it.name) })

            Collections.sort(it, { a, b ->
                a.absolutePath.toLowerCase().compareTo(b.absolutePath.toLowerCase())
            })
        }

        imageFileIndex = 0
        loadFile(imageFileList[imageFileIndex])
    }

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

    private val borderPane: BorderPane = FXMLLoader.load(javaClass.classLoader.getResource("main.fxml"))

    private val menuBar = borderPane.lookup("#menuBar") as MenuBar

    private val statusBar = (borderPane.lookup("#statusBar") as StatusBar).also {
        it.text = ""
    }
    private val dropView = (borderPane.lookup("#drop_view") as ImageView).also {
        it.image = dropImage
    }

    private val scene = Scene(borderPane, WINDOW_WIDTH, WINDOW_HEIGHT).also {
        initDragAndDrop(it)
    }

    val menuController = MainMenuController(menuBar, menuCallback)

    override fun start(stage: Stage) {
        this.stage = stage
        stage.title = "Region Cropper"

        stage.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            when {
                event.code == KeyCode.ESCAPE -> onReset(event.isControlDown)
                event.isShortcutDown && event.code == KeyCode.S -> save(force = true)
                event.isShortcutDown && event.code == KeyCode.W -> stage.close()
                event.isShiftDown && event.code == KeyCode.HOME -> prevPicture(offset = 10)
                event.isShiftDown && event.code == KeyCode.END -> nextPicture(offset = 10)
                event.code == KeyCode.HOME -> prevPicture()
                event.code == KeyCode.END -> nextPicture()
            }
        }

        setResizeListeners(borderPane, menuBar)

        stage.scene = scene
        stage.show()
    }

    private fun save(force: Boolean = false) {
        if (!editView.isUpdated && !force) {
            return
        }

        editView.annotationList.save(aanotationFile)
    }

    private fun initEditView() {
        if (editViewInitialized) {
            return
        }

        editView = EditView(editViewCallback, settings).also {
            it.settings = settings
            it.height = getEditViewHeight()
            it.width = borderPane.width
        }
        borderPane.center = editView

        editView.requestFocus()

        // 方向キーでフォーカスが外れてしまう不具合のワークアラウンド
        editView.focusedProperty().addListener { _, oldProperty, newProperty ->
            if (!newProperty) {
                editView.requestFocus()
            }
        }

        editViewInitialized = true
    }

    private fun getEditViewHeight() = borderPane.height - menuBar.height - statusBar.height

    private fun initDragAndDrop(scene: Scene) {
        scene.onDragOver = EventHandler<DragEvent> { event ->
            event.getDragboard().also { db ->
                if (db.hasFiles()) {
                    val files = db.files.filter { it.isDirectory || isImage(it.name) }

                    if (files.isNotEmpty()) {
                        event.acceptTransferModes(TransferMode.COPY)
                    }
                } else {
                    event.consume()
                }
            }
        }

        scene.onDragDropped = EventHandler<DragEvent> { event ->
            var success = false

            event.getDragboard().also { db ->
                if (db.hasFiles()) {
                    success = true

                    val imageFiles = findAllFiles(db.files).filter { isImage(it.name) }
                    initialize(imageFiles)
                }
            }

            event.setDropCompleted(success)
            event.consume()
        }
    }

    private fun setResizeListeners(root: BorderPane, menuBar: MenuBar) {
        /*
         * 課題
         * canvasのリサイズを検知できない
         * また、VBoxなどを設置した場合はリサイズ時に縮小イベントをキャッチしない
         */
        root.widthProperty().addListener { _: ObservableValue<out Number>, oldValue: Number, newValue: Number ->
            if (editViewInitialized) {
                editView.width = root.width
                editView.onResize()
            }
        }
        root.heightProperty().addListener { _: ObservableValue<out Number>, oldValue: Number, newValue: Number ->
            if (editViewInitialized) {
                editView.height = getEditViewHeight()
                editView.onResize()
            }
        }
    }

    fun onReset(isControlDown: Boolean) {
        if (!editViewInitialized) {
            return
        }

        showResetDialog()
    }

    private fun cropTo(path: File) {
        save()

        val filePath: File = if (path.isDirectory) path else path.parentFile

        var index = 0
        for (region: Region in editView.annotationList.regions) {
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
        val alert = Alert(Alert.AlertType.INFORMATION).also {
            it.title = "Open Source Licenses"
            it.headerText = null
            it.contentText = null
        }

        val text: String = javaClass.classLoader.getResourceAsStream(LICENSE_FILE_NAME)
                .reader(Charset.forName("UTF-8"))
                .readText()

        val textArea = TextArea(text).also {
            it.isEditable = false
            it.isWrapText = true
            it.maxWidth = java.lang.Double.MAX_VALUE
            it.maxHeight = java.lang.Double.MAX_VALUE
        }

        GridPane.setVgrow(textArea, Priority.ALWAYS)
        GridPane.setHgrow(textArea, Priority.ALWAYS)

        val expContent = GridPane().also {
            it.setMaxWidth(java.lang.Double.MAX_VALUE)
            it.add(textArea, 0, 0)
        }

        alert.dialogPane.content = expContent
        alert.show()
    }

    private fun showCropCompleteDialog(outputFilePath: File) {
        val alert = Alert(Alert.AlertType.INFORMATION).also {
            it.title = "Crop Complete"
            it.headerText = String.format("Saved pictures to %s", outputFilePath.absolutePath)
        }

        alert.showAndWait()
    }

    private fun showAboutDialog() {
        val alert = Alert(Alert.AlertType.INFORMATION).also {
            it.title = "About"
            it.headerText =
                    "RegionCropper (Megane Co)"
            it.contentText =
                    "Copyright 2016 Keiji Ariyama\n" +
                    "GitHub: https://github.com/keiji\n" +
                    "Web: https://blog.keiji.io"
        }

        alert.showAndWait()
    }

    private fun showFileChooser(): File? {
        val chooser = FileChooser().also {
            it.setTitle("Select Files")
            it.extensionFilters.add(
                    FileChooser.ExtensionFilter(
                            "Image files",
                            "*.png", "*.jpg", "*.jpeg", "*.gif")
            )
        }

        return chooser.showOpenDialog(stage)
    }

    private fun showDirectoryChooser(): File? {
        val chooser = DirectoryChooser().also {
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

    private fun prevPicture(offset: Int = 1, reverse: Boolean = false) {
        save()

        if (imageFileIndex == 0) {
            return
        }

        imageFileIndex -= offset
        imageFileIndex = if (imageFileIndex < 0) 0 else imageFileIndex

        loadFile(imageFileList[imageFileIndex], reverse)
    }

    private fun nextPicture(offset: Int = 1, reverse: Boolean = false) {
        save()

        val limit = imageFileList.size - 1
        if (imageFileIndex == limit) {
            return
        }

        imageFileIndex += offset
        imageFileIndex = if (imageFileIndex > limit) limit else imageFileIndex

        loadFile(imageFileList[imageFileIndex], reverse)
    }

    private fun loadFile(imageFile: File, reverse: Boolean = false) {
        val nameAndExtension = imageFile.name.split(".")
        val fileName = nameAndExtension.first()
        val dir = imageFile.parentFile

        val annotationList: AnnotationList

        aanotationFile = File(dir, String.format("%s.json", fileName))
        if (aanotationFile.exists()) {
            annotationList = AnnotationList.getInstance(aanotationFile.absolutePath)
        } else {
            annotationList = AnnotationList("Region Cropper",
                    imageFile.name,
                    ArrayList(),
                    Utils.createdAt())
        }

        imageData = Image(imageFile.toURI().toString())

        initEditView()
        editView.setData(imageData, annotationList, reverse)

        updateStatusBarText()
    }

    private fun updateStatusBarText() {
        stage.title = "%s (%,3d/%,3d)".format(
                imageFile.name, (imageFileIndex + 1), imageFileList.size)
        statusBar.text = ""
    }
}
