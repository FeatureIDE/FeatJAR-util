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
package de.featjar.base.cli;

import de.featjar.base.FeatJAR;
import de.featjar.base.data.Result;
import de.featjar.base.data.Void;
import de.featjar.base.log.IndentStringBuilder;
import de.featjar.base.log.Log;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Parses and validates options.
 *
 * @author Elias Kuiter
 */
public interface IOptionInput {
    /**
     * Option for printing usage information.
     */
    Option<ICommand> COMMAND_OPTION = new Option<>(
                    "command", s -> FeatJAR.extensionPoint(Commands.class).getMatchingExtension(s))
            .setRequired(true)
            .setDescription("Command to execute");

    /**
     * Option for printing usage information.
     */
    Option<Boolean> HELP_OPTION = new Flag("help").setDescription("Print usage information");

    /**
     * Option for printing version information.
     */
    Option<Boolean> VERSION_OPTION = new Flag("version").setDescription("Print version information");

    /**
     * Option for setting the logger verbosity.
     */
    Option<Log.Verbosity> VERBOSITY_OPTION = new Option<>("verbosity", Log.Verbosity::of)
            .setDescription("The logger verbosity, one of "
                    + Arrays.stream(Log.Verbosity.values())
                            .map(Objects::toString)
                            .map(String::toLowerCase)
                            .collect(Collectors.joining(", ")))
            .setDefaultValue(Commands.DEFAULT_VERBOSITY);

    /**
     * {@return a void result when the given options are valid in this option input}
     * In particular, returns an empty result when there are unused options in this option input.
     *
     * @param options the options
     */
    Result<Void> validate(List<Option<?>> options);

    /**
     * {@return the value of the given option in this option input}
     *
     * @param option the option
     * @param <T> the type of the option value
     */
    <T> Result<T> get(Option<T> option);

    /**
     * {@return the commands supplied in this option input}
     */
    default Result<ICommand> getCommand() {
        return get(COMMAND_OPTION);
    }

    /**
     * {@return the general options of this option input}
     */
    default List<Option<?>> getOptions() {
        return List.of(COMMAND_OPTION, HELP_OPTION, VERSION_OPTION, VERBOSITY_OPTION);
    }

    /**
     * {@return the command-line interface help}
     */
    default String getHelp() {
        IndentStringBuilder sb = new IndentStringBuilder();
        List<ICommand> commands = FeatJAR.extensionPoint(Commands.class).getExtensions();
        sb.appendLine("Usage: java -jar " + FeatJAR.LIBRARY_NAME
                        + " --command <command> [--<flag> | --<option> <value>]...")
                .appendLine();
        if (commands.size() == 0) {
            sb.append("No commands are available. You can register commands in an extensions.xml file when building "
                    + FeatJAR.LIBRARY_NAME + ".\n");
        }
        sb.append("The following commands are available:\n").addIndent();
        for (final ICommand command : commands) {
            sb.appendLine(String.format(
                    "%s: %s",
                    command.getIdentifier(),
                    Result.ofNullable(command.getDescription()).orElse("")));
        }
        sb.removeIndent();
        sb.appendLine();
        sb.appendLine("General options:").addIndent();
        sb.appendLine(getOptions());
        sb.removeIndent();
        if (getCommand().isPresent()) {
            ICommand command = getCommand().get();
            if (!command.getOptions().isEmpty()) {
                sb.appendLine();
                sb.appendLine(String.format("Options of command %s:", command.getIdentifier()));
                sb.addIndent();
                sb.appendLine(command.getOptions());
                sb.removeIndent();
            }
        }
        return sb.toString();
    }

    /**
     * {@return whether this option input requests help information}
     */
    default boolean isHelp() {
        return get(HELP_OPTION).get();
    }

    /**
     * {@return whether this option input requests version information}
     */
    default boolean isVersion() {
        return get(VERSION_OPTION).get();
    }

    /**
     * {@return the verbosity supplied in this option input}
     */
    default Log.Verbosity getVerbosity() {
        return get(VERBOSITY_OPTION).get();
    }
}