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
import com.google.gson.annotations.SerializedName
import java.io.File
import java.nio.charset.Charset
import java.util.*

data class CandidateList(
        @SerializedName("generator")
        val generator: String,

        @SerializedName("file_name")
        val fileName: String,

        @SerializedName("detected_faces")
        val detectedFaces: DetectedFaces,

        @SerializedName("created_at")
        val createdAt: String
) {
    companion object {
        fun getInstance(filePath: String): CandidateList {
            val source = File(filePath).readText(Charset.forName("UTF-8"))
            return Gson().fromJson(source, CandidateList::class.java)!!
        }
    }

    data class DetectedFaces(
            @SerializedName("model_version")
            val modelVersion: String,

            @SerializedName("regions")
            val regions: ArrayList<Region>
    ) {

    }

}