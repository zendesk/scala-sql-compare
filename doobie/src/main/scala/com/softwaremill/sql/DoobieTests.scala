package com.softwaremill.sql

import com.softwaremill.sql.TrackType.TrackType
import doobie._
import doobie.implicits._
import cats.effect.IO
import cats.effect.unsafe.implicits.global

object DoobieTests extends App with DbSetup {
  dbSetup()

  val xa =
    Transactor.fromDriverManager[IO]("org.postgresql.Driver", "jdbc:postgresql:sql_compare", "sqltester", "testpass")

  implicit val trackTypeMeta: Meta[TrackType] = Meta[Int].timap(TrackType.byIdOrThrow)(_.id)

  def insertCity(name: String, population: Int, area: Float, link: Option[String]): ConnectionIO[City] = {
    sql"insert into city(name, population, area, link) values ($name, $population, $area, $link)".update
      .withUniqueGeneratedKeys[CityId]("id")
      .map(id => City(id, name, population, area, link))
  }

  def insertWithGeneratedId(): Unit = {
    val result = insertCity("New York", 19795791, 141300, None).transact(xa).unsafeRunSync()
    println(s"Inserted, generated id: ${result.id}")
    println()
  }

  def selectAll(): Unit = {
    val program = sql"select id, name, population, area, link from city"
      .query[City]
      .to[List]

    runAndLogResults("All cities", program)
  }

  def selectAllLines(): Unit = {
    val program = sql"select id, system_id, name, station_count, track_type from metro_line"
      .query[MetroLine]
      .to[List]

    runAndLogResults("All lines", program)
  }

  def selectNamesOfBig(): Unit = {
    val bigLimit = 4000000

    val program = sql"select id, name, population, area, link from city where population > $bigLimit"
      .query[City]
      .to[List]

    runAndLogResults("All city names with population over 4M", program)
  }

  def selectMetroSystemsWithCityNames(): Unit = {
    case class MetroSystemWithCity(metroSystemName: String, cityName: String, dailyRidership: Int)

    val program =
      sql"select ms.name, c.name, ms.daily_ridership from metro_system as ms left join city as c on ms.city_id = c.id"
        .query[MetroSystemWithCity]
        .to[List]

    runAndLogResults("Metro systems with city names", program)
  }

  def selectMetroLinesSortedByStations(): Unit = {
    case class MetroLineWithSystemCityNames(
      metroLineName: String,
      metroSystemName: String,
      cityName: String,
      stationCount: Int,
    )

    val program = sql"""
      SELECT ml.name, ms.name, c.name, ml.station_count
        FROM metro_line as ml
        JOIN metro_system as ms on ml.system_id = ms.id
        JOIN city AS c ON ms.city_id = c.id
        ORDER BY ml.station_count DESC
      """.query[MetroLineWithSystemCityNames].to[List]

    runAndLogResults("Metro lines sorted by station count", program)
  }

  def selectMetroSystemsWithMostLines(): Unit = {
    case class MetroSystemWithLineCount(metroSystemName: String, cityName: String, lineCount: Int)

    val program = sql"""
      SELECT ms.name, c.name, COUNT(ml.id) as line_count
        FROM metro_line as ml
        JOIN metro_system as ms on ml.system_id = ms.id
        JOIN city AS c ON ms.city_id = c.id
        GROUP BY ms.id, c.id
        ORDER BY line_count DESC
      """.query[MetroSystemWithLineCount].to[List]

    runAndLogResults("Metro systems with most lines", program)
  }

  def selectCitiesWithSystemsAndLines(): Unit = {
    case class CityWithSystems(
      id: CityId,
      name: String,
      population: Int,
      area: Float,
      link: Option[String],
      systems: Seq[MetroSystemWithLines],
    )
    case class MetroSystemWithLines(id: MetroSystemId, name: String, dailyRidership: Int, lines: Seq[MetroLine])

    val program = sql"""
      SELECT c.id, c.name, c.population, c.area, c.link, ms.id, ms.city_id, ms.name, ms.daily_ridership, ml.id, ml.system_id, ml.name, ml.station_count, ml.track_type
        FROM metro_line as ml
        JOIN metro_system as ms on ml.system_id = ms.id
        JOIN city AS c ON ms.city_id = c.id
      """
      .query[(City, MetroSystem, MetroLine)]
      .to[List]
      .map { results =>
        results
          .groupBy(_._1)
          .map { case (c, citiesSystemsLines) =>
            val systems = citiesSystemsLines
              .groupBy(_._2)
              .map { case (s, systemsLines) =>
                MetroSystemWithLines(s.id, s.name, s.dailyRidership, systemsLines.map(_._3))
              }
            CityWithSystems(c.id, c.name, c.population, c.area, c.link, systems.toSeq)
          }
      }
      .map(_.toList)

    runAndLogResults("Cities with list of systems with list of lines", program)
  }

  def selectLinesConstrainedDynamically(): Unit = {
    val minStations: Option[Int] = Some(10)
    val maxStations: Option[Int] = None
    val sortDesc: Boolean = true

    val baseFr = fr"select id, system_id, name, station_count, track_type from metro_line"

    val minStationsFr = minStations.map(m => fr"station_count >= $m")
    val maxStationsFr = maxStations.map(m => fr"station_count <= $m")
    val whereFr = List(minStationsFr, maxStationsFr).flatten
      .reduceLeftOption(_ ++ _)
      .map(reduced => fr"where" ++ reduced)
      .getOrElse(fr"")

    val sortFr = fr"order by station_count" ++ (if (sortDesc) fr"desc" else fr"asc")

    val program = (baseFr ++ whereFr ++ sortFr).query[MetroLine].to[List]

    runAndLogResults("Lines constrained dynamically", program)
  }

  def transactions(): Unit = {
    def deleteCity(id: CityId): ConnectionIO[Int] = sql"delete from city where id = $id".update.run

    val insertAndDelete = for {
      inserted <- insertCity("Invalid", 0, 0, None)
      deleted <- deleteCity(inserted.id)
    } yield deleted

    println("Transactions")
    val deletedCount = insertAndDelete.transact(xa).unsafeRunSync()
    println(s"Deleted $deletedCount rows")
    println()
  }

  def checkQuery(): Unit = {
    println("Analyzing query for correctness")

    val yolo = xa.yolo
    import yolo._
    sql"select name from city".query[String].check.unsafeRunSync()

    println()
  }

  def runAndLogResults[R](label: String, program: ConnectionIO[List[R]]): Unit = {
    println(label)
    program.transact(xa).unsafeRunSync().foreach(println)
    println()
  }

  insertWithGeneratedId()
  selectAll()
  selectAllLines()
  selectNamesOfBig()
  selectMetroSystemsWithCityNames()
  selectMetroLinesSortedByStations()
  selectMetroSystemsWithMostLines()
  selectCitiesWithSystemsAndLines()
  selectLinesConstrainedDynamically()
  transactions()
  checkQuery()
}
