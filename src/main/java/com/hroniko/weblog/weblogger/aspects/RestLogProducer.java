package com.hroniko.weblog.weblogger.aspects;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;

import com.hroniko.weblog.persistence.FileSaver;
import com.hroniko.weblog.weblogger.entities.RestLogContainer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.logging.LogLevel;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class RestLogProducer {

    private static final SimpleDateFormat dateFormatter =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    private static final ImmutableList<String> httpCrudOperations = ImmutableList.<String>builder()
            .add("GET", "HEAD", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS", "TRACE", "PATCH")
            .build();


    public void log(RestLogContainer request) throws Throwable {
        ProceedingJoinPoint joinPoint = request.getJoinPoint();
        Object result = joinPoint.proceed();

        String logFile = request.getLogFile();

        LogLevel invokeLevel = request.getLevel();
        int[] ignoreParams = request.getIgnoreParams();

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Class<?> targetClass = joinPoint.getSignature().getDeclaringType();
        Object[] args = joinPoint.getArgs();

        Annotation[] methodAnnotations = method.getAnnotations();
        String restType = Arrays.stream(methodAnnotations)
                .map(Annotation::annotationType)
                .filter(Objects::nonNull)
                .map(Class::getSimpleName)
                .map(String::toUpperCase)
                .map(annotationName -> {
                    for (String httpCrudOperation : httpCrudOperations){
                        if (annotationName.contains(httpCrudOperation)) return httpCrudOperation;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("<EMPTY>");


        String pathByClass = "";
        List<String> partPathByClass = Optional.ofNullable(method.getDeclaringClass())
                .map(x -> x.getAnnotation(RequestMapping.class))
                .map(RequestMapping::value)
                .map(Arrays::asList)
                .get();
        if (!partPathByClass.isEmpty()){
            pathByClass = partPathByClass.stream().collect(Collectors.joining());
        }

        String pathByMethod = "";
        List<String> partPathByMethod = Optional.of(method)
                .map(x -> x.getAnnotation(GetMapping.class))
                .map(GetMapping::value)
                .map(Arrays::asList)
                .get();
        if(partPathByMethod.isEmpty())
            partPathByMethod = Optional.of(method)
                    .map(x -> x.getAnnotation(PostMapping.class))
                    .map(PostMapping::value)
                    .map(Arrays::asList)
                    .get();
        if(partPathByMethod.isEmpty())
            partPathByMethod = Optional.of(method)
                    .map(x -> x.getAnnotation(PatchMapping.class))
                    .map(PatchMapping::value)
                    .map(Arrays::asList)
                    .get();
        if(partPathByMethod.isEmpty())
            partPathByMethod = Optional.of(method)
                    .map(x -> x.getAnnotation(PutMapping.class))
                    .map(PutMapping::value)
                    .map(Arrays::asList)
                    .get();
        if(partPathByMethod.isEmpty())
            partPathByMethod = Optional.of(method)
                    .map(x -> x.getAnnotation(DeleteMapping.class))
                    .map(DeleteMapping::value)
                    .map(Arrays::asList)
                    .get();
        if (!partPathByMethod.isEmpty()){
            pathByMethod = partPathByMethod.stream().collect(Collectors.joining());
        }

        String fullPath = pathByClass + pathByMethod;

        String message = createMessage(invokeLevel, restType, fullPath, targetClass, method, args, ignoreParams, result);

        FileSaver.writeToFS(logFile, message);


    }


    private String createMessage(LogLevel level,
                                 String restType,
                                 String fullPath,
                                 Class targetClass,
                                 Method method,
                                 Object[] args,
                                 int[] ignoreParams,
                                 Object result){
        return createMessage(new Date(),
                level,
                restType,
                fullPath,
                targetClass,
                method,
                args,
                ignoreParams,
                result);

    }


    private String createMessage(Date date,
                                 LogLevel level,
                                 String restType,
                                 String fullPath,
                                 Class targetClass,
                                 Method method,
                                 Object[] args,
                                 int[] ignoreParams,
                                 Object result
    ){
        StringBuffer log = new StringBuffer();
        String separator = "\t";
        log
                /* add Date */
                .append(dateFormatter.format(date))
                .append(separator)

                /* add Logger level */
                .append(level.name())
                .append(separator)

                /* add rest type */
                .append(restType)
                .append(separator)

                /* add rest path */
                .append(fullPath)
                .append(separator)

                /* add class name dot method*/
                .append(targetClass.getSimpleName())
                .append(".")
                .append(method.getName())

                /* add (params) */
//                .append(Arrays.stream(args)
//                        .map(Object::toString)
//                        .collect(Collectors.joining(", ", "(", ")"))
                .append(Arrays.stream(method.getParameters())
                        .map(x -> x.getType() + " " + x.getName())
                        .collect(Collectors.joining(", ", "(", ")"))
                )
                .append(separator);

        /* add parameters values (request and body) */
        log.append("INPUT ");
        if (args.length > 0) {

            Stream<Pair<Parameter, Object>> pair =  Streams.zip(Arrays.stream(method.getParameters()),
                    Arrays.stream(args), Pair::of);

            Stream<Integer> iterate = Stream.iterate(0, i -> i + 1).limit(args.length);

            Stream<Triple<Parameter, Object, Integer>> triple = Streams.zip(pair, iterate, (x, y) -> Triple.of(x.getKey(), x.getValue(), y));

            ToStringStyle jsonStyle = ToStringStyle.JSON_STYLE;

            triple.forEach(paramTriple -> {
                Parameter parameter = paramTriple.getLeft();
                String paramName = parameter.isNamePresent()
                        ? parameter.getName()
                        : parameter.getType().getName();

                Object rawValue = paramTriple.getMiddle();

                int i = paramTriple.getRight();
                Object arg = ArrayUtils.contains(ignoreParams, i)
                        ? rawValue == null ? null : "<IGNORED>"
                        : rawValue;

                jsonStyle.append(log, paramName, arg, true);
            });
        } else {
            log.append("<NONE> ");
        }
        log.append(separator);

        /* add return values (response) */
        log.append("OUTPUT ");
        if (result != null) {
            Stream.of(ToStringStyle.JSON_STYLE)
                    .forEach(style -> style.append(log, result.getClass().getSimpleName(), result, true));
        } else {
            log.append("<NONE> ");
        }

        return log.toString();
    }



}