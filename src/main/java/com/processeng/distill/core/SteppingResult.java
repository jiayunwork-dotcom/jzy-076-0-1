package com.processeng.distill.core;

import java.util.List;

/**
 * 逐板阶梯整体结果。
 *
 * @param refluxRatio   实际回流比
 * @param totalStages   理论板总数（含再沸器，不含全凝器）
 * @param feedStage     进料板（第一级使用提馏段操作线的板号，1 起）
 * @param lines         本次工况的两条操作线
 * @param stages        每一级阶梯的逐板记录
 */
public record SteppingResult(double refluxRatio,
                             int totalStages,
                             int feedStage,
                             OperatingLines lines,
                             List<StageStep> stages) {
}
