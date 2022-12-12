package ru.axel.bricks;

import ru.axel.bricks.core.Core;
import ru.axel.bricks.core.IScenario;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException, IOException, URISyntaxException {
        Core.init(args);
        final Logger logger = Core.getLogger(Main.class, Level.ALL);

        final Set<IScenario> scenarios = Core.getScenarioList();

        scenarios.forEach(scenario -> {
            scenario.launch();
        });
    }
}