package com.daella.hospital_management_system.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that provides:
 * <ul>
 *   <li>Method entry/exit logging for all service classes</li>
 *   <li>Execution time measurement for service methods</li>
 * </ul>
 *
 * Sensitive fields are never logged — only method names and arg counts.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /** Pointcut for all methods in the service.impl package. */
    @Pointcut("execution(* com.daella.hospital_management_system.service.impl.*.*(..))")
    public void serviceLayer() {}

    /** Pointcut for all methods in the controller package. */
    @Pointcut("execution(* com.daella.hospital_management_system.controller.*.*(..))")
    public void controllerLayer() {}

    // ── @Before ─────────────────────────────────────────────────────────────

    @Before("serviceLayer()")
    public void logBeforeService(JoinPoint joinPoint) {
        String method = joinPoint.getSignature().toShortString();
        int argCount = joinPoint.getArgs().length;
        log.debug("[SERVICE] → {} called with {} argument(s)", method, argCount);
    }

    @Before("controllerLayer()")
    public void logBeforeController(JoinPoint joinPoint) {
        String method = joinPoint.getSignature().toShortString();
        log.debug("[CONTROLLER] → {} invoked", method);
    }

    // ── @After ──────────────────────────────────────────────────────────────

    @After("serviceLayer()")
    public void logAfterService(JoinPoint joinPoint) {
        log.debug("[SERVICE] ← {} completed", joinPoint.getSignature().toShortString());
    }

    // ── @Around (Performance Monitoring) ────────────────────────────────────

    @Around("serviceLayer()")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > 500) {
                log.warn("[PERFORMANCE] {} took {}ms — consider optimisation", method, elapsed);
            } else {
                log.debug("[PERFORMANCE] {} completed in {}ms", method, elapsed);
            }
            return result;
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[PERFORMANCE] {} threw {} after {}ms",
                    method, ex.getClass().getSimpleName(), elapsed);
            throw ex;
        }
    }

    // ── Exception Logging ────────────────────────────────────────────────────

    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        log.error("[ERROR] Exception in {}: {} — {}",
                joinPoint.getSignature().toShortString(),
                ex.getClass().getSimpleName(),
                ex.getMessage());
    }
}
