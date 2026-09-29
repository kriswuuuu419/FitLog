package com.cjlu.fitlog.service;

/**
 * Epley 公式估算 1RM：e1RM = w * (1 + r/30)
 * 替换为 Brzycki 等其他算法时，新建一个实现类即可，业务代码不动。
 */
public class EpleyPrCalculator implements PrCalculator {
    @Override
    public double estimateE1RM(double weightKg, int reps) {
        if (reps <= 0) throw new IllegalArgumentException("次数必须为正");
        return weightKg * (1.0 + reps / 30.0);
    }
}
