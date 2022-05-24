Source code for the blog: [Comparing Scala relational database access libraries](https://softwaremill.com/comparing-scala-relational-database-access-libraries/)

How to run with docker:

```docker
docker pull postgres
```

```docker
docker run --name sql-compare -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres -e POSTGRES_DB=sql_compare -p 5432:5432 -d postgres
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