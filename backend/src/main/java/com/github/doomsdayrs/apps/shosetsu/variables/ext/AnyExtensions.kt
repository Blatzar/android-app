package com.github.doomsdayrs.apps.shosetsu.variables.ext

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectOutputStream

/*
 * This file is part of shosetsu.
 *
 * shosetsu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * shosetsu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with shosetsu.  If not, see <https://www.gnu.org/licenses/>.
 * ====================================================================
 */

/**
 * shosetsu
 * 04 / 03 / 2020
 *
 * @author github.com/doomsdayrs
 */

/**
 * Serialize object to string
 *
 * @return Serialised string
 * @throws IOException exception
 */
@Throws(IOException::class)
fun Any.serializeToString(): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
    objectOutputStream.writeObject(this)
    val bytes = byteArrayOutputStream.toByteArray()
    return "serial-" + Base64.encodeToString(bytes, Base64.NO_WRAP)
}


/**
 * Converts Array of Strings into a String
 *
 * @return String Array
 */
fun Array<String>.convertArrayToString(): String {
    if (isNotEmpty()) {
        for (x in indices) {
            this[x] = this[x].replace(",", ">,<")
        }
        return contentToString()
    }
    return "[]"
}