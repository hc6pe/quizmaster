package app.flux.react.app

import hydro.flux.react.uielements.SbadminLayout
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

final class Layout(
    implicit sbadminLayout: SbadminLayout,
) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderPC { (_, props, children) =>
      implicit val router = props.router
      sbadminLayout(
        title = "Quizmaster",
        leftMenu = <.span(),
        pageContent = <.span(children),
      )
    }
    .build

  // **************** API ****************//
  def apply(router: RouterContext)(children: VdomNode*): VdomElement = {
    component(Props(router))(children: _*)
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
}
