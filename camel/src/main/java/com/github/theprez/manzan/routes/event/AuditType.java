package com.github.theprez.manzan.routes.event;


public enum AuditType {
    AUTHORITY_FAILURE("AUDIT_JOURNAL_AF"),
    AUTHORITY_CHANGES("AUDIT_JOURNAL_CA"),
    COMMAND_STRING("AUDIT_JOURNAL_CD"),
    CREATE_OBJECT("AUDIT_JOURNAL_CO"),
    USER_PROFILE_CHANGES("AUDIT_JOURNAL_CP"),
    DELETE_OPERATION("AUDIT_JOURNAL_DO"),
    ENVIRONMENT_VARIABLE("AUDIT_JOURNAL_EV"),
    GENERIC_RECORD("AUDIT_JOURNAL_GR"),
    JOB_CHANGE("AUDIT_JOURNAL_JS"),
    OBJECT_MANAGEMENT_CHANGE("AUDIT_JOURNAL_OM"),
    OWNERSHIP_CHANGE("AUDIT_JOURNAL_OW"),
    PASSWORD("AUDIT_JOURNAL_PW"),
    SERVICE_TOOLS_ACTION("AUDIT_JOURNAL_ST"),
    ACTION_TO_SYSTEM_VALUE("AUDIT_JOURNAL_SV");

    private final String value;

    AuditType(String value) {
        this.value = value;
    }

    /**
     * Get the AuditType table for the associated type
     *
     * @return The audit table to watch
     */
    public String getValue() {
        return String.format("SYSTOOLS.%s()", value);
    }

    /**
     * Get the AuditType representation of a string.
     *
     * @param value The string representation of the AuditType
     * @return The enum representation of the AuditType
     */
    public static AuditType fromValue(String value) {
        for (AuditType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}