package io.keiji.region_cropper;

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

import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.control.MenuBar;

class MainMenuController(menuBar: MenuBar,
                         private val callback: Callback) {

    interface Callback {
        fun openFile()
        fun openDirectory()
        fun close()

        fun undo()
        fun quickCrop()
        fun cropTo()

        fun licenseDialog()
        fun aboutDialog()
    }

    init {
        // File
        menuBar.menus[0].let { menu ->
            // Open File
            menu.items[0].onAction = EventHandler<ActionEvent> {
                callback.openFile()
            }

            // Open File
            menu.items[1].onAction = EventHandler<ActionEvent> {
                callback.openDirectory()
            }

            // Separator

            // Quit
            menu.items[3].onAction = EventHandler<ActionEvent> {
                callback.close()
            }
        }

        // Edit
        menuBar.menus[1].let { menu ->
            // Undo
            menu.items[0].onAction = EventHandler<ActionEvent> {
                callback.undo()
            }

            // Separator

            // Quick Crop
            menu.items[2].onAction = EventHandler<ActionEvent> {
                callback.quickCrop()
            }

            // Crop to
            menu.items[3].onAction = EventHandler<ActionEvent> {
                callback.cropTo()
            }
        }

        // Help
        menuBar.menus[2].let { menu ->
            // Licenses
            menu.items[0].onAction = EventHandler<ActionEvent> {
                callback.licenseDialog()
            }

            // Separator

            // About
            menu.items[2].onAction = EventHandler<ActionEvent> {
                callback.aboutDialog()
            }
        }
    }

}
