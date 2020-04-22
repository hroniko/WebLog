package com.hroniko.weblog.weblogger.annotations;

import org.springframework.boot.logging.LogLevel;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface RestLog {

    String logFile() default "/logs/restlog.log";

    LogLevel level() default LogLevel.DEBUG;

    Class clazz() default Void.class;

    String loggerName() default "";

    int[] ignoreParams() default {};

}
