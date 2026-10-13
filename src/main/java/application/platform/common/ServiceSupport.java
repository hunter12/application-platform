package application.platform.common;

import java.util.Objects;

public final class ServiceSupport {

    private ServiceSupport() {
    }

    public static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(label + " is required");
        }
        return value.trim();
    }

    public static <T> T require(T value, String label) {
        if (value == null) {
            throw new BusinessException(label + " is required");
        }
        return value;
    }

    /** Trims blank optional text to {@code null}. */
    public static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Normalizes a technical code to upper case with underscores, e.g.
     * {@code "branch manager"} becomes {@code BRANCH_MANAGER}.
     */
    public static String normalizeCode(String value, String label) {
        String code = requireText(value, label).toUpperCase().replace(' ', '_');
        if (!code.matches("[A-Z0-9][A-Z0-9_-]*")) {
            throw new BusinessException(label + " may only contain letters, digits, '_' and '-'");
        }
        return code;
    }

    /**
     * Fails when a unique value is already used by another row.
     * {@code rowsWithValue} must come from a query that also counts
     * soft-deleted rows, because the database unique constraint still applies
     * to them; the row being edited is not counted against itself.
     */
    public static void ensureUnique(long rowsWithValue, boolean editedRowHasValue, String message) {
        if (rowsWithValue > (editedRowHasValue ? 1 : 0)) {
            throw new BusinessException(message);
        }
    }

    /**
     * Optimistic locking for edit forms: rejects the save when the row was
     * changed by someone else after the form was opened.
     */
    public static void checkVersion(BaseEntity entity, Long expectedVersion) {
        if (expectedVersion != null && !Objects.equals(entity.getVersion(), expectedVersion)) {
            throw new BusinessException(
                    "This record was changed by someone else. Reload the page and try again.");
        }
    }
}
