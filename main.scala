import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.effect.*
import org.http4s.Header
import org.typelevel.ci.*

object Main
    extends CommandIOApp(
      name = "stargazers-raffle",
      header = "Stargazers Raffle ",
      version = "0.0.1"
    ):

  val authorOpts: Opts[String] =
    Opts.argument[String](metavar = "author")

  val repoOpts: Opts[String] =
    Opts.argument[String](metavar = "repo")

  val displayUsersFlag: Opts[Boolean] =
    Opts.flag("show-all-users", help = "Display all the stargazers before raffle", short = "s").orFalse

  val postWinnerFlag: Opts[Boolean] =
    Opts.flag("post-winner", help = "Post the winner on the designated Github issue").orFalse

  val ghTokenOpts: Opts[Option[String]] =
    Opts.env[String]("GH_TOKEN", help = "Github personal access token").orNone

  val main: Opts[IO[ExitCode]] =
    (
      authorOpts,
      repoOpts,
      ghTokenOpts,
      displayUsersFlag,
      postWinnerFlag
    ).mapN { (author, repo, token, display, shouldPost) =>
      // default rate-limit is 60 req/hour, when using auth token goes up to 5000 req/hour
      val auth = token.map(t => Header.Raw(ci"Authorization", s"token $t"))
      Raffle.run(author, repo, auth, display, shouldPost).as(ExitCode.Success)
    }
