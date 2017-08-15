odk-shootout-java-spark
=======================

Minimal Aggregate core based on Java, Spark, and Postgres.

Configure It
------------

Create the database:

```sql
CREATE DATABASE aggregate_spark;
\connect aggregate_spark

CREATE TABLE submissions (
    id bigserial PRIMARY KEY,
    formId text,
    instanceId text,
    xml text,

    CONSTRAINT instanceId_unique UNIQUE (formId, instanceId)
);
```

Set environment variables:

```bash
export DATABASE_URL=postgresql://localhost:5432/aggregate_spark
```

You may also need to configure `DATABASE_USERNAME` and `DATABASE_PASSWORD`.

Run It
------

Use Maven to start the server:

```bash
mvn compile && mvn exec:java
```
