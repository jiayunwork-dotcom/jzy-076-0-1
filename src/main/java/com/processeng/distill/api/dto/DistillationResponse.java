package com.processeng.distill.api.dto;

import com.processeng.distill.core.MaterialBalance;
import com.processeng.distill.core.StageStep;
import com.processeng.distill.core.SteppingResult;

import java.util.List;

/**
 * 简捷核算响应体：对外只暴露三样核心结果——最小回流比、最少理论板数、逐板阶梯结果，
 * 物料衡算明细作为闭合证据随阶梯结果附带。
 */
public record DistillationResponse(double minimumRefluxRatio,
                                   double minimumTheoreticalStages,
                                   MaterialBalanceView materialBalance,
                                   SteppingView stepping) {

    public record MaterialBalanceView(double feedRate,
                                      double distillateRate,
                                      double bottomsRate,
                                      double residual) {
    }

    public record OperatingLineView(double slope, double intercept) {
    }

    public record StageView(int stage,
                            double vaporIn,
                            double liquidComposition,
                            String section,
                            double vaporOut) {
    }

    public record SteppingView(double refluxRatio,
                               int totalStages,
                               int feedStage,
                               OperatingLineView rectifyingLine,
                               OperatingLineView strippingLine,
                               List<StageView> stages) {
    }

    public static DistillationResponse of(double minimumRefluxRatio,
                                          double minimumTheoreticalStages,
                                          MaterialBalance.Result balance,
                                          SteppingResult stepping) {
        var lines = stepping.lines();

        List<StageView> stages = stepping.stages().stream()
                .map(DistillationResponse::toStageView)
                .toList();

        return new DistillationResponse(
                minimumRefluxRatio,
                minimumTheoreticalStages,
                new MaterialBalanceView(
                        balance.feedRate(),
                        balance.distillateRate(),
                        balance.bottomsRate(),
                        balance.residual()),
                new SteppingView(
                        stepping.refluxRatio(),
                        stepping.totalStages(),
                        stepping.feedStage(),
                        new OperatingLineView(lines.rectifyingSlope(), lines.rectifyingIntercept()),
                        new OperatingLineView(lines.strippingSlope(), lines.strippingIntercept()),
                        stages));
    }

    private static StageView toStageView(StageStep step) {
        return new StageView(
                step.stage(),
                step.vaporIn(),
                step.liquidComposition(),
                step.section().name(),
                step.vaporOut());
    }
}
