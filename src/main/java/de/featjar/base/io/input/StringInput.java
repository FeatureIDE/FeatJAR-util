/*
 * Copyright (C) 2023 Sebastian Krieter, Elias Kuiter
 *
 * This file is part of FeatJAR-base.
 *
 * base is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * base is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with base. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-base> for further information.
 */
package de.featjar.base.io.input;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

/**
 * A string input.
 *
 * @author Elias Kuiter
 */
public class StringInput extends AInput {
    /**
     * Creates a string input.
     *
     * @param string        the string
     * @param charset       the charset
     * @param fileExtension the file extension
     */
    public StringInput(java.lang.String string, Charset charset, java.lang.String fileExtension) {
        super(new ByteArrayInputStream(string.getBytes(charset)), charset, fileExtension);
    }
}