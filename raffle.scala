import scala.util.Random

import cats.effect.*
import cats.syntax.all.*
import org.http4s.*
import org.http4s.ember.client.EmberClientBuilder

def displayStargazers(users: List[GithubUser]): IO[Unit] =
  users.traverse_(u => IO.println(s"> ${u.login}"))

def randomUser(users: List[GithubUser]): IO[Option[GithubUser]] =
  if users.isEmpty then IO.none
  else IO(Random.nextInt(users.size)).map(users.get)

object Raffle:
  // format: off
  def run(
      repo: String,
      auth: Option[Header.Raw],
      display: Boolean
  ): IO[Unit] =
    EmberClientBuilder.default[IO].build.use { cli =>
      for
        _ <- IO.println(s"⚠️ Missing Github auth token, default rate limit of 60 reqs/hour ⚠️").whenA(auth.isEmpty)
        uri = Uri.unsafeFromString(s"https://api.github.com/repos/$repo/stargazers")
        s <- fetchStargazers(cli, uri, auth)
        _ <- displayStargazers(s.users).whenA(display)
        _ <- IO.println(s"ℹ️ ${s.requests} requests made to Github API ℹ️")
        w <- randomUser(s.users)
        _ <- w.traverse_ { x =>
          IO.println(s"\n🏆🏆🏆 @${x.login} 🏆🏆🏆") *>
            IO.println(s"\n * from ${s.users.size} 🌟 stargazers!\n")
        }
      yield ()
    }.recoverWith {
      case FetchError(st) =>
        IO.println(s"🔥🔥🔥 Failed to fetch stargazers: ${st.show} 🔥🔥🔥")
    }
  // format: on
