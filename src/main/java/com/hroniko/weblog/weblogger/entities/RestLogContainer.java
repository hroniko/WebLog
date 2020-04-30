package com.hroniko.weblog.weblogger.entities;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.boot.logging.LogLevel;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RestLogContainer {
    private final static int[] NO_PARAMS = {};

    private final JoinPoint joinPoint;
    private final Object result;
    private final String logFile;
    private final int[] ignoreParams;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final Throwable exception;

    public RestLogContainer(JoinPoint joinPoint,
                            Object result,
                            String logFile,
                            int[] ignoreParams,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            Throwable exception) {
        this.joinPoint = joinPoint;
        this.result = result;
        this.logFile = logFile;
        this.ignoreParams = ignoreParams == null ? NO_PARAMS : ignoreParams;
        this.request = request;
        this.response = response;
        this.exception = exception;
    }

    public JoinPoint getJoinPoint() {
        return joinPoint;
    }

    public Object getResult() {
        return result;
    }

    public String getLogFile() {
        return logFile;
    }

    public int[] getIgnoreParams() {
        return ignoreParams;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public Throwable getException() {
        return exception;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
                .append("joinPoint", joinPoint)
                .append("logFile", logFile)
                .append("ignoreParams", ignoreParams)
                .append("request", request)
                .append("response", response)
                .append("exception", exception)
                .toString();
    }
}
