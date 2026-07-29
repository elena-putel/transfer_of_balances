-- Для окружений, где V1 уже применена с fl_file NOT NULL
ALTER TABLE transfer_load_log
    ALTER COLUMN fl_file DROP NOT NULL;
