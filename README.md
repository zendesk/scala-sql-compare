Source code for the blog: [Comparing Scala relational database access libraries](https://softwaremill.com/comparing-scala-relational-database-access-libraries/)

## Run ##

### Run with Postgres.app ###

See [Doobie](https://tpolecat.github.io/doobie/index.html)'s [Sample Database Setup](https://tpolecat.github.io/doobie/docs/01-Introduction.html#sample-database-setup) guide.

Then run the following:

    psql -c "create user sqltester with encrypted password 'testpass'" postgres
    psql -c 'CREATE DATABASE sql_compare OWNER sqltester;' postgres

### Run with docker ###

How to run with docker:

```docker
docker pull postgres
```

```docker
docker run --name sql-compare -e POSTGRES_PASSWORD=testpass -e POSTGRES_USER=sqltester -e POSTGRES_DB=sql_compare -p 5432:5432 -d postgres
```

If you got problem with connection to database like:
```java
UnsupportedAuthenticationMethodException: unsupported authentication method 10
```
need to update `pg_hba.conf` in docker-container:
```shell
# copy pg_hba.conf from container:
docker cp sql-compare:/var/lib/postgresql/data/pg_hba.conf .

# add to pg_hba.file file next line under "IPv4 local connections":
host    all             all             0.0.0.0/0               trust

# copy pg_hba.conf into container:
docker cp pg_hba.conf sql-compare:/var/lib/postgresql/data/

# restart container or run SELECT pg_reload_conf(); inside database.
```
