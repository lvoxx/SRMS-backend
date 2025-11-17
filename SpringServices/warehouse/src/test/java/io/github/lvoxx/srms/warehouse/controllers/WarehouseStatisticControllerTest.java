package io.github.lvoxx.srms.warehouse.controllers;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.lvoxx.srms.warehouse.dto.WarehouseDTO.Response;
import io.github.lvoxx.srms.warehouse.helper.MinimalWebFluxTest;
import io.github.lvoxx.srms.warehouse.services.WarehouseStatisticService;

@DisplayName("Warehouse Statistic Controller Validation Tests")
@Tags({
                @Tag("Controller"), @Tag("Validation"), @Tag("Mock")
})
@MinimalWebFluxTest(controllers = WarehouseStatisticController.class, controllersClasses = WarehouseStatisticController.class)
@ActiveProfiles("test")
@SuppressWarnings("unused")
public class WarehouseStatisticControllerTest {

        private static final Logger log = LoggerFactory.getLogger(WarehouseStatisticControllerTest.class);

        @Autowired
        private WebTestClient webTestClient;

        @Autowired
        private ObjectMapper mapper;

        @MockitoBean
        private WarehouseStatisticService statisticService;

        private void printPrettyLog(Logger log, EntityExchangeResult<byte[]> res) {
                try {
                        Object json = mapper.readValue(res.getResponseBody(), Object.class);
                        log.debug("Response:\n{}", mapper.writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(json));
                } catch (StreamReadException e) {
                        e.printStackTrace();
                } catch (DatabindException e) {
                        e.printStackTrace();
                } catch (JsonProcessingException e) {
                        e.printStackTrace();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }

        private void printPrettyLogWithLong(Logger log, EntityExchangeResult<Long> res) {
                Long body = res.getResponseBody();

                if (body == null) {
                        log.debug("Response: null");
                        return;
                }

                try {
                        // Convert long -> pretty JSON (vd: 2 -> "2")
                        String pretty = mapper.writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(body);

                        log.debug("Response:\n{}", pretty);
                } catch (JsonProcessingException e) {
                        log.error("Error printing response", e);
                }
        }

        private void printPrettyDTOLog(Logger log, EntityExchangeResult<Response> res) {
                try {
                        log.debug("Response:\n{}", mapper.writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(res));
                } catch (JsonProcessingException e) {
                        e.printStackTrace();
                }
        }
}
