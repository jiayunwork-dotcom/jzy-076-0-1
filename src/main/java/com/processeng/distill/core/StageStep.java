package com.processeng.distill.core;

/**
 * 一块理论板（一级阶梯）的逐板记录。
 *
 * @param stage               板序号（从塔顶全凝器下方起 1，末级为再沸器）
 * @param vaporIn             进入本级的气相组成 y（来自下一级）
 * @param liquidComposition   与 vaporIn 平衡的本级液相组成 x（阶梯与平衡线的交点）
 * @param section             本级所用操作线所属塔段
 * @param vaporOut            离开本级、向上的气相组成 y（操作线上取的下一级入口值）
 */
public record StageStep(int stage,
                        double vaporIn,
                        double liquidComposition,
                        Section section,
                        double vaporOut) {
}
