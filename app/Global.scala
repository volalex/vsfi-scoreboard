import controllers.{routes, Application}
import play.api.mvc._
import play.api.GlobalSettings

/**
 * Created with IntelliJ IDEA.
 * User: volkoval
 * Date: 20.06.13
 * Time: 18:58
 * To change this template use File | Settings | File Templates.
 */
object Global extends WithFilters(AuthorizedFilter("/godsPlace")) with GlobalSettings{}

object AuthorizedFilter {
    def apply(securedPath:String) = new AuthorizedFilter(securedPath)
  }

class AuthorizedFilter(securedPath:String) extends Filter{
    override def apply(next: RequestHeader => Result)(request: RequestHeader): Result = {
      if(authorizationRequired(request)) {
        if(!request.session.get("user").nonEmpty){
          print("Not Authorized")
          Application.Redirect(routes.Application.loginPage(Option(request.uri)))
        }
        else{
          next(request)
        }
      }
      else next(request)
    }

    private def authorizationRequired(request: RequestHeader) = {
      request.path.contains(securedPath)
    }
}
