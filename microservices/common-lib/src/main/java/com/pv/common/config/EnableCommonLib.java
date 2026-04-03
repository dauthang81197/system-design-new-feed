package com.pv.common.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Đặt annotation này trên Application class để tự động load toàn bộ common config:
 * Security (JWT), Redis, S3, Swagger, GlobalExceptionHandler.
 *
 * <pre>
 * {@code
 * @SpringBootApplication
 * @EnableCommonLib
 * public class YourServiceApplication { ... }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
    SecurityConfig.class,
    RedisConfig.class,
    S3Config.class,
    SwaggerConfig.class,
    com.pv.common.exception.GlobalExceptionHandler.class
})
public @interface EnableCommonLib {
}

