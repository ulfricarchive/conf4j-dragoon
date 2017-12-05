package com.ulfric.dragoon.conf4j;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.util.concurrent.TimeUnit;

@Retention(RUNTIME)
public @interface Reload {

	boolean never() default false;

	long period() default 15L;

	TimeUnit unit() default TimeUnit.SECONDS;

}
