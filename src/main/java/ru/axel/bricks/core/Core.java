package ru.axel.bricks.core;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Класс ядра.
 *
 * Требования:
 * 1. парсить аргументы: (done)
 *      I. app.config - файл с HOCON конфигурацией приложения. (done)
 *
 * 2. Установить логгер. (done)
 * 3. Определить каталог со сценариями. (done)
 * 4. Получить все сценарии. (done)
 * 5. Ручной запуск сценариев.
 * 6. Автозапуск сценариев по приоритету.
 */
public final class Core {
    private static Logger logger = Logger.getLogger(Core.class.getName());
    private static Method getLoggerMethod;
    private static Method setLogLevel;
    private static String scenarioDirectory;

    /* Константы */
    private static final String defaultConfigPath = "app/config/app-default.conf";
    private static final String defaultLoggerPath = "/libs/logger-1.0.0.jar";
    private static final String defaultLoggerClassName = "ru.axel.logger.MiniLogger";
    private static final String defaultScenarioPath = "/scenario";

    private Core() {
    }

    /**
     * Инициация ядра приложения. Парсинг аргументов приложения.
     * @param args - аргументы приложения.
     * @return инстанс ядра
     */
    public static void init(String[] args) throws MalformedURLException {
        /* 1. парсить аргументы: */
        final Stream<String> appConfigStream = Arrays.stream(args).filter(arg -> arg.startsWith("app.config="));
        final Optional<String> appConfig = appConfigStream.findAny();
        final Config config = getConfig(appConfig); // I. app.config - файл с HOCON конфигурацией приложения.

        /* 2. Установить логгер */
        installLogger(config);

        /* 3. Определить каталог со сценариями. */
        final Optional<String> scenarioPath = Optional.ofNullable(
            config.hasPath("application.scenario.path")
                ? config.getString("application.scenario.path")
                : null
        );

        scenarioDirectory = scenarioPath.orElse(defaultScenarioPath);
        logger.config("Каталог для сценариев определен: " + scenarioDirectory);
    }

    /**
     * Метод определяет нахождение файла конфигурации
     * @param appConfig возможный путь до файла
     * @return класс для чтения конфигурации
     */
    private static Config getConfig(@NotNull Optional<String> appConfig) {
        if (appConfig.isPresent()) { // 1. app.config - файл с HOCON конфигурацией приложения.
            final String pathToConfig = appConfig.get().split("=")[1];
            final File file = new File(pathToConfig);

            return file.exists() ? ConfigFactory.parseFile(file) : ConfigFactory.parseResources(pathToConfig);
        } else { // 1. app.config - файл с default HOCON конфигурацией приложения.
            logger.severe("Определен путь до файла конфигураций по умолчанию: " + defaultConfigPath);
            return ConfigFactory.parseResources(defaultConfigPath);
        }
    }

    /**
     * Метод устанавливает логгер для ядра.
     * @param config класс для чтения конфигурации.
     * @throws MalformedURLException
     */
    @SuppressWarnings("unchecked")
    private static void installLogger(@NotNull Config config) throws MalformedURLException {
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

            logger.config("Модуль Logger загружен!");
        } catch (Throwable throwable) {
            logger.severe("Ошибка загрузки модуля Logger!");
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
    public static Logger getLogger(
        Class<?> clazz,
        Level level
    ) throws InvocationTargetException, IllegalAccessException {
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
    public static Logger getLogger(
        Class<?> clazz
    ) throws InvocationTargetException, IllegalAccessException {
        setLogLevel.invoke(null, logger.getLevel());

        return (Logger) getLoggerMethod.invoke(null, clazz);
    }

    /**
     * Метод наход все сценарии в каталоге сценариев и возвращает отсортированный список сценариев по приоритету.
     * @return отсортированный список сценариев по приоритету.
     * @throws URISyntaxException
     */
    @Contract(pure = true)
    public static @NotNull Set<IScenario> getScenarioList() throws URISyntaxException {
        File scenariosDir = new File(scenarioDirectory);

        if (!scenariosDir.exists() || !scenariosDir.isDirectory()) {
            scenariosDir = new File(Objects.requireNonNull(Core.class.getResource(scenarioDirectory)).toURI());
        }

        /* 4. Получить все сценарии. */
        try(final Stream<Path> stream= Files.list(Path.of(scenariosDir.toURI()))) {
            return stream
                .filter(file -> !Files.isDirectory(file))
                .map(scenarioFile -> new Scenario(
                    ConfigFactory.parseFile(scenarioFile.toFile()),
                    logger
                ))
                .sorted(Comparator.comparingInt(Scenario::getPriority))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return new HashSet<>();
    }
}
