package com.processeng.distill;

import com.processeng.distill.api.dto.DistillationRequest;
import com.processeng.distill.error.DistillationException;
import com.processeng.distill.error.ErrorCode;
import com.processeng.distill.service.DistillationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 判据 4：回流比等于或低于最小回流比时必须报「回流不足」，
 * 不能继续吐出一个看似正常的板数。
 */
class RefluxBelowMinimumTest {

    private final DistillationService service = TestSupport.service();
    private final double rMin = TestSupport.underwood()
            .minimumRefluxRatio(TestSupport.ALPHA, TestSupport.ZF, TestSupport.XD, TestSupport.Q_BUBBLE);

    @Test
    void rmin_hasExpectedAnalyticValue() {
        assertEquals(TestSupport.EXPECTED_RMIN, rMin, 1e-9);
    }

    @Test
    void refluxExactlyAtMinimum_isRejected() {
        DistillationRequest atMinimum = TestSupport.standardCase(rMin);
        DistillationException ex = assertThrows(DistillationException.class,
                () -> service.calculate(atMinimum));
        assertEquals(ErrorCode.REFLUX_BELOW_MINIMUM, ex.code());
    }

    @Test
    void refluxBelowMinimum_isRejected() {
        for (double r : new double[]{0.5, 0.9, 1.0, rMin * 0.999, rMin - 1e-6}) {
            DistillationException ex = assertThrows(DistillationException.class,
                    () -> service.calculate(TestSupport.standardCase(r)),
                    "R=" + r + " 应被拒绝");
            assertEquals(ErrorCode.REFLUX_BELOW_MINIMUM, ex.code());
        }
    }

    @Test
    void refluxJustAboveMinimum_isAccepted() {
        double r = rMin * (1.0 + 1e-5);
        assertDoesNotThrow(() -> service.calculate(TestSupport.standardCase(r)));
    }
}
