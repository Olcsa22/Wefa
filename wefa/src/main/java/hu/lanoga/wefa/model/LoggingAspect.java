package hu.lanoga.wefa.model;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("execution(* hu.lanoga.wefa..*.*(..))")
    public void logMethod() {}

    @Before("logMethod()")
    public void log(JoinPoint joinPoint) {
        LOGGER.info("Method '{}' called with arguments {}", joinPoint.getSignature().toShortString(), Arrays.toString(joinPoint.getArgs()));
    }
}
