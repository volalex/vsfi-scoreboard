package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._
import java.util.Date

/**
 * Created with IntelliJ IDEA.
 * User: volal_000
 * Date: 16.06.13
 * Time: 15:21
 */
case class Team(id:Pk[Long] = NotAssigned,name:String,dnsIp:String) {
  lazy val solvedTasks: Map[Long,Result] = DB.withConnection{ implicit connection =>
    SQL(
      """
        |select task_id, score, solved_at from task_to_team where team_id={id}
      """.stripMargin).on('id -> this.id)
      .as(Team.taskIdWithResult *).toMap
  }
  lazy val fullScore:Option[Long] = DB.withConnection { implicit connection =>
    SQL(
      """
        |select sum(score) from task_to_team where team_id={id}
      """.stripMargin
    ).on('id->this.id)().map(row=>row[Option[Long]]("sum(score)")).head

  }
}

case class Result(score:Int,solvedAt:Option[Date])

object Team {
  val taskIdWithResult = {
    get[Long]("task_id") ~
    get[Int]("score") ~
    get[Long]("solved_at") map {
      case taskId~score~solvedAt => taskId -> Result(score,Option(new Date(solvedAt)))
    }
  }
  val simple = {
    get[Pk[Long]]("team.id") ~
    get[String]("team.name") ~
    get[String]("team.dns_ip") map {
      case id~name~dnsIp => Team(id,name,dnsIp)
    }
  }

  def findById(id:Long):Option[Team] = {
    DB.withConnection{implicit connection =>
      SQL("SELECT * FROM team where id={id}").on('id->id).as(Team.simple.singleOpt)
    }
  }

  def list():List[Team] = {
    DB.withConnection{ implicit connection =>
      SQL("select * from team").as(Team.simple *)
    }
  }

  def isIPUnique(ip:String):Boolean = {
    DB.withConnection{ implicit  connection =>
      val test = SQL("SELECT count(*) from team where dns_ip={dns_ip}").on('dns_ip -> ip) ()
        .map( row=>row[Option[Long]]("count(*)")).head
      test.getOrElse(0)==0
    }
  }

  def insert(team:Team) = {
    DB.withConnection{implicit connection =>
      SQL(
        """
          |insert into team(name,dns_ip)
          |values({name}, {dns_ip})
        """.stripMargin).on(
        'name -> team.name,
        'dns_ip -> team.dnsIp
      ).executeUpdate()
    }
  }

  def getLastId = {
    DB.withConnection{ implicit connection=>
      SQL(
        """
          |select MAX(id) from team
        """.stripMargin).as(scalar[Long].single)
    }
  }

  def update(id: Long, team:Team) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          update team
          set name = {name}, dns_ip = {dns_ip}
          where id = {id}
        """
      ).on(
        'id -> id,
        'name -> team.name,
        'dns_ip -> team.dnsIp
      ).executeUpdate()
    }
  }

  def delete(id:Long) = {
    DB.withConnection{ implicit connection =>
      SQL("delete from team where id={id}").on('id->id).executeUpdate()
    }
  }

}
