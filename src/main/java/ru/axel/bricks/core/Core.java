package ru.axel.bricks.core;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.*;
import java.util.stream.Stream;

/**
 * Класс ядра.
 *
 * Требования:
 * 1. парсить аргументы:
 *      I. app.config - файл с HOCON конфигурацией приложения.
 *
 * 2. Установить логгер
 */
public final class Core {
    private static Logger logger;
    private static Method getLoggerMethod;
    private static Method setLogLevel;
    private static final String defaultLoggerPath = "/libs/logger-1.0.0.jar";
    private static final String defaultLoggerClassName = "ru.axel.logger.MiniLogger";

    private Core() {
    }

    /**
     * Инициация ядра приложения. Парсинг аргументов приложения.
     * @param args - аргументы приложения.
     * @return инстанс ядра
     */
    public static void init(String[] args) throws IOException {
        final Stream<String> appConfigStream = Arrays.stream(args).filter(arg -> arg.startsWith("app.config="));
        final Optional<String> appConfig = appConfigStream.findAny();
        final Config config;

        if (appConfig.isPresent()) { // 1. app.config - файл с HOCON конфигурацией приложения.
            final String pathToConfig = appConfig.get().split("=")[1];
            final File file = new File(pathToConfig);

            config = file.exists() ? ConfigFactory.parseFile(file) : ConfigFactory.parseResources(pathToConfig);
        } else { // 1. app.config - файл с default HOCON конфигурацией приложения.
            config = ConfigFactory.parseResources("app/config/app-default.conf");
        }

        // 2. Установить логгер
        final Optional<String> loggerClassPath = Optional.ofNullable(
            config.hasPath("application.logger.classPath")
                ? config.getString("application.logger.classPath")
                : null
        );
        final Optional<String> loggerClassName = Optional.ofNullable(
            config.hasPath("application.logger.className")
                ? config.getString("application.logger.className")
                : null
        );
        final Optional<Level> level = Optional.ofNullable(
            config.hasPath("application.logger.level")
                ? Level.parse(config.getString("application.logger.level"))
                : null
        );

        final File fileClass = new File(loggerClassPath.orElse(defaultLoggerPath));
        final URL[] urls;

        if (fileClass.exists()) {
            urls = new URL[]{ fileClass.toURI().toURL() };
        } else { // logger по умолчанию
            urls = new URL[]{ Core.class.getResource(defaultLoggerPath) };
        }

        try (final URLClassLoader loader = new URLClassLoader(urls)) {
            final Class<Logger> loggerClass = (Class<Logger>) loader.loadClass(
                loggerClassName.orElse(defaultLoggerClassName)
            );

            getLoggerMethod = loggerClass.getDeclaredMethod("getLogger", Class.class);
            setLogLevel = loggerClass.getDeclaredMethod("setLogLevel", Level.class);
            logger = getLogger(Core.class, level.orElse(Level.CONFIG));

            logger.config("Logger module loaded!");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    /**
     * Метод возвращает настроенный инстанс логгера.
     * @param clazz - класс для логирования.
     * @param level - уровень логирования.
     * @return инстанс логгера.
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static Logger getLogger(Class<?> clazz, Level level) throws InvocationTargetException, IllegalAccessException {
        setLogLevel.invoke(null, level);
        return (Logger) getLoggerMethod.invoke(null, clazz);
    }

    /**
     * Метод возвращает настроенный инстанс логгера.
     * @param clazz - класс для логирования.
     * @return инстанс логгера.
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static Logger getLogger(Class<?> clazz) throws InvocationTargetException, IllegalAccessException {
        setLogLevel.invoke(null, logger.getLevel());
        return (Logger) getLoggerMethod.invoke(null, clazz);
    }
}
