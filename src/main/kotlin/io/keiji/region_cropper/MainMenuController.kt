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

class MainMenuController(menuBar: MenuBar, val callback: Callback) {

    interface Callback {
        fun openFile()
        fun close()

        fun quickCrop()
        fun cropTo()

        fun licenseDialog()
        fun aboutDialog()
    }

    init {
        // File
        menuBar.menus[0].let {
            // Open File
            it.items[0].onAction = EventHandler<ActionEvent> {
                callback.openFile()
            }

            // Separator

            // Quit
            it.items[2].onAction = EventHandler<ActionEvent> {
                callback.close()
            }
        }

        // Crop
        menuBar.menus[1].let {
            // Quick Crop
            it.items[0].onAction = EventHandler<ActionEvent> {
                callback.quickCrop()
            }

            // Crop to
            it.items[1].onAction = EventHandler<ActionEvent> {
                callback.cropTo()
            }
        }

        // Help
        menuBar.menus[2].let {

            // Licenses
            it.items[0].onAction = EventHandler<ActionEvent> {
                callback.licenseDialog()
            }

            // Separator

            // About
            it.items[2].onAction = EventHandler<ActionEvent> {
                callback.aboutDialog()
            }
        }
    }

}
