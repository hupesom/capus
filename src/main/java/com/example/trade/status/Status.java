package com.example.trade.status;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 自定义校验注解：限定 status 字段的取值。
 * 用法：@Status(values = {1,2,3,4})
 *
 * @author maofan
 * @create 2026-10-26 10:36
 */
@Documented
@Constraint(validatedBy = {StatusValidation.class})
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Status {
    int[] values();

    String message() default "status 的取值不合法";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
