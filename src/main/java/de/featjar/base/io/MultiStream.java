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

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * A stream that dispatches to several streams when written to.
 * Can be used to pipe some output through to many streams.
 *
 * @author Sebastian Krieter
 */
public class MultiStream extends OutputStream {

    protected final Set<OutputStream> streams = new HashSet<>();

    public MultiStream(OutputStream... streams) {
        this(Set.of(streams));
    }

    public MultiStream(Set<OutputStream> streams) {
        this.streams.addAll(streams);
    }

    public void addStream(OutputStream stream) {
        streams.add(stream);
    }

    public void clearStreams() {
        streams.clear();
    }

    @Override
    public void flush() throws IOException {
        for (final OutputStream outputStream : streams) {
            outputStream.flush();
        }
    }

    @Override
    public void write(byte[] buf, int off, int len) throws IOException {
        for (final OutputStream outputStream : streams) {
            outputStream.write(buf, off, len);
        }
    }

    @Override
    public void write(int b) throws IOException {
        for (final OutputStream outputStream : streams) {
            outputStream.write(b);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        for (final OutputStream outputStream : streams) {
            outputStream.write(b);
        }
    }
}