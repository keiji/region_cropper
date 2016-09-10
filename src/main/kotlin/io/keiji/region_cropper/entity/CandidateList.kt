package io.keiji.region_cropper.entity

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.io.File
import java.nio.charset.Charset
import java.util.*

data class CandidateList(
        @SerializedName("generator")
        val generator: String,

        @SerializedName("model_version")
        val modelVersion: String,

        @SerializedName("engine_version")
        val engineVersion: String,

        @SerializedName("file_name")
        val fileName: String,

        @SerializedName("mode")
        val mode: String,

        @SerializedName("candidates")
        val candidates: ArrayList<Candidate>,

        @SerializedName("regions")
        var regions: ArrayList<Candidate>,

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
        val gson = GsonBuilder().setPrettyPrinting().create();
        file!!.writeText(gson.toJson(this, CandidateList::class.java), Charset.forName("UTF-8"))
    }

    class Candidate(likelihood: Double, isFace: Boolean, rect: Rect) {
        @SerializedName("likelihood")
        val likelihood: Double = likelihood

        @SerializedName("rect")
        val rect: Rect = rect

        @SerializedName("is_face")
        var isFace: Boolean = isFace

        class Rect(left: Float, top: Float, right: Float, bottom: Float) {
            @SerializedName("left")
            var left: Float = left

            @SerializedName("top")
            var top: Float = top

            @SerializedName("right")
            var right: Float = right

            @SerializedName("bottom")
            var bottom: Float = bottom

            fun width(): Float {
                return right - left
            }

            fun height(): Float {
                return bottom - top
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