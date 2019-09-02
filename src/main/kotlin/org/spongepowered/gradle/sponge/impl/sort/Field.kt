/*
 * This file is part of SpongeGradle, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.gradle.sponge.impl.sort

import org.spongepowered.gradle.sponge.impl.util.TextConstants

/**
 * Field wrapper used to keep all of the component parts of a field
 * declaration together and allow them to be sorted based on name. Also
 * stores the field ordinal to preserve ordering in case natural ordering
 * fails.
 */
open class Field(
        /**
         * Comment lines, accumulated here until the field declaration is
         * located
         */
        var comment: Array<String> = arrayOf(),

        /**
         * Field modifiers, eg. public static final
         */
        var modifiers: String = "",
        /**
         * Field type, basically whatever is between the modifiers and the field
         * name
         */
        var type: String = "",
        /**
         * Field name
         */
        var name: String = "",
        /**
         * Field initialiser, basically whatever is between the field name and
         * the end of the line
         */
        var initializer: String = "",
        /**
         * Field ordinal
         */
        val index: Int = Indexes.index++
) : Comparable<Field> {

    fun isHasContent() : Boolean {
        return comment.isNotEmpty()
    }

    fun isValid() {
        !modifiers.isEmpty() && !type.isEmpty() && !name.isEmpty() && !initializer.isEmpty()
    }
    override fun compareTo(other: Field): Int {
        val diff : Int = this.name.compareTo(other.name)
        return if (diff == 0) index - other.index else diff
    }


    /**
     * Returns accumulated field comments as a String. In actual fact we
     * accumulate everything we don't recognise as a field in the "comments"
     * for the field, and this simply returns accumulated content
     *
     * @return
     */
    fun flush() : String
    {
        var commentBlock = ""
        for (commentLine in comment) {
            commentBlock += commentLine + TextConstants.newLine
        }
        return commentBlock
    }

    override fun toString() : String
    {
        return this.flush() + modifiers + type + name+ initializer
    }


    object Indexes {
        /**
         * Next field ordinal
         */
        var index: Int = 0
    }
}