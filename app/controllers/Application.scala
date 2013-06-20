package controllers

import play.api.mvc._
import play.api.data._
import play.api.libs.json.Json._
import play.api.libs.json.Writes._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models.{Team, Task}
import anorm.{NotAssigned, Pk}
import views.html
import scala.Predef._
import eu.henkelmann.actuarius.ActuariusTransformer


object Application extends Controller {

  val ipPattern = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"

  val AdminHome = Redirect(routes.Application.adminList())

  val transformer = new ActuariusTransformer

  val taskForm = Form(
    mapping(
      "id" -> ignored(NotAssigned: Pk[Long]),
      "name" -> nonEmptyText(maxLength = 100),
      "taskText" -> nonEmptyText
    )(Task.apply)(Task.unapply)
  )

  val teamForm = Form(
    mapping(
      "id" -> ignored(NotAssigned: Pk[Long]),
      "name" -> nonEmptyText(maxLength = 100),
      "dnsIp" -> text.verifying(pattern(ipPattern.r, name = "Валидный IP адрес",
        error = "Строка не является валидным IP адресом"))
    )(Team.apply)(Team.unapply)
  )

  val solveForm = Form(
    tuple(
      "taskId" -> longNumber(min = 1),
      "teamId" -> longNumber(min = 1),
      "score" -> number(min = 0)
    )
  )

  def transformTasks(tasks: List[Task]): List[Task] = tasks.map(task => task.copy(taskText = transformer(task.taskText)))

  def index = Action {
    Ok(html.index(transformTasks(Task.list()), Team.list(),solveForm))
  }

  def solveTask = Action {
    implicit connection =>
      solveForm.bindFromRequest().fold(
        formWithErrors => BadRequest(formWithErrors.errorsAsJson),
        values => {
          val (taskId, teamId, score) = values
          Task.findById(taskId).map {
            case task =>
              Team.findById(teamId).map {
                case team =>
                  Task.solve(team, task, score)
                  Ok(toJson(Map("status" -> "success", "message" -> "Задание успешно зачтено")))
              }.getOrElse(BadRequest(toJson(Map("status" -> "error", "message" -> "Команда с id = %d не найдена".format(teamId)))))
          }.getOrElse(BadRequest(toJson(Map("status" -> "error", "message" -> "Задание с id = %d не найдено".format(taskId)))))
        }
      )
  }


  def adminList = Action {
    Ok(html.admin(
      Team.list(), transformTasks(Task.list()), teamForm, taskForm)
    )
  }

  //Teams CRUD

  def createTeam = Action {
    Ok(html.createTeam(teamForm))
  }

  def saveTeam = Action {
    implicit request =>

      teamForm.bindFromRequest().fold(
        formWithErrors => BadRequest(html.createTeam(formWithErrors)),
        team => {
          Team.insert(team)
          AdminHome.flashing("success" -> "Команда %s создана".format(team.name))
        }
      )
  }

  def editTeam(id: Long) = Action {
    Team.findById(id).map {
      team =>
        Ok(html.editTeam(id, teamForm.fill(team)))
    }.getOrElse(NotFound)
  }

  def updateTeam(id: Long) = Action {
    implicit request =>
      teamForm.bindFromRequest.fold(
        formWithErrors => BadRequest(html.editTeam(id, formWithErrors)),
        team => {
          Team.update(id, team)
          AdminHome.flashing("success" -> "Команда %s обновлена".format(team.name))
        }
      )
  }

  def deleteTeam(id: Long) = Action {
    Team.delete(id)
    AdminHome.flashing("success" -> "Команда удалена")
  }

  //tasks CRUD

  def createTask() = Action {
    Ok(html.createTask(taskForm))
  }

  def saveTask = Action {
    implicit request =>
      taskForm.bindFromRequest().fold(
        formWithErrors => BadRequest(html.createTask(formWithErrors)),
        task => {
          Task.insert(task)
          AdminHome.flashing("success" -> "Задание %s создано".format(task.name))
        }
      )
  }

  def editTask(id: Long) = Action {
    Task.findById(id).map {
      task =>
        Ok(html.editTask(id, taskForm.fill(task)))
    }.getOrElse(NotFound)
  }

  def updateTask(id: Long) = Action {
    implicit request =>
      taskForm.bindFromRequest.fold(
        formWithErrors => BadRequest(html.editTask(id, formWithErrors)),
        task => {
          Task.update(id, task)
          AdminHome.flashing("success" -> "Задание %s обновлено".format(task.name))
        }
      )
  }

  def deleteTask(id: Long) = Action {
    Task.delete(id)
    AdminHome.flashing("success" -> "Задание удалено")
  }


}