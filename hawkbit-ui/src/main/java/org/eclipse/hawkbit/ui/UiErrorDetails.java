package org.eclipse.hawkbit.ui;

public class UiErrorDetails {
    private static final UiErrorDetails UNKNOWN = new UiErrorDetails(UiErrorDetailsType.UNKNOWN);
    private static final UiErrorDetails IGNORED = new UiErrorDetails(UiErrorDetailsType.IGNORED);

    private final UiErrorDetailsType type;

    private final String caption;
    private final String description;

    private UiErrorDetails(final UiErrorDetailsType type) {
        this(type, null, null);
    }

    private UiErrorDetails(final UiErrorDetailsType type, final String caption, final String description) {
        this.type = type;
        this.caption = caption;
        this.description = description;
    }

    public UiErrorDetailsType getType() {
        return type;
    }

    public String getCaption() {
        return caption;
    }

    public String getDescription() {
        return description;
    }

    public boolean isKnown() {
        return type != UiErrorDetailsType.UNKNOWN;
    }

    public boolean isPresent() {
        return type == UiErrorDetailsType.PRESENT;
    }

    public static UiErrorDetails unknown() {
        return UNKNOWN;
    }

    public static UiErrorDetails ignored() {
        return IGNORED;
    }

    public static UiErrorDetails create(final String caption, final String description) {
        return new UiErrorDetails(UiErrorDetailsType.PRESENT, caption, description);
    }

    public enum UiErrorDetailsType {
        PRESENT, IGNORED, UNKNOWN
    }
}
