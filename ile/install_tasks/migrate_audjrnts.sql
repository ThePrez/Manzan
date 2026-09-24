-- Add SESSION_ID to AUDJRNTS for multi-instance support.
--
-- Safety guarantees:
--   * ALTER TABLE ... ADD COLUMN with DEFAULT preserves every existing row.
--   * Existing rows receive SESSION_ID = 'LEGACY' so single-instance customers
--     lose no data and their watermark queries continue to work unchanged.
--   * The column is NOT NULL — future inserts must always supply a session ID,
--     making it impossible to create an unidentifiable watermark row.
--   * CONTINUE HANDLERs swallow "already exists" errors so the script is
--     idempotent and compatible with V7R5M0 and later.

BEGIN
    DECLARE CONTINUE HANDLER FOR SQLSTATE '42711' BEGIN END;  -- column already exists
    ALTER TABLE AUDJRNTS
        ADD COLUMN SESSION_ID VARCHAR(10) DEFAULT 'LEGACY' NOT NULL;
END;

BEGIN
    DECLARE CONTINUE HANDLER FOR SQLSTATE '42891' BEGIN END;  -- index already exists
    CREATE INDEX AUDJRNTS_SESSION_IDX ON AUDJRNTS (SESSION_ID, TIME DESC);
END;
