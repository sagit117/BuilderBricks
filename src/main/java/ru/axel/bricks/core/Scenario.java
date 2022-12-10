package ru.axel.bricks.core;

import com.typesafe.config.Config;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public final class Scenario implements IScenario {
    private final String name;
    private final String version;
    private final int priority;
    private final Logger logger;
    
    public Scenario(@NotNull Config config, @NotNull Logger logger) {
        this.logger = logger;
        
        name = config.getString("scenario.name");
        version = config.getString("scenario.version");
        priority = config.getInt("scenario.priority");

        logger.config("Прочитан сценарий: " + name);
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
}
