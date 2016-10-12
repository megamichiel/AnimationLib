package me.megamichiel.animationlib.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ArrayInfo {

    int min() default 0;
    int max() default 0;
    String[] def() default {};
}
