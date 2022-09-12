/*
 * Copyright (C) 2022 Sebastian Krieter, Elias Kuiter
 *
 * This file is part of util.
 *
 * util is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * util is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with util. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-util> for further information.
 */
package de.featjar.base.io;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Input file header to determine whether a {@link de.featjar.base.io.format.Format} can parse a particular content.
 *
 * @author Sebastian Krieter
 */
public class InputHeader implements Supplier<String> {

    /**
     * Maximum number of bytes read from the input (1 MiB).
     */
    public static final int MAX_HEADER_SIZE = 0x00100000;

    private final byte[] bytes;

    private final Charset charset;

    private final String fileExtension;

    /**
     * Creates an input header.
     *
     * @param fileExtension the file extension
     * @param bytes the header bytes
     * @param charset the charset
     */
    public InputHeader(String fileExtension, byte[] bytes, Charset charset) {
        this.fileExtension = fileExtension;
        this.bytes = bytes;
        this.charset = charset;
    }

    /**
     * {@return this input header's charset}
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * {@return this input header's file extension, if any}
     */
    public Optional<String> getFileExtension() {
        return Optional.ofNullable(fileExtension);
    }

    /**
     * {@return this input header's bytes}
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * {@return this input header's string}
     */
    @Override
    public String get() {
        return new String(bytes, charset);
    }

    /**
     * {@return this input header's lines}
     */
    public Stream<String> getLines() {
        return get().lines();
    }
}