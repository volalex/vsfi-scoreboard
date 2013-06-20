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
