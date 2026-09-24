package com.processeng.distill.core;

import org.springframework.stereotype.Component;

/**
 * Fenske 方程：全回流（R -> infinity）下所需的最少理论板数，
 * 是其他回流比工况的板数基准。
 *
 * <pre>
 *    Nmin = ln[ (xD/(1-xD)) * ((1-xB)/xB) ] / ln(alpha)
 * </pre>
 * 口径：板数含再沸器、不含全凝器（全凝器不计分离级），与逐板阶梯的计数口径一致。
 */
@Component
public class FenskeCalculator {

    public double minimumStages(double relativeVolatility,
                                double distillateComposition,
                                double bottomsComposition) {
        double separationFactor =
                (distillateComposition / (1.0 - distillateComposition))
                        * ((1.0 - bottomsComposition) / bottomsComposition);
        return Math.log(separationFactor) / Math.log(relativeVolatility);
    }
}
