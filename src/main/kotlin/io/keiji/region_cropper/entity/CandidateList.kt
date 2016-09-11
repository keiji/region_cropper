package io.keiji.region_cropper.entity

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.io.File
import java.nio.charset.Charset
import java.util.*

class CandidateComparator : Comparator<CandidateList.Candidate> {
    override fun compare(obj1: CandidateList.Candidate, obj2: CandidateList.Candidate): Int {
        when {
            obj1.rect.left > obj2.rect.left -> return 1
            obj1.rect.left < obj2.rect.left -> return -1
            obj1.rect.top > obj2.rect.top -> return -1
            obj1.rect.top < obj2.rect.top -> return 1
            else -> return 0
        }
    }
}

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
        var regions: ArrayList<Candidate>?,

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

    data class Candidate(
            @SerializedName("likelihood")
            val likelihood: Double,

            @SerializedName("is_face")
            var isFace: Boolean,

            @SerializedName("rect")
            val rect: Rect
    ) {
        class Rect(
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

            fun offset(x: Float, y: Float) {
                left += x
                right += x
                top += y
                bottom += y
            }
        }
    }
}