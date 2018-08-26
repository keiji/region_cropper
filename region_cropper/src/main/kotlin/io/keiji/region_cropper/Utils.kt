package io.keiji.region_cropper

import java.text.SimpleDateFormat
import java.util.*

class Utils {
    companion object {
        fun createdAt(): String {
            val tz = TimeZone.getTimeZone("UTC")
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S")
            df.setTimeZone(tz)
            return df.format(Date())
        }
    }

}