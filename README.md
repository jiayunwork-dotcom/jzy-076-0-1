# 精馏塔简捷核算服务 (distillation-shortcut-service)

二元理想溶液精馏塔的简捷核算后端：一次 HTTP 调用返回 **最小回流比（Underwood）**、
**最少理论板数（Fenske）** 和 **给定回流比下的 McCabe–Thiele 逐板阶梯结果**（每块板的液相组成）。

技术栈：Java 17 + Spring Boot 3.3，Maven 构建，两阶段 Docker 镜像。纯 JSON API，无任何页面/流程图界面。

## 计算方法与口径

| 量 | 方法 | 说明 |
|---|---|---|
| 物料衡算 | `F = D + B`，`F·zF = D·xD + B·xB` | 解析反推 `D = F(zF−xB)/(xD−xB)`、`B = F−D`；残差（总物料 + 轻组分）必须 ≤ 1e-9·F |
| 最少理论板数 | Fenske 方程 | `Nmin = ln[ xD/(1−xD) · (1−xB)/xB ] / ln α`，**含再沸器、不含全凝器** |
| 最小回流比 | Underwood 法 | 二分法在 θ∈(1, α) 内解 `α·zF/(α−θ) + (1−zF)/(1−θ) = 1−q`，再算 Rmin；q=1 时与 McCabe 图解解析值一致（测试交叉验证） |
| 逐板阶梯 | McCabe–Thiele | 从 xD 起，在平衡线（y→x）与操作线（x→y）间逐级下行，x ≤ xB 终止；越过两操作线交点（q 线交点）后由精馏段切到提馏段 |

**相平衡关系式全服务只有一处定义** —— `core/PhaseEquilibrium.java`：

```
y = αx / (1 + (α−1)x)      x = y / (α − (α−1)y)
```

精馏段/提馏段逐板迭代都注入同一个 `PhaseEquilibrium` 实例，提馏段操作线由
`L' = RD + qF`、`V' = (R+1)D − (1−q)F` 经物料衡算推出，不存在两份互相对不上的平衡曲线。

硬性保护：`R ≤ Rmin` 直接报 422「回流不足」，**不进入迭代**；迭代另有 100 000 级硬上限，任何输入都不会死循环。

## 接口

`POST /api/v1/distillation/shortcut`

请求：

```json
{
  "feedRate": 100.0,
  "feedComposition": 0.5,
  "distillateComposition": 0.95,
  "bottomsComposition": 0.05,
  "feedQuality": 1.0,
  "relativeVolatility": 2.5,
  "refluxRatio": 2.0
}
```

| 字段 | 含义 | 约束 |
|---|---|---|
| feedRate | F 总进料量 | > 0 |
| feedComposition | zF | (0,1) |
| distillateComposition | xD | (0,1)，且 xD > zF |
| bottomsComposition | xB | (0,1)，且 zF > xB |
| feedQuality | q 进料热状态（0=饱和蒸汽，1=泡点液体，>1=过冷液体） | ≥ 0 |
| relativeVolatility | α | > 0；α ≤ 1 无法分离 |
| refluxRatio | R 实际回流比 | 必须 > Rmin |

响应（上例实测值）：

```json
{
  "minimumRefluxRatio": 1.1,
  "minimumTheoreticalStages": 6.426866,
  "materialBalance": {"feedRate":100.0,"distillateRate":50.0,"bottomsRate":50.0,"residual":7.1e-15},
  "stepping": {
    "refluxRatio": 2.0,
    "totalStages": 11,
    "feedStage": 5,
    "rectifyingLine": {"slope": 0.6666667, "intercept": 0.3166667},
    "strippingLine": {"slope": 1.3333333, "intercept": -0.0166667},
    "stages": [
      {"stage":1,"vaporIn":0.95,"liquidComposition":0.88372,"section":"RECTIFYING","vaporOut":0.90581}
    ]
  }
}
```

`totalStages` 含再沸器（末级阶梯）；`feedStage` 是第一级使用提馏段操作线的板号（进料板）；
`stages` 每一级都记录入口气相 y、平衡液相 x、所属塔段、出口气相 y。

错误响应统一为：

```json
{"code":"REFLUX_BELOW_MINIMUM","message":"回流不足：……","details":[],"timestamp":"…"}
```

| HTTP | code | 触发条件 |
|---|---|---|
| 400 | VALIDATION_ERROR | 组成不在 (0,1)、F≤0、α≤0、q<0、JSON 非法 |
| 422 | COMPOSITION_ORDER_INVALID | xD > zF > xB 不成立（含相等） |
| 422 | SEPARATION_INFEASIBLE | 0 < α ≤ 1，消息为「无法分离……」 |
| 422 | REFLUX_BELOW_MINIMUM | R ≤ Rmin，消息为「回流不足……」 |
| 500 | MATERIAL_BALANCE_NOT_CLOSED / CALCULATION_NOT_CONVERGED | 保护性错误，正常输入不应出现 |

## 本地构建与测试

```bash
mvn test        # 48 个测试
mvn package     # target/distillation-shortcut-service.jar
java -jar target/distillation-shortcut-service.jar
```

## Docker（两阶段，一次构建）

构建阶段用官方 `maven:3.9-eclipse-temurin-17`（JDK 17）编译并跑全部测试，
运行阶段用瘦身 `eclipse-temurin:17-jre` 只加载可执行 jar：

```bash
docker build -t distillation-shortcut-service .
docker run --rm -p 8080:8080 distillation-shortcut-service
```

## 测试与验收判据对照

| 需求判据 | 测试类 |
|---|---|
| 合法输入物料衡算残差必在容差内（D+B=F、轻组分进出相等，多组参数化用例） | `MaterialBalanceTest` |
| R 取足够大（1e7）逼近全回流，逐板板数贴 Fenske，差 ≤ 1 块板；逐级 x 在唯一平衡线上 | `FenskeTotalRefluxTest` |
| R 增大板数不增（R=1.15…10 共 7 组逐一比较）；R=2.0 手工回归值 11 块 | `RefluxMonotonicityTest` |
| R 等于/低于 Rmin 必须报「回流不足」，不吐板数；略高于 Rmin 放行 | `RefluxBelowMinimumTest` |
| q=0.5/1/1.5 下提馏段斜率随之变（1.5/1.333/1.25）、精馏段不变、衡算始终闭合 | `FeedConditionStrippingSlopeTest` |
| 组成越界、F≤0、α≤0 拒绝；0<α≤1 回「无法分离」；组成顺序反了报错 | `ValidationTest` |
| 以上规则在真实 HTTP 栈上的状态码与结构化响应（含畸形 JSON） | `ApiIntegrationTest` |
| Underwood 与泡点进料 McCabe 图解解析值交叉一致 | `UnderwoodCalculatorTest` |

## 模块划分

```
core/
  PhaseEquilibrium.java   ← 相平衡关系唯一定义点
  MaterialBalance.java    ← 物料衡算与闭合判定
  FenskeCalculator.java   ← 最少理论板数
  UnderwoodCalculator.java← 最小回流比
  OperatingLines.java     ← 两段操作线（提馏段由 q + 衡算推出，共用同一平衡关系）
  MccabeThieleStepper.java← 逐板阶梯（R≤Rmin 拦截 + 迭代硬上限）
service/DistillationService.java  编排顺序与领域校验
api/  DistillationController / ApiExceptionHandler / dto
error/ ErrorCode / DistillationException
```
