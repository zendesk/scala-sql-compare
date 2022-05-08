Source code for the blog: [Comparing Scala relational database access libraries](https://softwaremill.com/comparing-scala-relational-database-access-libraries/)

How to run with docker:

```docker
docker pull postgres
```

```docker
docker run --name sql-compare -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres -e POSTGRES_DB=sql_compare -p 5432:5432 -d postgres
```