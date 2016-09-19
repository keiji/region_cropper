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

class PositionComparator : Comparator<CandidateList.Region> {
    override fun compare(obj1: CandidateList.Region, obj2: CandidateList.Region): Int {
        val distance1 = Math.sqrt(Math.pow(obj1.rect.centerX().toDouble(), 2.0)
                + Math.pow(obj1.rect.centerY().toDouble(), 2.0))
        val distance2 = Math.sqrt(Math.pow(obj2.rect.centerX().toDouble(), 2.0)
                + Math.pow(obj2.rect.centerY().toDouble(), 2.0))

        when {
            distance1 > distance2 -> return 1
            distance1 < distance2 -> return -1
            else -> return 0
        }
    }
}

class LikelihoodComparator : Comparator<CandidateList.Region> {
    override fun compare(obj1: CandidateList.Region, obj2: CandidateList.Region): Int {
        when {
            obj1.probability > obj2.probability -> return 1
            obj1.probability < obj2.probability -> return -1
            else -> return 0
        }
    }
}

data class CandidateList(
        @SerializedName("generator")
        val generator: String,

        @SerializedName("file_name")
        val fileName: String,

        @SerializedName("detected_faces")
        val detectedFaces: DetectedFaces?,

        @SerializedName("regions")
        var regions: ArrayList<Region>?,

        @SerializedName("created_at")
        val createdAt: String
) {
    companion object {
        fun getInstance(filePath: String): CandidateList {
            val source = File(filePath).readText(Charset.forName("UTF-8"))
            return Gson().fromJson(source, CandidateList::class.java)!!
        }
    }

    fun save(file: File?) {
        Collections.sort(regions, LikelihoodComparator())
        val gson = GsonBuilder().setPrettyPrinting().create();
        file!!.writeText(gson.toJson(this, CandidateList::class.java), Charset.forName("UTF-8"))

        Collections.sort(regions, PositionComparator())
    }

    fun deepCopy(): CandidateList {
        val copiedRegions = ArrayList<CandidateList.Region>()
        for (region: CandidateList.Region in regions!!) {
            val copiedRect = region.rect.copy()
            copiedRegions.add(CandidateList.Region(region.probability, region.label, copiedRect))
        }
        return copy(regions = copiedRegions)
    }

    data class DetectedFaces(
            @SerializedName("model_version")
            val modelVersion: String,

            @SerializedName("engine_version")
            val engineVersion: String,

            @SerializedName("regions")
            val regions: ArrayList<Region>
    ) {

    }

    data class Region(
            @SerializedName("probability")
            val probability: Double,

            @SerializedName("label")
            var label: Int,

            @SerializedName("rect")
            val rect: Rect
    ) {
        data class Rect(
                @SerializedName("left")
                var left: Float,

                @SerializedName("top")
                var top: Float,

                @SerializedName("right")
                var right: Float,

                @SerializedName("bottom")
                var bottom: Float
        ) {

            fun width(): Float {
                return right - left
            }

            fun height(): Float {
                return bottom - top
            }

            fun centerX(): Float {
                return left + width() / 2
            }

            fun centerY(): Float {
                return top + height() / 2
            }

            fun offset(x: Float, y: Float) {
                left += x
                right += x
                top += y
                bottom += y
            }
        }
    }
}