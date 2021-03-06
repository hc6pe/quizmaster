package app.flux.controllers

import scala.concurrent.duration._
import app.flux.router.AppPages
import app.flux.ClientApp.HtmlImage
import app.models.access.ModelFields
import app.models.quiz.Team
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.action.Action
import hydro.flux.action.Dispatcher
import hydro.flux.action.StandardActions
import hydro.flux.router.Page
import hydro.jsfacades.Audio
import hydro.models.access.JsEntityAccess
import hydro.models.modification.EntityModification

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.scalajs.js

final class SoundEffectController(implicit
    dispatcher: Dispatcher
) {
  private var currentPage: Page = _
  private val soundsPlaying: mutable.Set[SoundEffect] = mutable.Set()
  // Keep a reference to preloaded media to avoid garbage collection cleaning it up
  private val preloadedAudios: mutable.Buffer[Audio] = mutable.Buffer()

  dispatcher.registerPartialSync(dispatcherListener)
  preloadMedia()

  // **************** Public API ****************//
  def playNewSubmission(): Unit = playSoundEffect(SoundEffect.NewSubmission)
  def playRevealingSubmission(correct: Boolean): Unit = {
    if (correct) {
      playSoundEffect(SoundEffect.CorrectSubmission)
    } else {
      playSoundEffect(SoundEffect.IncorrectSubmission)
    }
  }
  def playTimerRunsOut(): Unit = playSoundEffect(SoundEffect.TimerRunsOut)
  def playScoreIncreased(): Unit = playSoundEffect(SoundEffect.ScoreIncreased)

  def timerAlmostRunningOutDetails: SoundDetails = SoundDetails(
    filepath = SoundEffect.TimerAlmostRunningOut.filepath,
    duration = java.time.Duration.ofSeconds(12).plusMillis(720),
    canPlayOnCurrentPage = canPlaySoundEffectsOnThisPage,
  )

  // **************** Public types ****************//
  case class SoundDetails(
      filepath: String,
      duration: java.time.Duration,
      canPlayOnCurrentPage: Boolean,
  )

  // **************** Private helper methods ****************//
  private def dispatcherListener: PartialFunction[Action, Unit] = {
    case StandardActions.SetPageLoadingState( /* isLoading = */ _, currentPage) =>
      this.currentPage = currentPage
  }

  private def canPlaySoundEffectsOnThisPage: Boolean = {
    currentPage match {
      case AppPages.Quiz              => true
      case _: AppPages.TeamController => true
      case _                          => false
    }
  }

  private def playSoundEffect(
      soundEffect: SoundEffect,
      minTimeBetweenPlays: Option[FiniteDuration] = None,
  ): Unit = logExceptions {
    if (canPlaySoundEffectsOnThisPage) {
      if (minTimeBetweenPlays.isDefined && (soundsPlaying contains soundEffect)) {
        // Skip
      } else {
        soundsPlaying.add(soundEffect)
        val audio = new Audio(soundEffect.filepath)
        audio.addEventListener(
          "ended",
          () => {
            val timeoutTime = minTimeBetweenPlays getOrElse (0.seconds)
            js.timers.setTimeout(timeoutTime)(logExceptions {
              soundsPlaying.remove(soundEffect)
            })
          },
        )

        println(s"  Playing ${soundEffect.filepath}..")
        audio.play()
      }
    }
  }

  def preloadMedia(): Unit = {
    println(s"  Preloading sound effects...")
    for (soundEffect <- SoundEffect.all) {
      val audio = new Audio(soundEffect.filepath)
      preloadedAudios.append(audio)
    }
  }

  private sealed abstract class SoundEffect(val filepath: String)
  private object SoundEffect {
    def all: Seq[SoundEffect] =
      Seq(
        NewSubmission,
        CorrectSubmission,
        IncorrectSubmission,
        ScoreIncreased,
        TimerRunsOut,
        TimerAlmostRunningOut,
      )

    case object NewSubmission extends SoundEffect("/assets/soundeffects/new_submission.mp3")
    case object CorrectSubmission extends SoundEffect("/assets/soundeffects/correct_submission.mp3")
    case object IncorrectSubmission extends SoundEffect("/assets/soundeffects/incorrect_submission.mp3")
    case object ScoreIncreased extends SoundEffect("/assets/soundeffects/score_increased.mp3")
    case object TimerRunsOut extends SoundEffect("/assets/soundeffects/timer_runs_out.mp3")
    case object TimerAlmostRunningOut extends SoundEffect("/assets/soundeffects/timer_almost_running_out.mp3")
  }
}
