import scala.util.Random

import cats.effect.*
import cats.syntax.all.*
import org.http4s.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.*

def displayStargazers(users: List[GithubUser]): IO[Unit] =
  users.traverse_(u => IO.println(s"> ${u.login}"))

def randomUser(users: List[GithubUser]): IO[Option[GithubUser]] =
  if users.isEmpty then IO.none
  else IO(Random.nextInt(users.size)).map(users.get)

object Raffle:
  // format: off
  def run(
      author: String,
      repo: String,
      auth: Option[Header.Raw],
      display: Boolean,
      shouldPost: Boolean
  ): IO[Unit] =
    EmberClientBuilder.default[IO].build.use { cli =>
      for
        _ <- IO.println(s"⚠️ Missing Github auth token, default rate limit of 60 reqs/hour ⚠️").whenA(auth.isEmpty)
        u = uri"https://api.github.com/repos" / author / repo / "stargazers"
        s <- fetchStargazers(cli, u, auth)
        _ <- displayStargazers(s.users).whenA(display)
        w <- randomUser(s.users)
        _ <- w.traverse_ { x =>
          val body = s"""
            |ℹ️ ${s.requests} requests made to Github API ℹ️
            |
            |🏆🏆🏆 @${x.login} 🏆🏆🏆
            |
            | * from ${s.users.size} 🌟 stargazers of https://github.com/$author/$repo!
          """.stripMargin('|').trim
          IO.println(body) *> postWinner(cli, body, auth).whenA(shouldPost)
        }
      yield ()
    }.recoverWith {
      case FetchError(st) =>
        IO.println(s"🔥🔥🔥 Failed to fetch stargazers: ${st.show} 🔥🔥🔥")
    }
  // format: on
