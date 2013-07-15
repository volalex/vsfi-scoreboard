package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import java.util.Date


case class Task(id: Pk[Long], name: String, taskText: String)

object Task {
  val simple = {
    get[Pk[Long]]("task.id") ~
      get[String]("task.name") ~
      get[String]("task.task_text") map {
      case id ~ name ~ taskText => new Task(id, name, taskText)
    }
  }


  def findById(id: Long): Option[Task] = {
    DB.withConnection {
      implicit connection =>
        SQL("SELECT * FROM task where id={id}").on('id -> id).as(Task.simple.singleOpt)
    }
  }

  def solve(team: Team, task: Task, score: Int) = {
    DB.withConnection {
      implicit connection =>
        if (team.solvedTasks.contains(task.id.get)) {
          score match {
            case 0 => SQL("DELETE from task_to_team WHERE team_id={team_id} and task_id={task_id}")
              .on('team_id->team.id.get,'task_id->task.id.get).executeUpdate()
            case _ => SQL("UPDATE task_to_team SET score={score} where team_id={team_id} and task_id={task_id}")
              .on('score -> score, 'team_id -> team.id.get, 'task_id -> task.id.get).executeUpdate()
          }
        }
        else {
          SQL(
            """
              |INSERT into task_to_team (team_id,task_id,score,solved_at)
              |values ({team_id},{task_id},{score},{solved_at})
            """.stripMargin
          ).on(
            'team_id -> team.id.get,
            'task_id -> task.id.get,
            'score -> score,
            'solved_at -> new Date().getTime
          ).executeUpdate()
        }
    }
  }

  def list(): List[Task] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from task").as(Task.simple *)
    }
  }

  def insert(task: Task) = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
            |insert into task(name,task_text)
            |values({name}, {task_text})
          """.stripMargin).on(
          'name -> task.name,
          'task_text -> task.taskText
        ).executeUpdate()
    }
  }

  def update(id: Long, task: Task) = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          update task
          set name = {name}, task_text = {task_text}
          where id = {id}
          """
        ).on(
          'id -> id,
          'name -> task.name,
          'task_text -> task.taskText
        ).executeUpdate()
    }
  }

  def delete(id: Long) = {
    DB.withConnection {
      implicit connection =>
        SQL("delete from task where id={id}").on('id -> id).executeUpdate()
    }
  }

}


