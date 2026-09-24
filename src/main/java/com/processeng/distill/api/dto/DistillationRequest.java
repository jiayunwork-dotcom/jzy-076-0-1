package com.processeng.distill.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 简捷核算请求体。
 *
 * @param feedRate                总进料量 F（必须为正）
 * @param feedComposition         进料轻组分摩尔分数 zF，开区间 (0,1)
 * @param distillateComposition   馏出液轻组分摩尔分数 xD，开区间 (0,1)，须满足 xD &gt; zF
 * @param bottomsComposition      釜液轻组分摩尔分数 xB，开区间 (0,1)，须满足 zF &gt; xB
 * @param feedQuality             进料热状态参数 q（0=饱和蒸汽，1=泡点液体，&gt;1=过冷液体）
 * @param relativeVolatility      相对挥发度 alpha（必须为正；alpha &lt;= 1 无法分离）
 * @param refluxRatio             实际回流比 R（必须大于最小回流比 Rmin）
 */
public record DistillationRequest(

        @NotNull(message = "进料量不能为空")
        @Positive(message = "进料量必须大于 0")
        Double feedRate,

        @NotNull(message = "进料组成不能为空")
        @DecimalMin(value = "0.0", inclusive = false, message = "进料组成必须在开区间 (0,1) 内")
        @DecimalMax(value = "1.0", inclusive = false, message = "进料组成必须在开区间 (0,1) 内")
        Double feedComposition,

        @NotNull(message = "馏出液组成不能为空")
        @DecimalMin(value = "0.0", inclusive = false, message = "馏出液组成必须在开区间 (0,1) 内")
        @DecimalMax(value = "1.0", inclusive = false, message = "馏出液组成必须在开区间 (0,1) 内")
        Double distillateComposition,

        @NotNull(message = "釜液组成不能为空")
        @DecimalMin(value = "0.0", inclusive = false, message = "釜液组成必须在开区间 (0,1) 内")
        @DecimalMax(value = "1.0", inclusive = false, message = "釜液组成必须在开区间 (0,1) 内")
        Double bottomsComposition,

        @NotNull(message = "进料热状态参数 q 不能为空")
        @PositiveOrZero(message = "进料热状态参数 q 必须非负")
        Double feedQuality,

        @NotNull(message = "相对挥发度不能为空")
        @Positive(message = "相对挥发度必须为正数")
        Double relativeVolatility,

        @NotNull(message = "实际回流比不能为空")
        @Positive(message = "实际回流比必须大于 0")
        Double refluxRatio) {
}
