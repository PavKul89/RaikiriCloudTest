-- Таблица для хранения сгенерированных событий
CREATE TABLE IF NOT EXISTS generated_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    is_processed BOOLEAN NOT NULL DEFAULT FALSE
    );


-- Комментарии к таблице и колонкам
COMMENT ON TABLE generated_events IS 'Таблица для хранения сгенерированных событий';
COMMENT ON COLUMN generated_events.id IS 'Уникальный идентификатор события';
COMMENT ON COLUMN generated_events.event_type IS 'Тип события';
COMMENT ON COLUMN generated_events.service_name IS 'Имя сервиса-создателя';
COMMENT ON COLUMN generated_events.payload IS 'Дополнительные данные события';
COMMENT ON COLUMN generated_events.created_at IS 'Время создания события';
COMMENT ON COLUMN generated_events.processed_at IS 'Время обработки события';
COMMENT ON COLUMN generated_events.is_processed IS 'Флаг обработки события';