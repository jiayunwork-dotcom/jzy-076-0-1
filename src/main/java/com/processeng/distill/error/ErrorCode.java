package com.processeng.distill.error;

import org.springframework.http.HttpStatus;

/**
 * 服务对外暴露的结构化错误码。
 */
public enum ErrorCode {

    /** 入参格式 / 取值范围非法（组成越界、进料量非正、相对挥发度非正等） */
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),

    /** 组成顺序不满足 xD &gt; zF &gt; xB */
    COMPOSITION_ORDER_INVALID(HttpStatus.UNPROCESSABLE_ENTITY),

    /** 相对挥发度 0 &lt; alpha &lt;= 1，物理上无法分离 */
    SEPARATION_INFEASIBLE(HttpStatus.UNPROCESSABLE_ENTITY),

    /** 实际回流比不大于最小回流比 */
    REFLUX_BELOW_MINIMUM(HttpStatus.UNPROCESSABLE_ENTITY),

    /** 物料衡算残差超容差（理论上不应发生，属保护性错误） */
    MATERIAL_BALANCE_NOT_CLOSED(HttpStatus.INTERNAL_SERVER_ERROR),

    /** 逐板阶梯不收敛（触及迭代硬上限等） */
    CALCULATION_NOT_CONVERGED(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
