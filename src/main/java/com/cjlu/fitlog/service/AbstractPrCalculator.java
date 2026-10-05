package com.cjlu.fitlog.service;

/**
 * PR 估算器的公共模板基类（模板方法模式）。
 * 先在 estimateE1RM 中统一完成入参校验，再把具体估算公式交给子类的 formula()。
 * 新增 Brzycki 等其他算法时，只需继承本类并实现 formula()，校验逻辑自动复用、业务代码不动。
 */
public abstract class AbstractPrCalculator implements PrCalculator {

    @Override
    public final double estimateE1RM(double weightKg, int reps) {
        if (weightKg <= 0) {
            throw new IllegalArgumentException("重量必须为正数");
        }
        if (reps <= 0) {
            throw new IllegalArgumentException("次数必须为正");
        }
        return formula(weightKg, reps);
    }

    protected abstract double formula(double weightKg, int reps);
}
