package me.xiaoying.sproxy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SFieldMethod {
    String fieldName();

    Type type();

    enum Type {
        GETTER,
        SETTER;
    }
}