package com.processeng.distill;

import com.processeng.distill.core.MaterialBalance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 判据 1：任何一组合法输入，物料衡算残差都必须落在容差内，且 D + B = F。
 */
class MaterialBalanceTest {

    static Stream<Arguments> validCases() {
        return Stream.of(
                Arguments.of(100.0, 0.50, 0.95, 0.05),
                Arguments.of(83.5, 0.40, 0.97, 0.02),
                Arguments.of(1.0, 0.35, 0.80, 0.05),
                Arguments.of(250.7, 0.60, 0.99, 0.01),
                Arguments.of(42.0, 0.10, 0.33, 0.001),
                Arguments.of(0.01, 0.50, 0.90, 0.10));
    }

    @ParameterizedTest
    @MethodSource("validCases")
    void closure_residualIsWithinTolerance_andFlowsAddUp(double f, double zF, double xD, double xB) {
        MaterialBalance balance = TestSupport.materialBalance();

        MaterialBalance.Result result = balance.solve(f, zF, xD, xB);

        assertTrue(balance.isClosed(result),
                "衡算残差 " + result.residual() + " 超出容差");

        // 总物料闭合
        assertEquals(f, result.distillateRate() + result.bottomsRate(),
                1e-9 * Math.max(1.0, f), "D + B 必须等于 F");

        // 与解析公式一致
        double expectedD = f * (zF - xB) / (xD - xB);
        assertEquals(expectedD, result.distillateRate(), 1e-12 * f, "D 与解析公式不符");
        assertEquals(f - expectedD, result.bottomsRate(), 1e-12 * f, "B 与解析公式不符");

        // 轻组分衡算独立复核
        double componentIn = f * zF;
        double componentOut = result.distillateRate() * xD + result.bottomsRate() * xB;
        assertEquals(componentIn, componentOut, 1e-9 * f, "轻组分进、出量必须相等");
    }
}
