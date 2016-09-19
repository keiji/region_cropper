package io.keiji.region_cropper.entity

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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import java.io.File
import java.nio.charset.Charset
import java.util.*

class LabelComparator : Comparator<Settings.Label> {
    override fun compare(obj1: Settings.Label, obj2: Settings.Label): Int {
        when {
            obj1.number > obj2.number -> return 1
            obj1.number < obj2.number -> return -1
            else -> return 0
        }
    }
}

data class Settings(
        @Expose
        @SerializedName("default_label_number")
        val defaultLabelNumber: Int,

        @Expose
        @SerializedName("selected_region_stroke_color")
        val selectedRegionStrokeColor: String,

        @Expose
        @SerializedName("dragging_region_stroke_color")
        val draggingRegionStrokeColor: String,

        @Expose
        @SerializedName("label_settings")
        val labelSettings: ArrayList<Label>
) {
    companion object {
        fun getInstance(filePath: String): Settings {
            val source = File(filePath).readText(Charset.forName("UTF-8"))
            val settings = Gson().fromJson(source, Settings::class.java)!!
            Collections.sort(settings.labelSettings, LabelComparator())
            return settings
        }
    }

    val selectedRegionStrokeWebColor: Color
        get() {
            return Color.web(selectedRegionStrokeColor)
        }

    val draggingRegionStrokeWebColor: Color
        get() {
            return Color.web(draggingRegionStrokeColor)
        }

    fun save(file: File) {
        val gson = GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();
        file.writeText(gson.toJson(this, Settings::class.java), Charset.forName("UTF-8"))
    }

    data class Label(
            @Expose
            @SerializedName("number")
            val number: Int,

            @Expose
            @SerializedName("name")
            val name: String?,

            @Expose
            @SerializedName("editable")
            var editable: Boolean,

            @Expose
            @SerializedName("deletable")
            var deletable: Boolean,

            @Expose
            @SerializedName("color")
            val color: String
    ) {

        val webColor: Color
            get() {
                return Color.web(color)
            }
    }

}