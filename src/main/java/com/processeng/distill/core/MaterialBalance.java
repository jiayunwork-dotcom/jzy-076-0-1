package com.processeng.distill.core;

import org.springframework.stereotype.Component;

/**
 * 全塔物料衡算（轻组分）：
 * <pre>
 *        F = D + B
 *    F*zF = D*xD + B*xB
 * </pre>
 * 由总进料量与三组组成反推馏出液、釜液流量：
 * <pre>
 *    D = F*(zF - xB)/(xD - xB)
 *    B = F - D
 * </pre>
 */
@Component
public class MaterialBalance {

    /** 衡算残差闭合容差（相对进料量）。解析解本身残差在机器精度量级，容差留足余量。 */
    public static final double CLOSURE_TOLERANCE = 1e-9;

    public record Result(double feedRate,
                         double distillateRate,
                         double bottomsRate,
                         double totalMassResidual,
                         double componentResidual) {

        /** 对外报告的衡算残差：取总物料与轻组分衡算残差的较大者。 */
        public double residual() {
            return Math.max(totalMassResidual, componentResidual);
        }
    }

    public Result solve(double feedRate, double feedComposition,
                        double distillateComposition, double bottomsComposition) {
        double distillateRate = feedRate * (feedComposition - bottomsComposition)
                / (distillateComposition - bottomsComposition);
        double bottomsRate = feedRate - distillateRate;

        double totalMassResidual = Math.abs(feedRate - distillateRate - bottomsRate);
        double componentResidual = Math.abs(
                feedRate * feedComposition
                        - distillateRate * distillateComposition
                        - bottomsRate * bottomsComposition);

        return new Result(feedRate, distillateRate, bottomsRate,
                totalMassResidual, componentResidual);
    }

    public boolean isClosed(Result result) {
        return result.residual() <= CLOSURE_TOLERANCE * Math.max(1.0, result.feedRate());
    }
}
