package com.hroniko.weblog.weblogger.entities;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.boot.logging.LogLevel;

public class RestLogContainer {
    private final static int[] NO_PARAMS = {};

    private final ProceedingJoinPoint joinPoint;
    private final String loggerName;
    private final LogLevel level;
    private final int[] ignoreParams;

    public RestLogContainer(ProceedingJoinPoint joinPoint,
                            String loggerName,
                            LogLevel level,
                            int[] ignoreParams) {
        this.joinPoint = joinPoint;
        this.loggerName = loggerName;
        this.level = level == null ? LogLevel.DEBUG : level;
        this.ignoreParams = ignoreParams == null ? NO_PARAMS : ignoreParams;
    }

    public ProceedingJoinPoint getJoinPoint() {
        return joinPoint;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public LogLevel getLevel() {
        return level;
    }

    public int[] getIgnoreParams() {
        return ignoreParams;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
                .append("joinPoint", joinPoint)
                .append("loggerName", loggerName)
                .append("invoke", level)
                .append("ignoreParams", ignoreParams)
                .toString();
    }
}
