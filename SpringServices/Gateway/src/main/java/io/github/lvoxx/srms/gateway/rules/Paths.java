package io.github.lvoxx.srms.gateway.rules;

import java.util.Collections;
import java.util.Set;

import org.springframework.http.HttpMethod;

public enum Paths {
    CUSTOMERS("/customers/**", Set.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)),
    CONTACTOR("/contactors/**", Set.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE));

    private final String pattern;
    private final Set<HttpMethod> methods;

    Paths(String pattern, Set<HttpMethod> methods) {
        this.pattern = pattern;
        this.methods = Collections.unmodifiableSet(methods);
    }

    public String getPattern() {
        return pattern;
    }

    public Set<HttpMethod> getMethods() {
        return methods;
    }
}