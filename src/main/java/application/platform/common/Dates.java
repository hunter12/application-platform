package application.platform.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Dates {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm",
            Locale.ENGLISH);

    private Dates() {
    }

    /** e.g. {@code 20 Oct 2026} */
    public static String format(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    /** e.g. {@code 20 Oct 2026 14:05}, in the server's time zone */
    public static String format(Instant instant) {
        return instant == null ? "" : DATE_TIME.format(instant.atZone(ZoneId.systemDefault()));
    }
}
