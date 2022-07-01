package com.softwaremill.sql

import com.dimafeng.testcontainers.SingleContainer
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import zio._
import org.flywaydb.core.Flyway

object TestContainer {

  def container[C <: SingleContainer[_]: Tag](c: C): ZLayer[Any, Throwable, C] =
    ZManaged.acquireReleaseWith {
      ZIO.attemptBlocking {
        c.start()
        c
      }
    }(container => ZIO.attemptBlocking(container.stop()).orDie).toLayer

  def postgres(imageName: String = "postgres:alpine"): ZManaged[Any, Throwable, PostgreSQLContainer] =
    ZManaged.acquireReleaseWith {
      ZIO.attemptBlocking {
        val c = new PostgreSQLContainer(
          dockerImageNameOverride = Option(imageName).map(DockerImageName.parse)
        )
        c.start()
        val flyway = Flyway.configure().dataSource(c.container.getJdbcUrl(), c.container.getUsername(), c.container.getPassword()).load()
        flyway.migrate()
        c
      }
    } { container =>
      ZIO.attemptBlocking(container.stop()).orDie
    }
}
