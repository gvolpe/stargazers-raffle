import scala.util.control.NoStackTrace
import scala.util.Random

import cats.data.NonEmptyList
import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.effect.*
import io.circe.Codec
import org.http4s.*
import org.http4s.Method.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.client.dsl.io.*
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.ci.*

case class GithubUser(
    login: String,
    avatar_url: String,
    html_url: String
) derives Codec.AsObject

//NonEmptyList(Link: <https://api.github.com/repositories/298870508/stargazers?page=2>; rel="next", <https://api.github.com/repositories/298870508/stargazers?page=4>; rel="last")
def nextUri(h: NonEmptyList[Header.Raw]): Option[Uri] =
  h.map(_.value).head.split(",").toList.find(_.contains("rel=\"next\"")).flatMap { x =>
    Uri.fromString(x.trim.takeWhile(_ != '>').drop(1)).toOption
  }

case class FetchError(st: Status) extends Exception(st.show) with NoStackTrace

def fetchUsers(
    cli: Client[IO],
    baseUri: Uri,
    auth: Option[Header.Raw]
): IO[List[GithubUser]] =
  def go(users: List[GithubUser], uri: Uri): IO[List[GithubUser]] =
    val req = auth.fold(GET(uri))(x => GET(uri, x))
    cli.run(req).use { resp =>
      resp.status match
        case Status.Ok =>
          resp.asJsonDecode[List[GithubUser]].flatMap { xs =>
            resp.headers.get(ci"Link").flatMap(nextUri) match
              case Some(next) =>
                go(users ++ xs, next)
              case None =>
                IO.pure(users ++ xs)
          }
        case _ =>
          IO.raiseError(FetchError(resp.status))
    }

  go(Nil, baseUri)

def displayUsers(users: List[GithubUser]): IO[Unit] =
  users.traverse_(u => IO.println(s"> ${u.login}"))

def randomUser(users: List[GithubUser]): IO[Option[GithubUser]] =
  if users.isEmpty then IO.none
  else IO(Random.nextInt(users.size)).map(users.get)

// format: off
def program(
    repo: String,
    auth: Option[Header.Raw],
    display: Boolean
): IO[Unit] =
  EmberClientBuilder.default[IO].build.use { cli =>
    for
      _ <- IO.println(s"⚠️ Missing Github auth token, default to rate limit of 60 reqs/hour ⚠️").whenA(auth.isEmpty)
      b = Uri.unsafeFromString(s"https://api.github.com/repos/$repo/stargazers")
      u <- fetchUsers(cli, b, auth)
      _ <- displayUsers(u).whenA(display)
      w <- randomUser(u)
      _ <- w.traverse_ { x =>
        IO.println(s"\n    @${x.login}    ") *>
          IO.println(s" ﬦ from ${u.size} 🌟 stargazers!\n")
      }
    yield ()
  }.recoverWith {
    case FetchError(st) =>
      IO.println(s"🔥🔥🔥 Failed to fetch stargazers: ${st.show} 🔥🔥🔥")
  }
// format: on

/*    @username    ﬦ from 50 🌟 stargazers! */
object Raffle
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

  val ghTokenOpts: Opts[Option[String]] =
    Opts.env[String]("GH_TOKEN", help = "Github personal access token").orNone

  val main: Opts[IO[ExitCode]] =
    (authorOpts, repoOpts, ghTokenOpts, displayUsersFlag).mapN { (author, repo, token, display) =>
      // default rate-limit is 60 req/hour, when using auth token goes up to 5000 req/hour
      val auth = token.map(t => Header.Raw(ci"Authorization", s"token $t"))
      program(s"$author/$repo", auth, display).as(ExitCode.Success)
    }
