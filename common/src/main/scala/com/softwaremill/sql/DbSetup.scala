package com.softwaremill.sql

import org.flywaydb.core.Flyway

trait DbSetup {
  val connectionString = "jdbc:postgresql:sql_compare"

  def dbSetup(): Unit = {
    val flyway = Flyway.configure().dataSource(connectionString, "postgres", "postgres").load()
    flyway.clean()
    flyway.migrate()
  }
}