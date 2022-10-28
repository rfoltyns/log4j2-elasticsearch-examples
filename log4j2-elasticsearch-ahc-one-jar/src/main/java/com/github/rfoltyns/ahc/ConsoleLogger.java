package com.github.rfoltyns.ahc;

import org.appenders.core.logging.Logger;

public class ConsoleLogger implements Logger {

    private String replacePlaceholders(String messageFormat) {
        return messageFormat.replace("{}", "%s");
    }

    @Override
    public void error(String messageFormat, Object... parameters) {
        System.out.println(String.format(replacePlaceholders(messageFormat), parameters));
    }

    @Override
    public void warn(String messageFormat, Object... parameters) {
        System.out.println(String.format(replacePlaceholders(messageFormat), parameters));
    }

    @Override
    public void info(String messageFormat, Object... parameters) {
        System.out.println(String.format(replacePlaceholders(messageFormat), parameters));
    }

    @Override
    public void debug(String messageFormat, Object... parameters) {
        System.out.println(String.format(replacePlaceholders(messageFormat), parameters));
    }

    @Override
    public void trace(String messageFormat, Object... parameters) {
        System.out.println(String.format(replacePlaceholders(messageFormat), parameters));
    }
}
