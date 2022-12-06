package ru.axel.bricks;

import ru.axel.bricks.core.Core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException {
        Core.init(args);
        final Logger logger = Core.getLogger(Main.class, Level.ALL);
    }
}