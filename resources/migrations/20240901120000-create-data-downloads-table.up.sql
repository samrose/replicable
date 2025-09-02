CREATE TABLE IF NOT EXISTS data_downloads (
    id SERIAL PRIMARY KEY,
    workflow_id VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    file_size BIGINT,
    download_started_at TIMESTAMP,
    download_completed_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
--;;
CREATE INDEX IF NOT EXISTS idx_workflow_id ON data_downloads(workflow_id);
--;;
CREATE INDEX IF NOT EXISTS idx_status ON data_downloads(status);
--;;
CREATE INDEX IF NOT EXISTS idx_created_at ON data_downloads(created_at);