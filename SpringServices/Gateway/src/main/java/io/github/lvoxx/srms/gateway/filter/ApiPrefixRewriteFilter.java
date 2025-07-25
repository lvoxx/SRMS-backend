package io.github.lvoxx.srms.gateway.filter;

import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class ApiPrefixRewriteFilter implements RewriteFunction<String, String> {

    @Override
    public Mono<String> apply(ServerWebExchange exchange, String body) {
        String path = exchange.getRequest().getPath().toString();
        if (path.startsWith("/api/")) {
            String newPath = path.replaceFirst("/api/[^/]+/", "/");
            exchange.getAttributes().put("rewritten_path", newPath);
        }
        return Mono.just(body);
    }
}