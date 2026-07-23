CREATE TABLE IF NOT EXISTS transfer_balance (
    id              BIGSERIAL PRIMARY KEY,
    cust_code       INTEGER         NOT NULL,
    fio_askr        VARCHAR(155),
    ndog_billing_a  CHAR(20)        NOT NULL,
    account_a       CHAR(20)        NOT NULL,
    ndog_billing_b  CHAR(20)        NOT NULL,
    fio_billing_a   VARCHAR(155)    NOT NULL,
    bill_date       DATE,
    type_serv_a     INTEGER         NOT NULL DEFAULT 326,
    operation       SMALLINT        NOT NULL,
    summa           DECIMAL(15, 0)  NOT NULL,
    status          INTEGER         NOT NULL DEFAULT 1,
    date_input      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_mod        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cod_oper        INTEGER         NOT NULL,
    comment         VARCHAR(200),
    bill_type_a     SMALLINT,
    type_enter      SMALLINT        NOT NULL DEFAULT 0,
    fl_file         VARCHAR(22)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_transfer_balance_business_key
    ON transfer_balance (fl_file, ndog_billing_a, account_a, ndog_billing_b, bill_date, summa);

CREATE INDEX IF NOT EXISTS idx_transfer_balance_fl_file ON transfer_balance (fl_file);
CREATE INDEX IF NOT EXISTS idx_transfer_balance_bill_date ON transfer_balance (bill_date);
CREATE INDEX IF NOT EXISTS idx_transfer_balance_date_input ON transfer_balance (date_input);

CREATE TABLE IF NOT EXISTS transfer_load_log (
    id                  BIGSERIAL PRIMARY KEY,
    fl_file             VARCHAR(22)     NOT NULL,
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
