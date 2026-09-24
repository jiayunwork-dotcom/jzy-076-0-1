package com.processeng.distill;

import com.processeng.distill.api.dto.DistillationResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 判据 2：实际回流比取足够大的值逼近全回流时，逐板板数必须贴着 Fenske 最少板数。
 * 阶梯是离散取整的，允许差一块板，再多就不行。
 */
class FenskeTotalRefluxTest {

    /** “足够大的 R”：操作线与对角线（全回流操作线 y=x）的差别在 1e-7 量级。 */
    private static final double NEAR_TOTAL_REFLUX = 1.0e7;

    @Test
    void fenske_matchesAnalyticValue() {
        double nMin = TestSupport.fenske().minimumStages(2.5, 0.95, 0.05);
        assertEquals(Math.log(361.0) / Math.log(2.5), nMin, 1e-12);
    }

    @Test
    void nearTotalReflux_standardCase_stageCountIsWithinOnePlateOfFenske() {
        var service = TestSupport.service();

        DistillationResponse response = service.calculate(TestSupport.standardCase(NEAR_TOTAL_REFLUX));

        double nMin = response.minimumTheoreticalStages();
        int stages = response.stepping().totalStages();

        assertTrue(Math.abs(stages - nMin) <= 1.0,
                "R 逼近全回流时板数 " + stages + " 与 Fenske Nmin=" + nMin + " 相差超过一块板");
        // 板数只能是 Fenske 值向上取整的结果
        assertTrue(stages >= nMin - 1e-9 && stages < nMin + 1.0,
                "板数应等于 ceil(Nmin)");
        // 手工核算的精确值：7 块（含再沸器）
        assertEquals(7, stages);
        // 进料板为第 4 块（手工核算）
        assertEquals(4, response.stepping().feedStage());
    }

    @Test
    void nearTotalReflux_secondSeparationTarget_alsoWithinOnePlate() {
        var service = TestSupport.service();
        // xD=0.9, xB=0.1：Fenske Nmin=ln(81)/ln(2.5)≈4.796，手工核算阶梯 5 块
        var request = TestSupport.request(100.0, 0.5, 0.9, 0.1, 1.0, 2.5, NEAR_TOTAL_REFLUX);

        DistillationResponse response = service.calculate(request);

        double nMin = response.minimumTheoreticalStages();
        assertEquals(Math.log(81.0) / Math.log(2.5), nMin, 1e-12);

        int stages = response.stepping().totalStages();
        assertTrue(Math.abs(stages - nMin) <= 1.0,
                "板数 " + stages + " 与 Nmin=" + nMin + " 相差超过一块板");
        assertEquals(5, stages);
    }

    @Test
    void everyStage_liquidCompositionIsOnTheUniqueEquilibriumCurve() {
        var service = TestSupport.service();

        DistillationResponse response = service.calculate(TestSupport.standardCase(NEAR_TOTAL_REFLUX));

        // 每一级记录的液相 x 必须就是平衡线反函数给出的值，且末级已掉到 xB 以下
        var stages = response.stepping().stages();
        for (var stage : stages) {
            double expectedX = stage.vaporIn()
                    / (2.5 - 1.5 * stage.vaporIn());
            assertEquals(expectedX, stage.liquidComposition(), 1e-12,
                    "第 " + stage.stage() + " 级液相组成不在相平衡线上");
        }
        assertTrue(stages.get(stages.size() - 1).liquidComposition() <= 0.05,
                "末级液相组成必须已低于釜液目标");
    }
}
