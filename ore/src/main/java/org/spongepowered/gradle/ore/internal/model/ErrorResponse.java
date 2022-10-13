/*
 * This file is part of spongegradle-ore, licensed under the MIT License (MIT).
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
package org.spongepowered.gradle.ore.internal.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public class ErrorResponse {
    private final JsonElement error;

    @SerializedName("user_error")
    private final String userError;

    private final int requestId;

    public ErrorResponse(final JsonElement error, final String userError, final int requestId) {
        this.error = error;
        this.userError = userError;
        this.requestId = requestId;
    }

    public @Nullable String error() {
        if (this.userError != null) {
            return this.userError;
        } else if (this.error == null) {
            return null;
        }

        return this.error.isJsonObject() ? this.error.getAsJsonObject().getAsJsonPrimitive("message").getAsString() : this.error.getAsString();
    }
}
