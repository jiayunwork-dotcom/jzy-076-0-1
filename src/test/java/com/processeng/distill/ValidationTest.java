package com.processeng.distill;

import com.processeng.distill.api.dto.DistillationRequest;
import com.processeng.distill.error.DistillationException;
import com.processeng.distill.error.ErrorCode;
import com.processeng.distill.service.DistillationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 边界与非法输入：全部拒绝，并给出可机读的结构化错误码。
 */
class ValidationTest {

    private final DistillationService service = TestSupport.service();

    static Stream<Arguments> invalidCompositions() {
        return Stream.of(
                // 组成不在 (0,1)
                Arguments.of(100.0, 0.5, 1.2, 0.05, 1.0, 2.5, 2.0),
                Arguments.of(100.0, 0.5, 0.95, -0.1, 1.0, 2.5, 2.0),
                Arguments.of(100.0, 0.0, 0.95, 0.05, 1.0, 2.5, 2.0),
                Arguments.of(100.0, 1.0, 0.95, 0.05, 1.0, 2.5, 2.0));
    }

    @ParameterizedTest
    @MethodSource("invalidCompositions")
    void compositionsOutsideOpenUnitInterval_areRejected(
            double f, double zF, double xD, double xB, double q, double alpha, double r) {
        DistillationException ex = assertThrows(DistillationException.class,
                () -> service.calculate(TestSupport.request(f, zF, xD, xB, q, alpha, r)));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.code());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "0.0", "-50.0"
    })
    void nonPositiveFeedRate_isRejected(double feedRate) {
        DistillationException ex = assertThrows(DistillationException.class,
                () -> service.calculate(
                        TestSupport.request(feedRate, 0.5, 0.95, 0.05, 1.0, 2.5, 2.0)));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.code());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(doubles = {0.0, -2.0, -0.01})
    void nonPositiveRelativeVolatility_isRejected(double alpha) {
        DistillationException ex = assertThrows(DistillationException.class,
                () -> service.calculate(
                        TestSupport.request(100.0, 0.5, 0.95, 0.05, 1.0, alpha, 2.0)));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.code());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(doubles = {0.5, 0.999, 1.0})
    void volatilityAtOrBelowOne_meansCannotSeparate(double alpha) {
        DistillationException ex = assertThrows(DistillationException.class,
                () -> service.calculate(
                        TestSupport.request(100.0, 0.5, 0.95, 0.05, 1.0, alpha, 2.0)));
        assertEquals(ErrorCode.SEPARATION_INFEASIBLE, ex.code());
        assertEquals("无法分离：相对挥发度必须大于 1（当前 alpha=" + alpha + "）", ex.getMessage());
    }

    @Test
    void compositionOrder_xDBelowFeed_isRejected() {
        // xD=0.4 < zF=0.5
        DistillationException ex = assertThrows(DistillationException.class,
                () -> service.calculate(
                        TestSupport.request(100.0, 0.5, 0.4, 0.05, 1.0, 2.5, 2.0)));
        assertEquals(ErrorCode.COMPOSITION_ORDER_INVALID, ex.code());
    }

    @Test
    void compositionOrder_feedBelowBottoms_isRejected() {
        // zF=0.04 < xB=0.05
        DistillationException ex = assertThrows(DistillationException.class,
                () -> service.calculate(
                        TestSupport.request(100.0, 0.04, 0.95, 0.05, 1.0, 2.5, 2.0)));
        assertEquals(ErrorCode.COMPOSITION_ORDER_INVALID, ex.code());
    }

    @Test
    void compositionOrder_equalAdjacentCompositions_isRejected() {
        DistillationException ex = assertThrows(DistillationException.class,
                () -> service.calculate(
                        TestSupport.request(100.0, 0.5, 0.5, 0.05, 1.0, 2.5, 2.0)));
        assertEquals(ErrorCode.COMPOSITION_ORDER_INVALID, ex.code());
    }
}
