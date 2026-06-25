#!/bin/sh
set -e

echo "Creating multiple databases for Saga Pattern POC..."

psql -v ON_ERROR_STOP=1 --username "admin" --dbname "admin" <<-EOSQL
    CREATE DATABASE order_db;
    CREATE DATABASE inventory_db;
    CREATE DATABASE payment_db;
    CREATE DATABASE fulfillment_db;
EOSQL

echo "Successfully created all 4 databases!"
