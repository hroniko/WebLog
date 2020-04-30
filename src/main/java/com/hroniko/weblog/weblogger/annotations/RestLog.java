package com.hroniko.weblog.weblogger.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface RestLog {

    String logFile() default "/logs/restlog.log";

    int[] ignoreParams() default {};

}