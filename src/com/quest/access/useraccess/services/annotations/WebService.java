package com.quest.access.useraccess.services.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author CONSTANT
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WebService {
    String name();  // the message name
    int level() default 0;// the services that can share this method without privileges
    String privileged() default "no";
}
