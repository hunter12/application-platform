package application.platform.views;

import java.util.Optional;
import java.util.UUID;

public final class UrlIds {

    private UrlIds() {
    }

    /** The id in {@code value}, or empty when it is not a valid UUID. */
    public static Optional<UUID> parse(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
