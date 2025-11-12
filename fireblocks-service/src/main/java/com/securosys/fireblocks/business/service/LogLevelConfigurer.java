// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogLevelConfigurer implements SmartInitializingSingleton {

    @Value("${fireblocks.log-level:#{null}}")
    private Integer numericLevel;

    @Override
    public void afterSingletonsInstantiated() {

        if (numericLevel == null) {
            return;
        }

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Level level = mapNumericLevel(numericLevel);

        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(level);
        context.getLogger("com.securosys.fireblocks").setLevel(level);

    }

    private Level mapNumericLevel(int level) {
        return switch (level) {
            case 0, 1, 2, 3 -> Level.ERROR;   // Emergency, Alert, Critical, Error
            case 4 -> Level.WARN;             // Warning
            case 5, 6 -> Level.INFO;          // Notice, Informational
            case 7 -> Level.DEBUG;            // Debug
            default -> Level.INFO;            // fallback
        };
    }

}
