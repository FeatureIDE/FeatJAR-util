/*
 * Copyright (C) 2023 FeatJAR-Development-Team
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
package de.featjar.base.log;

import de.featjar.base.data.Pair;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * Logs messages to standard output and files. Formats log messages with
 * {@link IFormatter formatters}. TODO: instead of logging values directly, only
 * pass suppliers that are called if some log target is configured. this saves
 * time for creating log strings
 *
 * @author Sebastian Krieter
 * @author Elias Kuiter
 */
public class BufferedLog implements Log {

    private final LinkedList<Pair<Verbosity, String>> logBuffer = new LinkedList<>();

    public void println(String message, Verbosity verbosity) {
        synchronized (logBuffer) {
            logBuffer.add(new Pair<>(verbosity, message));
        }
    }

    public void println(Throwable error, boolean isWarning) {
        synchronized (logBuffer) {
            logBuffer.add(new Pair<>(isWarning ? Verbosity.WARNING : Verbosity.ERROR, error.getMessage()));
        }
    }

    public void flush(Consumer<Pair<Verbosity, String>> messageConsumer) {
        synchronized (logBuffer) {
            for (Pair<Verbosity, String> message : logBuffer) {
                messageConsumer.accept(message);
            }
            logBuffer.clear();
        }
    }
}
