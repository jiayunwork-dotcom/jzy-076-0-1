package com.processeng.distill.core;

/**
 * 二元体系、恒相对挥发度下的相平衡关系。
 *
 * <p><b>这是全服务相平衡关系式的唯一定义点</b>：精馏段与提馏段的逐板阶梯都引用本类，
 * 禁止在别处再写一份平衡式，以免两段各用各的曲线导致板数对不上。
 *
 * <pre>
 * 平衡线（液相 x -> 气相 y）:  y = alpha * x / (1 + (alpha - 1) * x)
 * 反函数（气相 y -> 液相 x）:  x = y / (alpha - (alpha - 1) * y)
 * </pre>
 */
public final class PhaseEquilibrium {

    private final double alpha;

    public PhaseEquilibrium(double alpha) {
        if (alpha <= 1.0) {
            throw new IllegalArgumentException("相对挥发度必须大于 1 才能进行二元精馏分离");
        }
        this.alpha = alpha;
    }

    public double alpha() {
        return alpha;
    }

    /** 与液相组成 x 平衡的气相组成 y。 */
    public double equilibriumVapor(double liquidX) {
        return alpha * liquidX / (1.0 + (alpha - 1.0) * liquidX);
    }

    /** 与气相组成 y 平衡的液相组成 x（平衡线反函数，逐板阶梯每一级都从这里取液相组成）。 */
    public double equilibriumLiquid(double vaporY) {
        return vaporY / (alpha - (alpha - 1.0) * vaporY);
    }
}
