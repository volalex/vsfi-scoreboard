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
import play.api.Play.current
import play.api.Play
import utils.XSSTransformer
import play.api.data.validation.{Valid, ValidationError, Invalid, Constraint}
import scala.sys.process._


object Application extends Controller {

  val ipPattern = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"

  val AdminHome = Redirect(routes.Application.adminList())

  def addRecord(name:String,ip:String) = Seq(Play.application.configuration
    .getString("dns.add.script").getOrElse("/home/vsfi/dns_add.py"),name,ip).!

  def delRecord(name:String) = Seq(Play.application.configuration
    .getString("dns.del.script").getOrElse("/home/vsfi/dns_del.py"),name).!


  val transformer = new XSSTransformer

  val taskForm = Form(
    mapping(
      "id" -> ignored(NotAssigned: Pk[Long]),
      "name" -> nonEmptyText(maxLength = 100),
      "taskText" -> nonEmptyText
    )(Task.apply)(Task.unapply)
  )

  val loginForm = Form(
    tuple(
      "login" -> nonEmptyText,
      "password" -> nonEmptyText,
      "redirectUrl" -> play.api.data.Forms.optional(text)
    )
  )

  val teamForm = Form(
    mapping(
      "id" -> ignored(NotAssigned: Pk[Long]),
      "name" -> nonEmptyText(maxLength = 100),
      "dnsIp" -> text.verifying(pattern(ipPattern.r, name = "Валидный IP адрес",
        error = "Строка не является валидным IP адресом"),ipUnique)
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

  def ipUnique: Constraint[String] = Constraint[String]("Уникальный IP адресс") { ip =>
    if(!Team.isIPUnique(ip)) Invalid(ValidationError("IP адресс не уникален")) else Valid
  }

  def index = Action { implicit request=>
    Ok(html.index(transformTasks(Task.list()), Team.list().sortBy(_.fullScore)(Ordering[Option[Long]].reverse), solveForm))
  }

  def loginPage(redirectUrl:Option[String]) = Action { implicit request =>
    if(session.get("user").nonEmpty){
      Redirect(redirectUrl.getOrElse(default = "/godsPlace"),MOVED_PERMANENTLY)
    }
    else{
      Ok(html.login(loginForm.fill(("","",redirectUrl))))
    }

  }

  def doLogin() = Action { implicit request =>
    val pass = Play.application.configuration.getString("admin.pass").getOrElse("superDuperPassword")
    loginForm.bindFromRequest().fold(
      formWithErrors => BadRequest(html.login(formWithErrors)),
      values => { values match{
        case ("admin", `pass`,redirectUrl) => Redirect(redirectUrl.getOrElse(default = "/godsPlace"),MOVED_PERMANENTLY).withSession("user"->"admin")
        case  v => Redirect(routes.Application.loginPage(v._3)).flashing("error" -> "Неправильный логин или пароль")
      }
      }
    )
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
          if(Play.application.configuration.getBoolean("dns.add.script").nonEmpty){
            addRecord("team"+Team.getLastId,team.dnsIp)
          }
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
    if(Play.application.configuration.getBoolean("dns.del.script").nonEmpty){
      delRecord("team"+id)
    }
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