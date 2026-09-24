package com.processeng.distill.core;

import com.processeng.distill.error.DistillationException;
import com.processeng.distill.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * McCabe-Thiele 逐板阶梯迭代。
 *
 * <p>从馏出液组成 xD 起手（全凝器回流液组成等于 xD），每一级：
 * <ol>
 *   <li>水平向左：由气相 y 在<b>唯一的平衡线</b> {@link PhaseEquilibrium#equilibriumLiquid(double)}
 *       上反求本级液相 x；</li>
 *   <li>按液相 x 相对两操作线交点（q 线交点）的位置选段：
 *       x &gt; xq 用精馏段操作线，否则用提馏段操作线（两条线共用同一份平衡关系）；</li>
 *   <li>竖直向上：在所选操作线上求离开本级的气相 y'，作为下一级入口。</li>
 * </ol>
 * 当液相组成掉到釜液目标 xB 以下时结束，走过的阶梯数即理论板总数（末级为再沸器）。
 *
 * <p>硬性保护：实际回流比不大于 Rmin 时直接报「回流不足」，绝不进入可能永不终止的迭代；
 * 另设 {@link #MAX_STAGES} 硬上限兜底，任何输入都不会陷入死循环。
 */
@Component
public class MccabeThieleStepper {

    /** 阶梯级数硬上限（正常工况远低于此值）。 */
    public static final int MAX_STAGES = 100_000;

    /** R 必须严格大于 Rmin 的判定容差（绝对值）。 */
    public static final double REFLUX_TOLERANCE = 1e-9;

    public SteppingResult step(PhaseEquilibrium equilibrium,
                               OperatingLines lines,
                               double distillateComposition,
                               double bottomsComposition,
                               double refluxRatio,
                               double minimumRefluxRatio) {
        if (refluxRatio <= minimumRefluxRatio + REFLUX_TOLERANCE) {
            throw new DistillationException(ErrorCode.REFLUX_BELOW_MINIMUM,
                    "回流不足：实际回流比 R=" + refluxRatio
                            + " 必须严格大于最小回流比 Rmin=" + minimumRefluxRatio);
        }

        List<StageStep> stages = new ArrayList<>();
        double vaporIn = distillateComposition;
        int feedStage = -1;

        for (int number = 1; number <= MAX_STAGES; number++) {
            // 平衡线：气相 y -> 液相 x（全服务唯一的相平衡关系）
            double liquidX = equilibrium.equilibriumLiquid(vaporIn);

            // 过两线交点后切换到提馏段操作线
            Section section = liquidX > lines.feedIntersectionX()
                    ? Section.RECTIFYING
                    : Section.STRIPPING;
            double vaporOut = section == Section.RECTIFYING
                    ? lines.rectifyingY(liquidX)
                    : lines.strippingY(liquidX);

            stages.add(new StageStep(number, vaporIn, liquidX, section, vaporOut));
            if (section == Section.STRIPPING && feedStage < 0) {
                feedStage = number;
            }

            // 液相组成掉到釜液目标以下：到达再沸器，结束
            if (liquidX <= bottomsComposition) {
                return new SteppingResult(refluxRatio, number, feedStage, lines, List.copyOf(stages));
            }

            vaporIn = vaporOut;
        }

        throw new DistillationException(ErrorCode.CALCULATION_NOT_CONVERGED,
                "逐板阶梯超过 " + MAX_STAGES + " 级仍未达到釜液组成，迭代已中止");
    }
}
