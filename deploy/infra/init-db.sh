#!/bin/bash
psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "CREATE DATABASE your_staging_database;" 2>/dev/null || true