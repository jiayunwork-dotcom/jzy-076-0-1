package com.processeng.distill.core;

import com.processeng.distill.error.DistillationException;
import com.processeng.distill.error.ErrorCode;

/**
 * 精馏段与提馏段操作线（几何参数 y = slope*x + intercept）。
 *
 * <p><b>两条线共用同一个 {@link PhaseEquilibrium} 平衡关系</b>，提馏段参数由进料热状态 q
 * 和物料衡算推出，而不是另画一条平衡曲线：
 * <pre>
 * 精馏段: L = R*D, V = (R+1)*D
 *         y = L/V*x + D*xD/V = R/(R+1)*x + xD/(R+1)
 * 提馏段: L' = L + q*F,  V' = V - (1 - q)*F
 *         y = L'/V'*x - B*xB/V'
 * </pre>
 * 两线交点即进料状态（q 线）交点，由两条操作线直接解出，保证两段衡算自动闭合。
 */
public record OperatingLines(double rectifyingSlope,
                             double rectifyingIntercept,
                             double strippingSlope,
                             double strippingIntercept,
                             double feedIntersectionX,
                             double feedIntersectionY) {

    public static OperatingLines of(double refluxRatio,
                                    double distillateComposition,
                                    double feedQuality,
                                    double feedRate,
                                    double distillateRate,
                                    double bottomsRate,
                                    double bottomsComposition) {
        double rectSlope = refluxRatio / (refluxRatio + 1.0);
        double rectIntercept = distillateComposition / (refluxRatio + 1.0);

        double liquidRect = refluxRatio * distillateRate;
        double vaporRect = (refluxRatio + 1.0) * distillateRate;
        double liquidStrip = liquidRect + feedQuality * feedRate;
        double vaporStrip = vaporRect - (1.0 - feedQuality) * feedRate;

        if (vaporStrip <= 0.0) {
            throw new DistillationException(ErrorCode.CALCULATION_NOT_CONVERGED,
                    "提馏段气相流量非正（V'=" + vaporStrip + "），请检查进料热状态 q 与回流比");
        }

        double stripSlope = liquidStrip / vaporStrip;
        double stripIntercept = -bottomsRate * bottomsComposition / vaporStrip;

        double denominator = rectSlope - stripSlope;
        if (Math.abs(denominator) < 1e-15) {
            throw new DistillationException(ErrorCode.CALCULATION_NOT_CONVERGED,
                    "精馏段与提馏段操作线平行，无法确定进料交点");
        }
        double feedX = (stripIntercept - rectIntercept) / denominator;
        double feedY = rectSlope * feedX + rectIntercept;

        return new OperatingLines(rectSlope, rectIntercept, stripSlope, stripIntercept, feedX, feedY);
    }

    public double rectifyingY(double liquidX) {
        return rectifyingSlope * liquidX + rectifyingIntercept;
    }

    public double strippingY(double liquidX) {
        return strippingSlope * liquidX + strippingIntercept;
    }
}
