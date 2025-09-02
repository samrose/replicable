# Replicable

A Clojure framework for replicable data workflows using Temporal.

## Prerequisites

1. Start the services (PostgreSQL and Temporal):
```bash
nix run .#dev
```

2. Ensure PostgreSQL is running on localhost:5432 with:
   - Database: `app`
   - User: `admin` 
   - Password: `admin`

3. Ensure Temporal is running on localhost:7233 with UI on port 8088

## Setup

1. Install dependencies:
```bash
lein deps
```

2. Run database migrations:
```bash
lein run migrate
```

## Usage

### Start the Worker

In one terminal, start the Temporal worker:
```bash
lein run worker
```

### Run the Example Workflow

In another terminal, run the example workflow:
```bash
lein run example
```

This will download sample CSV datasets and track the downloads in PostgreSQL.

### Monitor Progress

- View Temporal workflows: http://localhost:8088
- Check database records:
```bash
psql -h localhost -U admin -d app -c "SELECT * FROM data_downloads;"
```

## Project Structure

- `src/replicable/workflows.clj` - Temporal workflow definitions
- `src/replicable/activities.clj` - Temporal activity implementations
- `src/replicable/worker.clj` - Worker configuration and startup
- `src/replicable/client.clj` - Client code to start workflows
- `src/replicable/core.clj` - Main entry point
- `resources/migrations/` - Database migration files

## Development

The project uses:
- Temporal SDK for workflow orchestration
- Migratus for database migrations
- PostgreSQL for data persistence
- clj-http for downloading files