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
 * See <https://github.com/FeatJAR/util> for further information.
 */
package de.featjar.util.logging;

import de.featjar.util.cli.CLI;
import de.featjar.util.data.Problem;
import de.featjar.util.io.MultiStream;
import de.featjar.util.job.Monitor;
import de.featjar.util.job.MonitorUpdateFunction;
import de.featjar.util.job.UpdateThread;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Extends the standard output with time codes, indentation, and log file
 * writing.
 *
 * @author Sebastian Krieter
 */
public final class Logger {

    public enum LogType {
        ERROR,
        INFO,
        DEBUG,
        PROGRESS
    }

    private static class Log {
        private final HashSet<LogType> enabledLogTypes = new HashSet<>();
        private final String path;
        private final PrintStream out;

        public Log(String path, PrintStream out, LogType... logTypes) {
            this.path = path;
            this.out = out;
            for (final LogType logType : logTypes) {
                enabledLogTypes.add(logType);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }

        @Override
        public boolean equals(Object obj) {
            return (this == obj) || ((obj instanceof Log) && Objects.equals(path, ((Log) obj).path));
        }
    }

    private static final PrintStream orgOut = System.out;
    private static final PrintStream orgErr = System.err;

    private static boolean installed = false;

    private static final LinkedHashSet<Log> logs = new LinkedHashSet<>();
    private static final LinkedList<Formatter> formatters = new LinkedList<>();
    private static Progress progressBar = null;

    public static synchronized boolean addFileLog(Path path, LogType... logTypes) {
        if (!installed) {
            path = path.toAbsolutePath().normalize();
            try {
                final PrintStream stream = new PrintStream(new FileOutputStream(path.toFile()));
                return logs.add(new Log(path.toString(), stream, logTypes));
            } catch (final FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static synchronized boolean setOutLog(LogType... logTypes) {
        if (!installed) {
            return logs.add(new Log(CLI.SYSTEM_OUTPUT, System.out, logTypes));
        }
        return false;
    }

    public static synchronized boolean setErrLog(LogType... logTypes) {
        if (!installed) {
            return logs.add(new Log(CLI.SYSTEM_ERROR, System.err, logTypes));
        }
        return false;
    }

    public static synchronized void addFormatter(Formatter formatter) {
        formatters.add(formatter);
    }

    public static synchronized void removeFormatter(Formatter formatter) {
        formatters.remove(formatter);
    }

    public static synchronized void install() {
        if (!installed) {
            final List<OutputStream> outStreamList = new ArrayList<>();
            final List<OutputStream> errStreamList = new ArrayList<>();
            for (final Log log : logs) {
                outStreamList.add(log.out);
                if (log.enabledLogTypes.contains(LogType.ERROR)) {
                    errStreamList.add(log.out);
                }
            }

            System.setOut(new PrintStream(new MultiStream(outStreamList)));
            System.setErr(new PrintStream(new MultiStream(errStreamList)));

            installed = true;
        }
    }

    public static synchronized void uninstall() {
        if (installed) {
            System.setOut(orgOut);
            System.setErr(orgErr);
            installed = false;
        }
    }

    public static UpdateThread startMonitorLogger(Monitor monitor) {
        final UpdateThread updateThread = new UpdateThread(new MonitorUpdateFunction(monitor));
        updateThread.start();
        return updateThread;
    }

    public static void logProblems(List<Problem> problems) {
        problems.stream().map(Problem::getException).flatMap(Optional::stream).forEach(Logger::logError);
    }

    public static void logError(Throwable error) {
        println(error);
    }

    public static void logError(String message) {
        println(message, LogType.ERROR);
    }

    public static void logInfo(Object messageObject) {
        println(String.valueOf(messageObject), LogType.INFO);
    }

    public static void logInfo(String message) {
        println(message, LogType.INFO);
    }

    public static void logDebug(Object messageObject) {
        println(String.valueOf(messageObject), LogType.DEBUG);
    }

    public static void logDebug(String message) {
        println(message, LogType.DEBUG);
    }

    public static void logProgress(String message) {
        println(message, LogType.PROGRESS);
    }

    public static void log(String message, LogType logType) {
        println(message, logType);
    }

    public static synchronized void startProgressEstimation() {
        progressBar = new Progress();
    }

    public static synchronized void stopProgressEstimation() {
        if (progressBar != null) {
            final String message = progressBar.toBlankString();
            if (installed) {
                for (final Log log : logs) {
                    if (log.enabledLogTypes.contains(LogType.PROGRESS)) {
                        log.out.print(message);
                    }
                }
            } else {
                System.out.println(message);
            }
            progressBar = null;
        }
    }

    public static synchronized void showProgress(Monitor monitor) {
        if (progressBar != null) {
            final String message = progressBar.toString(monitor.getRelativeWorkDone()) //
                    + ' ' //
                    + monitor.reportStatus();
            if (installed) {
                for (final Log log : logs) {
                    if (log.enabledLogTypes.contains(LogType.PROGRESS)) {
                        log.out.print(message);
                    }
                }
            } else {
                System.out.print(message);
            }
        }
    }

    private static synchronized void println(String message, LogType logType) {
        final String formattedMessage = formatMessage(message);
        if (installed) {
            for (final Log log : logs) {
                if (log.enabledLogTypes.contains(logType)) {
                    log.out.println(formattedMessage);
                }
            }
        } else {
            switch (logType) {
                case ERROR:
                    System.err.println(formattedMessage);
                    break;
                case DEBUG:
                    break;
                case INFO:
                case PROGRESS:
                default:
                    System.out.println(formattedMessage);
                    break;
            }
        }
    }

    private static synchronized void println(Throwable error) {
        final String formattedMessage = formatMessage(error.getMessage());
        if (installed) {
            for (final Log log : logs) {
                if (log.enabledLogTypes.contains(LogType.ERROR)) {
                    log.out.println(formattedMessage);
                    error.printStackTrace(log.out);
                }
            }
        } else {
            System.err.println(formattedMessage);
            error.printStackTrace(System.err);
        }
    }

    private static String formatMessage(String message) {
        if (formatters.isEmpty()) {
            return message;
        } else {
            final StringBuilder sb = new StringBuilder();
            for (final Formatter formatter : formatters) {
                formatter.format(sb);
            }
            sb.append(message);
            return sb.toString();
        }
    }
}
