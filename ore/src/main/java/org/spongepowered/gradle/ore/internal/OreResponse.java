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
package org.spongepowered.gradle.ore.internal;

import org.apache.hc.core5.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface OreResponse<V> {

    static <T> @NotNull Success<T> success(final @Nullable T value) {
        return new Success<>(value);
    }

    @SuppressWarnings("unchecked")
    static <T> @NotNull Reauthenticate<T> reauthenticate() {
        return Reauthenticate.INSTANCE;
    }

    static <T> @NotNull Failure<T> failure(final int responseCode, final @Nullable String errorMessage) {
        return new Failure<>(responseCode, errorMessage);
    }

    boolean wasSuccessful();

    int responseCode();

    <E extends Throwable> Success<V> asSuccessOrThrow(final Function<String, E> errorProvider) throws E;

    final class Success<T> implements OreResponse<T> {
        private final T value;

        Success(final T value) {
            this.value = value;
        }

        public T value() {
            return this.value;
        }

        @Override
        public boolean wasSuccessful() {
            return true;
        }

        @Override
        public int responseCode() {
            return HttpStatus.SC_OK;
        }

        @Override
        public <E extends Throwable> Success<T> asSuccessOrThrow(Function<String, E> errorProvider) {
            return this;
        }
    }

    final class Reauthenticate<T> implements OreResponse<T> {

        @SuppressWarnings("rawtypes")
        private static final Reauthenticate INSTANCE = new Reauthenticate<>();


        private Reauthenticate() {
        }

        @Override
        public boolean wasSuccessful() {
            return false;
        }

        @Override
        public int responseCode() {
            return HttpStatus.SC_UNAUTHORIZED;
        }

        @Override
        public <E extends Throwable> Success<T> asSuccessOrThrow(Function<String, E> errorProvider) throws E {
            throw errorProvider.apply("Session expired!");
        }
    }

    final class Failure<T> implements OreResponse<T> {
        private final int responseCode;
        private final @Nullable String errorMessage;

        public Failure(int responseCode, @Nullable String errorMessage) {
            this.responseCode = responseCode;
            this.errorMessage = errorMessage;
        }

        public @Nullable String errorMessage() {
            return this.errorMessage;
        }

        @Override
        public boolean wasSuccessful() {
            return false;
        }

        @Override
        public int responseCode() {
            return this.responseCode;
        }

        @Override
        public <E extends Throwable> Success<T> asSuccessOrThrow(Function<String, E> errorProvider) throws E {
            throw this.errorMessage == null ? errorProvider.apply(String.valueOf(this.responseCode)) : errorProvider.apply(this.errorMessage);
        }
    }

}
