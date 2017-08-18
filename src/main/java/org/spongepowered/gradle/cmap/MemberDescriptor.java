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
package org.spongepowered.gradle.cmap;

final class MemberDescriptor implements Comparable<MemberDescriptor> {

    final String name;
    final String desc;

    MemberDescriptor(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    @Override
    public int compareTo(MemberDescriptor o) {
        int result = this.name.length() - o.name.length();
        if (result == 0) {
            result = this.name.compareTo(o.name);
            if (result == 0) {
                result = this.desc.compareTo(o.desc);
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MemberDescriptor that = (MemberDescriptor) o;
        return this.name.equals(that.name) && this.desc.equals(that.desc);
    }

    @Override
    public int hashCode() {
        int result = this.name.hashCode();
        result = 31 * result + this.desc.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.name.length() + this.desc.length() + 1);
        builder.append(this.name);

        if (!this.desc.isEmpty() && this.desc.charAt(0) != '(') {
            builder.append(':');
        }

        builder.append(this.desc);
        return builder.toString();
    }

}
