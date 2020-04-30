package com.hroniko.weblog.weblogger.aspects;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;

import com.hroniko.weblog.persistence.FileSaver;
import com.hroniko.weblog.weblogger.entities.RestLogContainer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.logging.LogLevel;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class RestLogProducer {

    private static final SimpleDateFormat dateFormatter =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    private static final ImmutableList<String> httpCrudOperations = ImmutableList.<String>builder()
            .add("GET", "HEAD", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS", "TRACE", "PATCH")
            .build();


    public void log(RestLogContainer container) {
        JoinPoint joinPoint = container.getJoinPoint();
        Object result = container.getResult();

        String logFile = container.getLogFile();

        int[] ignoreParams = container.getIgnoreParams();

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

        HttpServletRequest request = container.getRequest();
        HttpServletResponse response = container.getResponse();
        Throwable exception = container.getException();

        String message = createMessage(restType, fullPath, targetClass, method, args, ignoreParams, result, request, response, exception);

        FileSaver.writeToFS(logFile, message);


    }


    private String createMessage(String restType,
                                 String fullPath,
                                 Class targetClass,
                                 Method method,
                                 Object[] args,
                                 int[] ignoreParams,
                                 Object result,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 Throwable exception){
        return createMessage(new Date(),
                restType,
                fullPath,
                targetClass,
                method,
                args,
                ignoreParams,
                result,
                request,
                response,
                exception);

    }


    private String createMessage(Date date,
                                 String restType,
                                 String fullPath,
                                 Class targetClass,
                                 Method method,
                                 Object[] args,
                                 int[] ignoreParams,
                                 Object result,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 Throwable exception
    ){
        StringBuffer log = new StringBuffer();
        String separator = "\t";
        log
                /* add Date */
                .append(dateFormatter.format(date))
                .append(separator)

                /* add rest type */
                .append(restType)
                .append(separator);

        /* add response status code */
        if (exception == null){
            log.append(200);
        } else {
            log.append(500);
        }
        log.append(separator);

        /* add rest path */
        if (request != null){
            log.append(getUri(request));
        } else {
            log.append(fullPath);
        }
        log.append(separator);


        /* add class name dot method*/
        log.append(targetClass.getName())
                .append("#")
                .append(method.getName())
                .append(Arrays.stream(method.getParameters())
                        .map(x -> x.getType().getSimpleName() + " " + x.getName())
                        .map(x -> x.replace("class ", "")
                                .replace("interface ", ""))
                        .collect(Collectors.joining(", ", "(", ")"))
                )
                .append(separator);

        /* add parameters values (request and body) */
        log.append("INPUT ");
        if (args.length == 0
                || (args.length == 1 && request != null)
                || (args.length == 1 && response != null)
                || (args.length == 2 && request != null && response != null)

        ){
            log.append("<NONE> ");
        } else {
            Stream<Pair<Parameter, Object>> pair =  Streams.zip(Arrays.stream(method.getParameters()),
                    Arrays.stream(args), Pair::of);

            Stream<Integer> iterate = Stream.iterate(0, i -> i + 1).limit(args.length);

            Stream<Triple<Parameter, Object, Integer>> triple = Streams.zip(pair, iterate, (x, y) -> Triple.of(x.getKey(), x.getValue(), y));

            ToStringStyle jsonStyle = ToStringStyle.JSON_STYLE;

            for (Triple<Parameter, Object, Integer> paramTriple : triple.collect(Collectors.toList())){
                Parameter parameter = paramTriple.getLeft();
                String paramName = parameter.isNamePresent()
                        ? parameter.getName()
                        : parameter.getType().getName();

                Object rawValue = paramTriple.getMiddle();

                int i = paramTriple.getRight();
                Object arg = ArrayUtils.contains(ignoreParams, i)
                        ? rawValue == null ? null : "<IGNORED>"
                        : rawValue;

                if (!(arg instanceof HttpServletRequest) && !(arg instanceof HttpServletResponse)){
                    jsonStyle.append(log, paramName, arg, true);
                }
            }
        }
        log.append(separator);

        Triple<String, String, String> clientHostInfo = getClientHostInfo(request);

        log.append("CLIENT ");
        log.append(clientHostInfo.getLeft());
        log.append(" | ");

        log.append(clientHostInfo.getMiddle());
        log.append(separator);

        /* add return values (response) */
        log.append("OUTPUT ");
        if (result != null) {
            Stream.of(ToStringStyle.JSON_STYLE)
                    .forEach(style -> style.append(log, result.getClass().getSimpleName(), result, true));
        } else {
            log.append("<NONE> ");
        }
        log.append(separator);

        /* add error tracing */
        log.append("ERROR ");
        if (exception != null){
            Stream.of(ToStringStyle.JSON_STYLE)
                    .forEach(style -> style.append(log, "exception", ExceptionUtils.getStackTrace(exception), true));
        } else {
            log.append("<NONE> ");
        }

        return log.toString();
    }

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR" };

    public static String getClientIpAddress(HttpServletRequest request) {
        String cIp = null;
        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                cIp = ip;
                break;
            }
        }
        if (cIp == null) cIp = request.getRemoteAddr();
        if (cIp == null) return "127.0.0.1";
        if (cIp.equals("0:0:0:0:0:0:0:1")) return "127.0.0.1";
        return cIp;
    }

    public static String getClientHost(HttpServletRequest request) {
        String host = request.getRemoteHost();
        if (host.equals("0:0:0:0:0:0:0:1") || host.equals("127.0.0.1") ) return "localhost";
        return host;
    }

    public static Triple<String, String, String> getClientHostInfo(HttpServletRequest request){
        if (request == null) return Triple.of("<NONE>", "<NONE>", "<NONE>");
        String hostName = null;
        String hostIp = null;
        String hostPort = null;
        String host = request.getHeader("host");
        if (host != null){
            String[] partHost = host.split(":");

            if (partHost.length == 0){
                host = null;
            }
            if (partHost.length > 0){
                String tmp = partHost[0];
                if (Pattern.matches("([0-9]*[.:])*[0-9]+", tmp)){
                    hostIp = tmp;
                } else {
                    hostName = tmp;
                }
                if (partHost.length > 1) {
                    hostPort = partHost[1];
                }
            }

        }
        if (hostName == null && hostIp != null){
            if (hostIp.equals("127.0.0.1") || hostIp.equals("0.0.0.0")){
                hostName = "localhost";
            } else {
                hostName = getClientHost(request);
            }
        }

        if (hostName != null && hostIp == null){
            if (hostName.equals("localhost")){
                hostIp = "127.0.0.1";
            } else {
                hostIp = getClientIpAddress(request);
            }
        }

        if (hostName != null && hostIp != null) {
            hostName = getClientHost(request);
            hostIp = getClientIpAddress(request);
        }

        if (hostPort == null){
            hostPort = "80";
        }

        return Triple.of(hostName, hostIp, hostPort);
    }

    public static String getUri(HttpServletRequest request){
        return request.getScheme() + "://" +
                request.getServerName() +
                ("http".equals(request.getScheme()) && request.getServerPort() == 80 || "https".equals(request.getScheme()) && request.getServerPort() == 443 ? "" : ":" + request.getServerPort() ) +
                request.getRequestURI() +
                (request.getQueryString() != null ? "?" + request.getQueryString() : "");
    }



}