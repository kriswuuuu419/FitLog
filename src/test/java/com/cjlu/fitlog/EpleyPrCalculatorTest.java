package com.cjlu.fitlog;

import com.cjlu.fitlog.service.EpleyPrCalculator;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class EpleyPrCalculatorTest {

    private final EpleyPrCalculator calc = new EpleyPrCalculator();

    @Test
    public void oneRepMaxIsExact() {
        // 1RM 时公式应为 weight * (1 + 1/30) ≈ 1.0333 * weight
        double e1 = calc.estimateE1RM(100, 1);
        assertEquals(100 * (1 + 1.0/30), e1, 0.01);
    }

    @Test
    public void tenRepsEstimatesCorrectly() {
        // 100kg x 10 -> 100 * (1 + 10/30) = 133.33
        double e = calc.estimateE1RM(100, 10);
        assertEquals(133.33, e, 0.1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroRepsRejected() {
        calc.estimateE1RM(100, 0);
    }
}
