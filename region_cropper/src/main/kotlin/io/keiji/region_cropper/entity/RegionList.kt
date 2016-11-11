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

class PositionComparator : Comparator<Region> {

    override fun compare(obj1: Region, obj2: Region): Int {
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

data class RegionList(
        @SerializedName("generator")
        val generator: String,

        @SerializedName("file_name")
        val fileName: String,

        @SerializedName("regions")
        var regions: ArrayList<Region>?,

        @SerializedName("created_at")
        val createdAt: String
) {
    companion object {
        fun getInstance(filePath: String): RegionList {
            val source = File(filePath).readText(Charset.forName("UTF-8"))
            return Gson().fromJson(source, RegionList::class.java)!!
        }
    }

    fun save(file: File?) {
        val gson = GsonBuilder().setPrettyPrinting().create();
        file!!.writeText(gson.toJson(this, RegionList::class.java), Charset.forName("UTF-8"))
    }

    fun deepCopy(): RegionList {
        val copiedRegions = ArrayList<Region>()
        for (region: Region in regions!!) {
            copiedRegions.add(region.deepCopy())
        }
        return copy(regions = copiedRegions)
    }

}