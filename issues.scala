import cats.effect.IO
import cats.syntax.show.*
import io.circe.Json
import org.http4s.*
import org.http4s.Method.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.client.Client
import org.http4s.client.dsl.io.*
import org.http4s.implicits.*

def postWinner(
    cli: Client[IO],
    body: String,
    auth: Option[Header.Raw]
): IO[Unit] =
  auth.fold(IO.println("⚠️ Missing Github auth token, skipping issue comment ⚠️ ")) { h =>
    val uri = uri"https://api.github.com/repos/gvolpe/stargazers-raffle/issues/1/comments"
    val jsonBody = Json.obj("body" -> Json.fromString(body))

    cli.run(POST(jsonBody ,uri, h)).map(_.status).use {
      case Status.Created =>
        IO.println(s"\n ✔️ Done. See $uri")
      case st =>
        IO.println(s"\n 🚫 Failed to post issue comment: ${st.show} 🚫")
    }
  }
