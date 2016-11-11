package io.keiji.region_cropper.entity

import com.google.gson.annotations.SerializedName

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

data class Region(
        @SerializedName("probability")
        val probability: Double,

        @SerializedName("label")
        var label: Int,

        @SerializedName("rect")
        val rect: Rect
) {
    fun deepCopy(): Region {
        return Region(probability, label, rect.copy())
    }

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
