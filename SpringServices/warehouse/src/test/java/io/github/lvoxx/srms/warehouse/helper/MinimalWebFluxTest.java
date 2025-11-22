package io.github.lvoxx.srms.warehouse.helper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ContextConfiguration;

import io.github.lvoxx.srms.controllerhandler.controller.GlobalExceptionHandler;
import io.github.lvoxx.srms.controllerhandler.controller.ValidationExceptionHandler;
import io.github.lvoxx.srms.warehouse.config.TestControllerWithMessagesConfig;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebFluxTest(excludeAutoConfiguration = {
                R2dbcAutoConfiguration.class,
                R2dbcDataAutoConfiguration.class,
                R2dbcRepositoriesAutoConfiguration.class,
                WebSocketReactiveAutoConfiguration.class
})
@ContextConfiguration
@Import({
                TestControllerWithMessagesConfig.class,
                GlobalExceptionHandler.class,
                ValidationExceptionHandler.class
})
public @interface MinimalWebFluxTest {
        @AliasFor(annotation = WebFluxTest.class, attribute = "controllers")
        Class<?>[] controllers() default {};

        @AliasFor(annotation = ContextConfiguration.class, attribute = "classes")
        Class<?>[] controllersClasses() default {};
}