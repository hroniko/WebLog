package com.hroniko.weblog.weblogger.aspects;


import com.hroniko.weblog.weblogger.annotations.RestLog;
import com.hroniko.weblog.weblogger.entities.RestLogContainer;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Aspect
public class RestLogAspect {

    private RestLogProducer logProducer = new RestLogProducer();

    @AfterReturning(value = "execution(@com.hroniko.weblogger.annotations.RestLog * *(..))", returning = "result")
    public void interceptRestLog(JoinPoint joinPoint, Object result) throws Throwable {
        restLogLogic(joinPoint, result, null);
    }

    @AfterThrowing(value = "execution(@com.hroniko.weblogger.annotations.RestLog * *(..))", throwing= "exception")
    public void afterThrowingAdvice(JoinPoint joinPoint, Throwable exception) throws Throwable {
        restLogLogic(joinPoint, null, exception);
    }

    private void restLogLogic(JoinPoint joinPoint, Object result, Throwable exception) throws Throwable {
        Signature signature = joinPoint.getSignature();

        if (signature instanceof MethodSignature) {
            Method method = ((MethodSignature) signature).getMethod();

            RestLog restLog = AnnotationUtils.findAnnotation(method, RestLog.class);

            if (restLog == null ) {
                restLog =  AnnotationUtils.findAnnotation(method.getDeclaringClass(), RestLog.class);
            }
            String logFile = restLog.logFile();

            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest();

            HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getResponse();

            logProducer.log(new RestLogContainer(joinPoint,
                    result,
                    logFile,
                    restLog.ignoreParams(),
                    request,
                    response,
                    exception
            ));
        }
    }
}
