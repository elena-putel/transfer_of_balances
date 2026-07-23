package com.beltelecom.transfer.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("within(com.beltelecom.transfer.service..*)")
    public void serviceLayer() {
    }

    @Around("serviceLayer()")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();
        log.info(">>> Вызов: {}", methodName);
        try {
            Object result = joinPoint.proceed();
            log.info("<<< Завершено: {} за {} ms", methodName, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.error("!!! Ошибка в {} за {} ms: {}", methodName, System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }
}
