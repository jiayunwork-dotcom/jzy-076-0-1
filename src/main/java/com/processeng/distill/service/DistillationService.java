package com.processeng.distill.service;

import com.processeng.distill.api.dto.DistillationRequest;
import com.processeng.distill.api.dto.DistillationResponse;
import com.processeng.distill.core.FenskeCalculator;
import com.processeng.distill.core.MaterialBalance;
import com.processeng.distill.core.MccabeThieleStepper;
import com.processeng.distill.core.OperatingLines;
import com.processeng.distill.core.PhaseEquilibrium;
import com.processeng.distill.core.SteppingResult;
import com.processeng.distill.core.UnderwoodCalculator;
import com.processeng.distill.error.DistillationException;
import com.processeng.distill.error.ErrorCode;
import org.springframework.stereotype.Service;

/**
 * 简捷核算编排层：物料衡算 -> Fenske -> Underwood -> 操作线 -> 逐板阶梯。
 * 各计算职责都在独立模块里，这里只负责顺序、前置校验和结果装配。
 */
@Service
public class DistillationService {

    private final MaterialBalance materialBalance;
    private final FenskeCalculator fenskeCalculator;
    private final UnderwoodCalculator underwoodCalculator;
    private final MccabeThieleStepper stepper;

    public DistillationService(MaterialBalance materialBalance,
                               FenskeCalculator fenskeCalculator,
                               UnderwoodCalculator underwoodCalculator,
                               MccabeThieleStepper stepper) {
        this.materialBalance = materialBalance;
        this.fenskeCalculator = fenskeCalculator;
        this.underwoodCalculator = underwoodCalculator;
        this.stepper = stepper;
    }

    public DistillationResponse calculate(DistillationRequest request) {
        double feedRate = request.feedRate();
        double zF = request.feedComposition();
        double xD = request.distillateComposition();
        double xB = request.bottomsComposition();
        double q = request.feedQuality();
        double alpha = request.relativeVolatility();
        double refluxRatio = request.refluxRatio();

        validateDomain(feedRate, zF, xD, xB, q, alpha, refluxRatio);

        // 相平衡关系只在这里实例化一次，两段操作线和逐板迭代共用它
        PhaseEquilibrium equilibrium = new PhaseEquilibrium(alpha);

        // 1) 物料闭合：反推 D、B，残差必须在容差内
        MaterialBalance.Result balance = materialBalance.solve(feedRate, zF, xD, xB);
        if (!materialBalance.isClosed(balance)) {
            throw new DistillationException(ErrorCode.MATERIAL_BALANCE_NOT_CLOSED,
                    "物料衡算未闭合，残差=" + balance.residual());
        }

        // 2) Fenske：全回流最少理论板数（其他工况的基准）
        double minimumStages = fenskeCalculator.minimumStages(alpha, xD, xB);

        // 3) Underwood：最小回流比
        double minimumReflux = underwoodCalculator.minimumRefluxRatio(alpha, zF, xD, q);

        // 4) 实际回流比必须严格大于 Rmin，否则报「回流不足」，绝不进入死循环
        if (refluxRatio <= minimumReflux + MccabeThieleStepper.REFLUX_TOLERANCE) {
            throw new DistillationException(ErrorCode.REFLUX_BELOW_MINIMUM,
                    "回流不足：实际回流比 R=" + refluxRatio
                            + " 必须严格大于最小回流比 Rmin=" + minimumReflux);
        }

        // 5) 两条操作线（共用同一相平衡关系；提馏段由 q 与物料衡算推出）
        OperatingLines lines = OperatingLines.of(
                refluxRatio, xD, q,
                feedRate, balance.distillateRate(), balance.bottomsRate(), xB);

        // 6) McCabe-Thiele 逐板阶梯
        SteppingResult stepping = stepper.step(
                equilibrium, lines, xD, xB, refluxRatio, minimumReflux);

        return DistillationResponse.of(minimumReflux, minimumStages, balance, stepping);
    }

    /**
     * Bean 校验之外的领域规则（控制器入口已有 bean validation，这里保证服务单独使用时同样安全）。
     */
    private void validateDomain(double feedRate, double zF, double xD, double xB,
                                double q, double alpha, double refluxRatio) {
        if (feedRate <= 0.0) {
            throw new DistillationException(ErrorCode.VALIDATION_ERROR, "进料量必须大于 0");
        }
        if (q < 0.0) {
            throw new DistillationException(ErrorCode.VALIDATION_ERROR, "进料热状态参数 q 必须非负");
        }
        if (refluxRatio <= 0.0) {
            throw new DistillationException(ErrorCode.VALIDATION_ERROR, "实际回流比必须大于 0");
        }
        if (!isOpenUnitInterval(zF) || !isOpenUnitInterval(xD) || !isOpenUnitInterval(xB)) {
            throw new DistillationException(ErrorCode.VALIDATION_ERROR,
                    "各组成必须落在开区间 (0,1) 内（组成取 0 或 1 会使对数/比值关系退化）");
        }
        if (alpha <= 0.0) {
            throw new DistillationException(ErrorCode.VALIDATION_ERROR,
                    "相对挥发度必须为正数（当前 alpha=" + alpha + "）");
        }
        if (alpha <= 1.0) {
            throw new DistillationException(ErrorCode.SEPARATION_INFEASIBLE,
                    "无法分离：相对挥发度必须大于 1（当前 alpha=" + alpha + "）");
        }
        if (!(xD > zF && zF > xB)) {
            throw new DistillationException(ErrorCode.COMPOSITION_ORDER_INVALID,
                    "组成顺序必须满足 xD > zF > xB（当前 xD=" + xD + ", zF=" + zF + ", xB=" + xB + "）");
        }
    }

    private boolean isOpenUnitInterval(double value) {
        return value > 0.0 && value < 1.0;
    }
}
