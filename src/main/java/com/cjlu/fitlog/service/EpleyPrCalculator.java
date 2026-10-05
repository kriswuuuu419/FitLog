package com.cjlu.fitlog.service;

/**
 * Epley 公式估算 1RM：e1RM = w * (1 + r/30)。
 * 公共的入参校验由父类 AbstractPrCalculator 完成，本类只负责具体公式。
 * 替换为 Brzycki 等其他算法时，新建一个继承 AbstractPrCalculator 的类即可，业务代码不动。
 */
public class EpleyPrCalculator extends AbstractPrCalculator {

    @Override
    protected double formula(double weightKg, int reps) {
        return weightKg * (1.0 + reps / 30.0);
    }
}
