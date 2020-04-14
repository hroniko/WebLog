package com.hroniko.weblog.weblogger.aspects;


import com.hroniko.weblog.weblogger.annotations.RestLog;
import com.hroniko.weblog.weblogger.entities.RestLogContainer;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

@Aspect
public class RestLogAspect {

    private RestLogProducer logProducer = new RestLogProducer();

    @Around("@within(com.hroniko.weblog.weblogger.annotations.RestLog) && execution(public * *(..)) " +
            "|| @annotation(com.hroniko.weblog.weblogger.annotations.RestLog) && execution(* *(..))")
    public Object interceptLoggable(ProceedingJoinPoint joinPoint) throws Throwable {

        Signature signature = joinPoint.getSignature();

        if (signature instanceof MethodSignature) {
            Method method = ((MethodSignature) signature).getMethod();

            RestLog loggable = AnnotationUtils.findAnnotation(method, RestLog.class);

            if (loggable == null ) {
                loggable =  AnnotationUtils.findAnnotation(method.getDeclaringClass(), RestLog.class);
            }

            String loggerName = loggable.loggerName();
            Class clazz = loggable.clazz();

            logProducer.log(new RestLogContainer(joinPoint,
                    StringUtils.isNoneBlank(loggerName)
                            ? loggerName
                            : (Void.class.equals(clazz)
                            ? joinPoint.getSignature().getDeclaringType()
                            : clazz
                    ).getName(),
                    loggable.level(),
                    loggable.ignoreParams()
            ));
        }
        return joinPoint.proceed();
    }
}
