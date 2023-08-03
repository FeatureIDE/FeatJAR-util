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
package de.featjar.base;

import de.featjar.base.cli.Commands;
import de.featjar.base.cli.IOptionInput;
import de.featjar.base.cli.OptionList;
import de.featjar.base.computation.Cache;
import de.featjar.base.data.Result;
import de.featjar.base.extension.AExtensionPoint;
import de.featjar.base.extension.ExtensionManager;
import de.featjar.base.extension.IExtension;
import de.featjar.base.io.IO;
import de.featjar.base.log.CallerFormatter;
import de.featjar.base.log.Log;
import de.featjar.base.log.TimeStampFormatter;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Configures, initializes, and runs FeatJAR. To use FeatJAR, create a
 * {@link FeatJAR} object and use it. After usage, call {@link #close()} or use
 * a try...with block. If only a quick computation is needed, call
 * {@link #run(Consumer)} or {@link #apply(Function)}. For convenience, this
 * class inherits all methods in {@link IO} and provides access to the
 * {@link Log} with {@link #log()} and {@link Cache} with {@link #cache()}. Both
 * {@link #log()} and {@link #cache()} return fallback instances when used
 * outside a FeatJAR instantiation. For simplicity, only one FeatJAR instance
 * can exist at a time (although this limitation may be lifted in the future).
 * Thus, do not create FeatJAR objects at the same time in different threads.
 * Also, do not nest {@link #run(Consumer)} or {@link #apply(Function)} calls.
 * However, different FeatJAR instances can be created over time in the same
 * thread (e.g., to change the configuration).
 *
 * @author Elias Kuiter
 */
public class FeatJAR extends IO implements AutoCloseable {
    public static final String ROOT_PACKAGE_NAME = "de.featjar";
    public static final String LIBRARY_NAME = "feat.jar";

    /**
     * Configures FeatJAR.
     */
    public static class Configuration {
        /**
         * This configuration's log sub-configuration.
         */
        protected final Log.Configuration log = new Log.Configuration();

        /**
         * This configuration's cache sub-configuration.
         */
        protected final Cache.Configuration cache = new Cache.Configuration();

        /**
         * Configures this configuration's log sub-configuration.
         *
         * @param configurationConsumer the log configuration consumer
         * @return this configuration
         */
        public Configuration log(Consumer<Log.Configuration> configurationConsumer) {
            configurationConsumer.accept(log);
            return this;
        }

        /**
         * Configures this configuration's cache sub-configuration.
         *
         * @param configurationConsumer the cache configuration consumer
         * @return this configuration
         */
        public Configuration cache(Consumer<Cache.Configuration> configurationConsumer) {
            configurationConsumer.accept(cache);
            return this;
        }
    }

    /**
     * The current instance of FeatJAR. Only one instance can exist at a time.
     */
    private static FeatJAR instance;

    /**
     * This FeatJAR instance's extension manager. Holds references to all loaded
     * extension points and extensions.
     */
    protected final ExtensionManager extensionManager;

    /**
     * The default verbosity of FeatJAR, if not adjusted otherwise. Can be set at
     * startup to allow showing log output even before this value is adjusted.
     */
    public static Log.Verbosity defaultVerbosity = Log.Verbosity.INFO;

    /**
     * Configures the default log configuration, if not adjusted otherwise.
     */
    public static final Function<Log.Configuration, Log.Configuration> defaultLogConfiguration =
            cfg -> cfg.logAtMost(defaultVerbosity)
                    .addFormatter(new TimeStampFormatter())
                    .addFormatter(new CallerFormatter());

    /**
     * Configures the default cache configuration, if not adjusted otherwise.
     */
    public static final Function<Cache.Configuration, Cache.Configuration> defaultCacheConfiguration =
            cfg -> cfg.setCachePolicy(Cache.CachePolicy.CACHE_TOP_LEVEL);

    /**
     * {@return the current FeatJAR instance}
     */
    public static FeatJAR getInstance() {
        return instance;
    }

    /**
     * Initializes FeatJAR with a default configuration.
     */
    public static FeatJAR initialize() {
        return initialize(
                new Configuration().log(defaultLogConfiguration::apply).cache(defaultCacheConfiguration::apply));
    }

    /**
     * Initializes FeatJAR.
     *
     * @param configuration the FeatJAR configuration
     */
    public static FeatJAR initialize(Configuration configuration) {
        if (instance != null) throw new RuntimeException("FeatJAR already initialized");
        return instance = new FeatJAR(configuration);
    }
    /**
     * De-initializes FeatJAR.
     */
    public static void deinitialize() {
        if (instance != null) {
            log().debug("de-initializing FeatJAR");
            instance.extensionManager.close();
            instance = null;
        }
    }

    /**
     * Initializes FeatJAR.
     *
     * @param configuration the FeatJAR configuration
     */
    private FeatJAR(Configuration configuration) {
        log().debug("initializing FeatJAR");
        Log.setDefaultConfiguration(configuration.log);
        Cache.setDefaultConfiguration(configuration.cache);
        extensionManager = new ExtensionManager();
    }

    /**
     * De-initializes FeatJAR.
     */
    @Override
    public void close() {
        deinitialize();
    }

    /**
     * {@return this FeatJAR instance's extension manager}
     */
    public ExtensionManager getExtensionManager() {
        return extensionManager;
    }

    /**
     * {@return the extension point for a given class installed in this FeatJAR
     * instance's extension manager}
     *
     * @param <T>   the type of the extension point's class
     * @param klass the extension point's class
     */
    public <T extends AExtensionPoint<?>> Result<T> getExtensionPoint(Class<T> klass) {
        return extensionManager.getExtensionPoint(klass);
    }

    /**
     * {@return the extension for a given class installed in this FeatJAR instance's
     * extension manager}
     *
     * @param <T>   the type of the extension's class
     * @param klass the extension's class
     */
    public <T extends IExtension> Result<T> getExtension(Class<T> klass) {
        return extensionManager.getExtension(klass);
    }

    /**
     * Runs some function in a temporary FeatJAR instance.
     *
     * @param configuration the FeatJAR configuration
     * @param fn            the function
     */
    public static void run(Configuration configuration, Consumer<FeatJAR> fn) {
        try (FeatJAR featJAR = FeatJAR.initialize(configuration)) {
            fn.accept(featJAR);
        }
    }

    /**
     * Runs some function in a temporary FeatJAR instance.
     *
     * @param fn the function
     */
    public static void run(Consumer<FeatJAR> fn) {
        try (FeatJAR featJAR = FeatJAR.initialize()) {
            fn.accept(featJAR);
        }
    }

    /**
     * Runs some function in a temporary FeatJAR instance.
     *
     * @param configuration the FeatJAR configuration
     * @param fn            the function
     * @return the supplied object
     */
    public static <T> T apply(Configuration configuration, Function<FeatJAR, T> fn) {
        T t;
        try (FeatJAR featJAR = FeatJAR.initialize(configuration)) {
            t = fn.apply(featJAR);
        }
        return t;
    }

    /**
     * Runs some function in a temporary FeatJAR instance.
     *
     * @param fn the function
     * @return the supplied object
     */
    public static <T> T apply(Function<FeatJAR, T> fn) {
        T t;
        try (FeatJAR featJAR = FeatJAR.initialize()) {
            t = fn.apply(featJAR);
        }
        return t;
    }

    /**
     * {@return the extension point for a given class installed in the current
     * FeatJAR instance's extension manager}
     *
     * @param <T>   the type of the extension point's class
     * @param klass the extension point's class
     */
    public static <T extends AExtensionPoint<?>> T extensionPoint(Class<T> klass) {
        if (instance == null) throw new IllegalStateException("FeatJAR not initialized yet");
        Result<T> extensionPoint = instance.getExtensionPoint(klass);
        if (extensionPoint.isEmpty())
            throw new RuntimeException("extension point " + klass + " not currently installed in FeatJAR");
        return extensionPoint.get();
    }

    /**
     * {@return the extension point for a given class installed in the current
     * FeatJAR instance's extension manager}
     *
     * @param <T>   the type of the extension point's class
     * @param klass the extension point's class
     */
    public static <T extends IExtension> T extension(Class<T> klass) {
        if (instance == null) throw new IllegalStateException("FeatJAR not initialized yet");
        Result<T> extension = instance.getExtension(klass);
        if (extension.isEmpty())
            throw new RuntimeException("extension " + klass + " not currently installed in FeatJAR");
        return extension.get();
    }

    /**
     * {@return the current FeatJAR instance's log, or a fallback log if
     * uninitialized}
     */
    public static Log log() {
        return instance == null
                ? new Log.Fallback()
                : instance.getExtension(Log.class).orElseGet(Log.Fallback::new);
    }

    /**
     * {@return the current FeatJAR instance's cache, or a fallback cache if
     * uninitialized}
     */
    public static Cache cache() {
        return instance == null
                ? new Cache.Fallback()
                : instance.getExtension(Cache.class).orElseGet(Cache.Fallback::new);
    }

    /**
     * Main entry point of FeatJAR.
     *
     * @param arguments command-line arguments
     */
    public static void main(String[] arguments) {
        IOptionInput optionInput = new OptionList(arguments);
        defaultVerbosity = optionInput.getVerbosity();
        FeatJAR.run(featJAR -> Commands.run(optionInput));
    }
}
