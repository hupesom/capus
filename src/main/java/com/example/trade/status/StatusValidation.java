package com.example.trade.status;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 自定义校验器：校验 status 字段的取值范围。
 *
 * 修：原来泛型写的是 <Status, String>，但 Goods.status / Orders.status 是 Integer。
 * 校验器泛型必须和字段类型一致，否则 Hibernate Validator 运行时抛
 * UnexpectedTypeException："No validator could be found for constraint"。
 *
 * @author maofan
 * @create 2026-10-26 10:35
 */
public class StatusValidation implements ConstraintValidator<Status, Integer> {

    private int[] values;

    @Override
    public void initialize(Status constraintAnnotation) {
        this.values = constraintAnnotation.values();
    }

    @Override
    public boolean isValid(Integer s, ConstraintValidatorContext constraintValidatorContext) {
        if (s == null) {
            return false;
        }
        for (int value : values) {
            if (value == s) {
                return true;
            }
        }
        return false;
    }
}
