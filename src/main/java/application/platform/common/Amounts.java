package application.platform.common;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class Amounts {

    public static final BigDecimal MAX = new BigDecimal("9999999999999.99");

    private static final Locale INDONESIA = Locale.forLanguageTag("id-ID");

    private Amounts() {
    }

    /** Rupiah with Indonesian separators, e.g. {@code Rp 10.000.000,01}. */
    public static String format(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(INDONESIA);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        return "Rp " + format.format(amount);
    }
}
