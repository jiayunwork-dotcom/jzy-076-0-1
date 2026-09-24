package com.processeng.distill;

import com.processeng.distill.api.dto.DistillationResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 判据 5：进料热状态 q 改变时，提馏段操作线斜率必须随之改变，
 * 但物料衡算闭合关系（F、D、B 与残差）不受影响。
 */
class FeedConditionStrippingSlopeTest {

    @ParameterizedTest
    @ValueSource(doubles = {0.5, 1.0, 1.5})
    void materialBalance_staysClosed_forAnyFeedQuality(double q) {
        var service = TestSupport.service();

        DistillationResponse response = service.calculate(
                TestSupport.request(100.0, 0.5, 0.95, 0.05, q, 2.5, 2.0));

        var balance = response.materialBalance();
        assertEquals(100.0, balance.feedRate(), 0.0);
        assertEquals(50.0, balance.distillateRate(), 1e-9);
        assertEquals(50.0, balance.bottomsRate(), 1e-9);
        assertEquals(100.0, balance.distillateRate() + balance.bottomsRate(), 1e-9);
        assertTrue(balance.residual() <= 1e-9 * 100.0,
                "q=" + q + " 时衡算残差 " + balance.residual() + " 超容差");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.5, 1.0, 1.5})
    void rectifyingLine_isUnchanged_butStrippingSlopeFollowsQ(double q) {
        var service = TestSupport.service();

        DistillationResponse response = service.calculate(
                TestSupport.request(100.0, 0.5, 0.95, 0.05, q, 2.5, 2.0));

        var rect = response.stepping().rectifyingLine();
        var strip = response.stepping().strippingLine();

        // 精馏段操作线只取决于 R 与 xD，与 q 无关：斜率恒为 2/3
        assertEquals(2.0 / 3.0, rect.slope(), 1e-12);
        assertEquals(0.95 / 3.0, rect.intercept(), 1e-12);

        // 手工解析值：
        // q=0.5 -> L'=150, V'=100, 斜率 1.50
        // q=1.0 -> L'=200, V'=150, 斜率 4/3
        // q=1.5 -> L'=250, V'=200, 斜率 1.25
        double expectedSlope = switch (String.format(java.util.Locale.ROOT, "%.1f", q)) {
            case "0.5" -> 1.50;
            case "1.0" -> 4.0 / 3.0;
            case "1.5" -> 1.25;
            default -> throw new IllegalStateException("unexpected q " + q);
        };
        assertEquals(expectedSlope, strip.slope(), 1e-9,
                "q=" + q + " 时提馏段斜率不符");
    }

    @org.junit.jupiter.api.Test
    void strippingSlopes_differAcrossQ_andMinimumRefluxChanges() {
        var service = TestSupport.service();

        double slopeQ05 = slopeAt(0.5);
        double slopeQ10 = slopeAt(1.0);
        double slopeQ15 = slopeAt(1.5);

        assertTrue(Math.abs(slopeQ05 - slopeQ10) > 1e-6);
        assertTrue(Math.abs(slopeQ10 - slopeQ15) > 1e-6);
        // q 越大（进料带下来的液相越多），提馏段斜率越小
        assertTrue(slopeQ05 > slopeQ10 && slopeQ10 > slopeQ15);

        double rMinQ05 = TestSupport.underwood().minimumRefluxRatio(2.5, 0.5, 0.95, 0.5);
        double rMinQ15 = TestSupport.underwood().minimumRefluxRatio(2.5, 0.5, 0.95, 1.5);
        assertNotEquals(rMinQ05, rMinQ15, 1e-9, "不同 q 下 Rmin 应有区别");
    }

    private static double slopeAt(double q) {
        return TestSupport.service().calculate(
                        TestSupport.request(100.0, 0.5, 0.95, 0.05, q, 2.5, 2.0))
                .stepping().strippingLine().slope();
    }
}
