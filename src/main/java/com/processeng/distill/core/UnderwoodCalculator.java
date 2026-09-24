package com.processeng.distill.core;

import org.springframework.stereotype.Component;

/**
 * Underwood 法求最小回流比（二元体系、恒相对挥发度、恒摩尔流假设）。
 *
 * <p>第一步，在区间 theta in (1, alpha) 内求 Underwood 方程的根：
 * <pre>
 *    alpha*zF/(alpha - theta) + (1 - zF)/(1 - theta) = 1 - q
 * </pre>
 * 左端在 (1, alpha) 上由 -infinity 单调递增到 +infinity，根唯一，用二分法稳健求解。
 *
 * <p>第二步求最小回流比：
 * <pre>
 *    Rmin + 1 = alpha*xD/(alpha - theta) + (1 - xD)/(1 - theta)
 * </pre>
 * q 为进料热状态参数（q=1 泡点液体，q=0 饱和蒸汽，q&gt;1 过冷液体）。
 */
@Component
public class UnderwoodCalculator {

    private static final int MAX_ITERATIONS = 200;
    private static final double BRACKET_EPS = 1e-12;

    public double minimumRefluxRatio(double relativeVolatility,
                                     double feedComposition,
                                     double distillateComposition,
                                     double feedQuality) {
        double theta = solveTheta(relativeVolatility, feedComposition, feedQuality);
        return relativeVolatility * distillateComposition / (relativeVolatility - theta)
                + (1.0 - distillateComposition) / (1.0 - theta)
                - 1.0;
    }

    private double solveTheta(double alpha, double zF, double q) {
        double target = 1.0 - q;
        double lo = 1.0 + BRACKET_EPS;
        double hi = alpha - BRACKET_EPS;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double mid = 0.5 * (lo + hi);
            double value = alpha * zF / (alpha - mid)
                    + (1.0 - zF) / (1.0 - mid);
            if (value < target) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return 0.5 * (lo + hi);
    }
}
