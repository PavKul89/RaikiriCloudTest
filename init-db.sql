CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'events_db') THEN
        CREATE DATABASE events_db;
END IF;
END
$$;