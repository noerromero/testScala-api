package models

import java.util.UUID



case class Movie (
  id: Option[String] = Option(UUID.randomUUID().toString()),
  title: String,
  year: Int,
  cover: String,
  description: String,
  duration: Int,
  contentRating: String,
  source: String,
  tags: Option[String]
)


import slick.jdbc.SQLiteProfile.api._
/*
Clase que mapea el objeto Movie de Scala con la tabla Movie de la BD
 */

class MovieTable(tag: Tag) extends Table[Movie](tag, _tableName = "movie") {
  def id: Rep[String] = column[String]("id", O.PrimaryKey)
  def title: Rep[String] = column[String]("title")
  def year: Rep[Int] = column[Int]("year")
  def cover: Rep[String] = column[String]("cover")
  def description: Rep[String] = column[String]("description")
  def duration: Rep[Int] = column[Int]("duration")
  def contentRating: Rep[String] = column[String]("content_rating")
  def source: Rep[String] = column[String]("source")
  def tags: Rep[Option[String]]=column[Option[String]]("tags", O.Length(2000, varying = true))

  def * = (id.?, title, year, cover, description, duration, contentRating, source, tags) <> (Movie.tupled, Movie.unapply)

}

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc.{AbstractController,ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.JdbcProfile

/*
Clase Repository, donde iran las consultas
 */

class MovieRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  cc: ControllerComponents
                               )(implicit ec: ExecutionContext)
                                extends AbstractController(cc)
                                with HasDatabaseConfigProvider[JdbcProfile] {
  private lazy val movieQuery = TableQuery[MovieTable]

  def dbInit: Future[Unit] = {
    // Definición de la sentencia SQL de creación del schema
    val createSchema = movieQuery.schema.createIfNotExists
    // db.run Ejecuta una sentencia SQL, devolviendo un Future
    db.run(createSchema)
  }

  def getAll: Future[Seq[MovieTable#TableElementType]] = {
    //val q = movieQuery.sortBy(x => x.id)
    val q = movieQuery.sortBy(_.id)
    db.run(q.result)
  }

  def getOne(id: String): Future[Option[MovieTable#TableElementType]] = {
    val q = movieQuery.filter(_.id === id)
    db.run(q.result.headOption)
  }

  def create(movie: Movie): Future[Option[MovieTable#TableElementType]] = {
    val insert = movieQuery += movie
    db.run(insert).flatMap(_ => getOne(movie.id.getOrElse("")))
  }

  def update(id: String, movie: Movie): Future[Option[MovieTable#TableElementType]] = {
    val q = movieQuery.filter(_.id === id && movie.id.contains(id))
    val update = q.update(movie)
    db.run(update).flatMap(_ => db.run(q.result.headOption))
  }

  def delete(id: String): Future[Option[MovieTable#TableElementType]] = {
    val q = movieQuery.filter(_.id === id)
    for {
      objeto <- db.run(q.result.headOption)
      _ <- db.run(q.delete)
    } yield objeto
  }
}