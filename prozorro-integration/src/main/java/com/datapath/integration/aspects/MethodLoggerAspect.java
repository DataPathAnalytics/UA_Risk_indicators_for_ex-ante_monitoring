package com.datapath.integration.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class MethodLoggerAspect {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

//    @Before("execution(* ProzorroTenderLoaderService.*(..))")
    public void before(JoinPoint joinPoint){
        final String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        final String methodName = joinPoint.getSignature().getName();
        final String args = Arrays.stream(joinPoint.getArgs()).map(Object::toString)
                .collect(Collectors.joining("\t"));
        logger.info("[{}]\t[{}]  {}", className, methodName, (args.isEmpty() ? args : "<< " + args));
    }

//    @AfterReturning(value = "execution(* ProzorroTenderLoaderService.*(..))", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result){
        final String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        final String methodName = joinPoint.getSignature().getName();
        logger.info("[{}]\t[{}]  >>  {}", className, methodName, (result != null ? result.toString() : null));
    }
}
