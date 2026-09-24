package com.processeng.distill;

import com.processeng.distill.api.dto.DistillationRequest;
import com.processeng.distill.core.FenskeCalculator;
import com.processeng.distill.core.MaterialBalance;
import com.processeng.distill.core.MccabeThieleStepper;
import com.processeng.distill.core.UnderwoodCalculator;
import com.processeng.distill.service.DistillationService;

/**
 * 不启 Spring 容器直接组装服务，供单元/属性测试使用。
 */
final class TestSupport {

    /** 标准算例：F=100, zF=0.5, xD=0.95, xB=0.05, alpha=2.5, q=1（泡点进料）。 */
    static final double FEED_RATE = 100.0;
    static final double ZF = 0.5;
    static final double XD = 0.95;
    static final double XB = 0.05;
    static final double ALPHA = 2.5;
    static final double Q_BUBBLE = 1.0;

    /** 该算例解析值：D=B=50；Underwood Rmin=1.1；Fenske Nmin=ln(361)/ln(2.5)。 */
    static final double EXPECTED_RMIN = 1.1;

    private TestSupport() {
    }

    static DistillationService service() {
        return new DistillationService(
                new MaterialBalance(),
                new FenskeCalculator(),
                new UnderwoodCalculator(),
                new MccabeThieleStepper());
    }

    static UnderwoodCalculator underwood() {
        return new UnderwoodCalculator();
    }

    static FenskeCalculator fenske() {
        return new FenskeCalculator();
    }

    static MaterialBalance materialBalance() {
        return new MaterialBalance();
    }

    static DistillationRequest request(double feedRate, double zF, double xD, double xB,
                                       double q, double alpha, double refluxRatio) {
        return new DistillationRequest(feedRate, zF, xD, xB, q, alpha, refluxRatio);
    }

    static DistillationRequest standardCase(double refluxRatio) {
        return request(FEED_RATE, ZF, XD, XB, Q_BUBBLE, ALPHA, refluxRatio);
    }
}
