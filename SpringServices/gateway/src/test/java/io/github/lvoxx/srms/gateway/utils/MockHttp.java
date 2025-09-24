package io.github.lvoxx.srms.gateway.utils;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.mockserver.client.MockServerClient;
import org.mockserver.model.Delay;
import org.springframework.http.HttpMethod;

import io.github.lvoxx.srms.gateway.constants.Api2Test;

public abstract class MockHttp {
        public static void callWithCode(MockServerClient client, HttpMethod method, Api2Test.SubPath path2Test,
                        Integer returnedCode) {
                client.when(request().withMethod(method.name()).withPath(Api2Test.SubPath.TEST.getPath()))
                                .respond(response().withStatusCode(returnedCode));
        }

        public static void callWithTimeout(MockServerClient client, HttpMethod method, String path2Test,
                        Integer returnedCode,
                        Duration duration) {
                client.when(request()
                                .withMethod(method.name())
                                .withPath(path2Test))
                                .respond(
                                                response()
                                                                .withStatusCode(returnedCode)
                                                                .withDelay(new Delay(TimeUnit.MILLISECONDS,
                                                                                duration.toMillis())));
        }
}
