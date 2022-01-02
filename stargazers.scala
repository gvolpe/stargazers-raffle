import scala.util.control.NoStackTrace

import cats.data.NonEmptyList
import cats.effect.*
import io.circe.Codec
import org.http4s.*
import org.http4s.Method.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.client.dsl.io.*
import org.typelevel.ci.*

case class GithubUser(
    login: String,
    avatar_url: String,
    html_url: String
) derives Codec.AsObject

case class StargazersResponse(
  users: List[GithubUser],
  requests: Int
)

case class FetchError(st: Status) extends NoStackTrace

//NonEmptyList(Link: <https://api.github.com/repositories/298870508/stargazers?page=2>; rel="next", <https://api.github.com/repositories/298870508/stargazers?page=4>; rel="last")
def nextUri(h: NonEmptyList[Header.Raw]): Option[Uri] =
  h.map(_.value).head.split(",").toList.find(_.contains("rel=\"next\"")).flatMap { x =>
    Uri.fromString(x.trim.takeWhile(_ != '>').drop(1)).toOption
  }

def fetchStargazers(
    cli: Client[IO],
    baseUri: Uri,
    auth: Option[Header.Raw]
): IO[StargazersResponse] =
  def go(users: List[GithubUser], uri: Uri, acc: Int): IO[(List[GithubUser], Int)] =
    cli.run(auth.fold(GET(uri))(GET(uri, _))).use { resp =>
      resp.status match
        case Status.Ok =>
          resp.asJsonDecode[List[GithubUser]].flatMap { xs =>
            resp.headers.get(ci"Link").flatMap(nextUri) match
              case Some(next) =>
                go(users ++ xs, next, acc + 1)
              case None =>
                IO.pure(users ++ xs -> acc)
          }
        case _ =>
          IO.raiseError(FetchError(resp.status))
    }

  go(Nil, baseUri, 1).map(StargazersResponse.apply)
