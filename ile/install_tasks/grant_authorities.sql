-- Authority grants for multi-instance operation.
--
-- These grants are intentionally permissive at the *PUBLIC level because the
-- actual data isolation is enforced by the SESSION_ID column in each table —
-- every instance only queries and mutates its own rows.
--
-- Note: HANDLER (*PGM) and MANZANDTAQ (*DTAQ) are ILE/OS objects that cannot
-- be granted via SQL GRANT.  Their authorities are set with GRTOBJAUT in the
-- ile/Makefile migrate target.

-- Event tables: instances must INSERT new rows and SELECT their own rows
GRANT INSERT, SELECT ON TABLE MANZANMSG  TO PUBLIC;
GRANT INSERT, SELECT ON TABLE MANZANOTH  TO PUBLIC;
GRANT INSERT, SELECT ON TABLE MANZANPAL  TO PUBLIC;
GRANT INSERT, SELECT ON TABLE MANZANVLOG TO PUBLIC;

-- Audit watermark table: instances must SELECT, INSERT, and UPDATE their row
GRANT SELECT, INSERT, UPDATE ON TABLE AUDJRNTS TO PUBLIC;
