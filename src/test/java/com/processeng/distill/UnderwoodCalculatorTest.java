package com.processeng.distill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Underwood 法的独立交叉校验：泡点进料（q=1）时，Rmin 必须与 McCabe-Thiele
 * 图解法（精馏段操作线过进料点 (zF, y*(zF))）的解析结果一致。
 */
class UnderwoodCalculatorTest {

    @Test
    void underwood_matchesMcCabeAnalyticForm_forBubblePointFeed() {
        double alpha = 2.5;
        double q = 1.0;

        // 算例一：zF=0.5, xD=0.95 -> Rmin=1.1
        assertEquals(1.1,
                TestSupport.underwood().minimumRefluxRatio(alpha, 0.5, 0.95, q), 1e-9);

        // 算例二：zF=0.4, xD=0.9, alpha=2 -> Rmin=23/12 ≈ 1.91667
        double rMin2 = TestSupport.underwood().minimumRefluxRatio(2.0, 0.4, 0.9, q);
        assertEquals(mccabeBubblePointRmin(2.0, 0.4, 0.9), rMin2, 1e-9);
        assertEquals(23.0 / 12.0, rMin2, 1e-9);

        // 算例三：zF=0.6, xD=0.9, alpha=3
        double rMin3 = TestSupport.underwood().minimumRefluxRatio(3.0, 0.6, 0.9, q);
        assertEquals(mccabeBubblePointRmin(3.0, 0.6, 0.9), rMin3, 1e-9);
    }

    /**
     * 泡点进料图解法：操作线过 (zF, y*(zF)) 与 (xD, xD)，
     * slope = (xD - y*)/(xD - zF)，R = slope/(1 - slope)。
     */
    private static double mccabeBubblePointRmin(double alpha, double zF, double xD) {
        double yEquilibrium = alpha * zF / (1.0 + (alpha - 1.0) * zF);
        double slope = (xD - yEquilibrium) / (xD - zF);
        return slope / (1.0 - slope);
    }
}
