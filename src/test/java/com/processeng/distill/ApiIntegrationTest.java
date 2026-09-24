package com.processeng.distill;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 走真实 HTTP 栈的端到端测试：结构化参数进、结构化结果出，错误同样走结构化响应。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegrationTest {

    private static final String PATH = "/api/v1/distillation/shortcut";

    @Autowired
    private TestRestTemplate rest;

    private Map<String, Object> body(double f, double zF, double xD, double xB,
                                    double q, double alpha, double r) {
        return Map.of(
                "feedRate", f,
                "feedComposition", zF,
                "distillateComposition", xD,
                "bottomsComposition", xB,
                "feedQuality", q,
                "relativeVolatility", alpha,
                "refluxRatio", r);
    }

    @Test
    void validRequest_returnsAllThreeResultsAndClosedBalance() {
        ResponseEntity<JsonNode> resp = post(body(100, 0.5, 0.95, 0.05, 1.0, 2.5, 2.0));

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JsonNode json = resp.getBody();

        assertEquals(1.1, json.path("minimumRefluxRatio").asDouble(), 1e-9);
        assertEquals(Math.log(361.0) / Math.log(2.5),
                json.path("minimumTheoreticalStages").asDouble(), 1e-9);

        JsonNode balance = json.path("materialBalance");
        assertEquals(50.0, balance.path("distillateRate").asDouble(), 1e-9);
        assertEquals(50.0, balance.path("bottomsRate").asDouble(), 1e-9);
        assertTrue(balance.path("residual").asDouble() < 1e-7);

        JsonNode stepping = json.path("stepping");
        assertEquals(11, stepping.path("totalStages").asInt());
        assertEquals(5, stepping.path("feedStage").asInt());
        assertEquals(2.0 / 3.0, stepping.path("rectifyingLine").path("slope").asDouble(), 1e-12);
        assertEquals(4.0 / 3.0, stepping.path("strippingLine").path("slope").asDouble(), 1e-12);
        assertEquals(11, stepping.path("stages").size());

        JsonNode first = stepping.path("stages").get(0);
        assertEquals(0.95, first.path("vaporIn").asDouble(), 0.0);
        assertEquals("RECTIFYING", first.path("section").asText());
        assertEquals("STRIPPING", stepping.path("stages").get(4).path("section").asText());
        JsonNode last = stepping.path("stages").get(10);
        assertTrue(last.path("liquidComposition").asDouble() <= 0.05);
    }

    @Test
    void nearTotalReflux_stageCountHugsFenske() {
        ResponseEntity<JsonNode> resp = post(body(100, 0.5, 0.95, 0.05, 1.0, 2.5, 1.0e7));
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        int stages = resp.getBody().path("stepping").path("totalStages").asInt();
        double nMin = resp.getBody().path("minimumTheoreticalStages").asDouble();
        assertTrue(Math.abs(stages - nMin) <= 1.0);
        assertEquals(7, stages);
    }

    @Test
    void compositionOutOfRange_is400ValidationError() {
        ResponseEntity<JsonNode> resp = post(body(100, 0.5, 1.5, 0.05, 1.0, 2.5, 2.0));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("VALIDATION_ERROR", resp.getBody().path("code").asText());
        assertTrue(resp.getBody().path("details").size() > 0);
    }

    @Test
    void nonPositiveFeedRate_is400() {
        ResponseEntity<JsonNode> resp = post(body(0, 0.5, 0.95, 0.05, 1.0, 2.5, 2.0));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("VALIDATION_ERROR", resp.getBody().path("code").asText());
    }

    @Test
    void nonPositiveVolatility_is400() {
        ResponseEntity<JsonNode> resp = post(body(100, 0.5, 0.95, 0.05, 1.0, -2.0, 2.0));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("VALIDATION_ERROR", resp.getBody().path("code").asText());
    }

    @Test
    void volatilityBelowOne_is422CannotSeparate() {
        ResponseEntity<JsonNode> resp = post(body(100, 0.5, 0.95, 0.05, 1.0, 0.8, 2.0));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertEquals("SEPARATION_INFEASIBLE", resp.getBody().path("code").asText());
        assertTrue(resp.getBody().path("message").asText().contains("无法分离"));
    }

    @Test
    void compositionOrderReversed_is422() {
        ResponseEntity<JsonNode> resp = post(body(100, 0.5, 0.40, 0.05, 1.0, 2.5, 2.0));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertEquals("COMPOSITION_ORDER_INVALID", resp.getBody().path("code").asText());
    }

    @Test
    void refluxBelowMinimum_is422AndNeverRunsStages() {
        // 该算例 Rmin=1.1，给 R=1.0 必须报「回流不足」，且响应里不含正常板数
        ResponseEntity<JsonNode> resp = post(body(100, 0.5, 0.95, 0.05, 1.0, 2.5, 1.0));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertEquals("REFLUX_BELOW_MINIMUM", resp.getBody().path("code").asText());
        assertTrue(resp.getBody().path("message").asText().contains("回流不足"));
    }

    @Test
    void malformedJson_is400() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<String> entity =
                new org.springframework.http.HttpEntity<>("{not-json", headers);
        ResponseEntity<String> resp = rest.postForEntity(PATH, entity, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    private ResponseEntity<JsonNode> post(Map<String, Object> requestBody) {
        return rest.postForEntity(PATH, requestBody, JsonNode.class);
    }
}
