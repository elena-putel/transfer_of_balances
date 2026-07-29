CREATE TABLE IF NOT EXISTS transfer_load_log (
    id                  BIGSERIAL PRIMARY KEY,
    id_region           INTEGER         NOT NULL,
    fl_file             VARCHAR(22),
    report_file         VARCHAR(22),
    status              VARCHAR(20)     NOT NULL,
    records_total       INTEGER         NOT NULL DEFAULT 0,
    records_processed   INTEGER         NOT NULL DEFAULT 0,
    records_failed      INTEGER         NOT NULL DEFAULT 0,
    checksum_expected   DECIMAL(20, 4),
    checksum_calculated DECIMAL(20, 4),
    error_message       VARCHAR(1000),
    started_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at         TIMESTAMP,
    duration_ms         BIGINT
);

CREATE INDEX IF NOT EXISTS idx_transfer_load_log_fl_file ON transfer_load_log (fl_file);
CREATE INDEX IF NOT EXISTS idx_transfer_load_log_started_at ON transfer_load_log (started_at DESC);

CREATE TABLE IF NOT EXISTS transfer_path (
    id          BIGSERIAL PRIMARY KEY,
    id_region   INTEGER         NOT NULL,
    path        VARCHAR(500)    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_transfer_path_id_region ON transfer_path (id_region);

