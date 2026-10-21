-- Per-instance data retention procedure.
--
-- CLEANUP_INSTANCE_DATA removes all rows written by a given Manzan instance
-- from every event and audit-watermark table.  Typical use cases:
--   * Retiring an instance and reclaiming table space.
--   * Resetting a test or development instance between runs.
--
-- The caller supplies the 10-character SESSION_ID prefix that uniquely
-- identifies the instance (e.g. 'WATSON0001').  Rows in all other instances
-- are left completely untouched.
--
-- The procedure runs inside a single transaction: either every table is
-- cleaned or none is, so partial cleanup cannot leave the database in an
-- inconsistent state.

CREATE OR REPLACE PROCEDURE CLEANUP_INSTANCE_DATA (
    IN p_session_id VARCHAR(10)
)
LANGUAGE SQL
BEGIN
    DELETE FROM MANZANMSG  WHERE SESSION_ID = p_session_id;
    DELETE FROM MANZANOTH  WHERE SESSION_ID = p_session_id;
    DELETE FROM MANZANPAL  WHERE SESSION_ID = p_session_id;
    DELETE FROM MANZANVLOG WHERE SESSION_ID = p_session_id;
    DELETE FROM AUDJRNTS   WHERE SESSION_ID = p_session_id;
END;
