package com.processeng.distill;

import com.processeng.distill.api.dto.DistillationResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 判据 3：同一组分离目标，实际回流比增大时，所需理论板数不得反而变多。
 * 该单调（不增）关系在多组回流比下逐一比对。
 */
class RefluxMonotonicityTest {

    @Test
    void stageCount_isNonIncreasing_asRefluxRatioIncreases() {
        var service = TestSupport.service();

        List<Double> refluxRatios = List.of(1.15, 1.3, 1.5, 2.0, 3.0, 5.0, 10.0);

        int previousStages = Integer.MAX_VALUE;
        double previousR = Double.NaN;
        for (double r : refluxRatios) {
            DistillationResponse response = service.calculate(TestSupport.standardCase(r));
            int stages = response.stepping().totalStages();

            assertTrue(stages <= previousStages,
                    "回流比由 R=" + previousR + " 增大到 R=" + r
                            + " 时，板数反而由 " + previousStages + " 增多到 " + stages);
            // 任何有限回流比的板数都不应少于全回流基准（手工核算为 7 块）
            assertTrue(stages >= 7, "R=" + r + " 时板数少于全回流基准，计算异常");

            previousStages = stages;
            previousR = r;
        }
    }

    @Test
    void refluxTwo_stagesEleven_regressionValue() {
        // 手工逐板核算的回归基准：R=2.0 时共 11 块理论板，进料板为第 5 块
        var service = TestSupport.service();
        DistillationResponse response = service.calculate(TestSupport.standardCase(2.0));
        assertEquals(11, response.stepping().totalStages());
        assertEquals(5, response.stepping().feedStage());
    }
}
