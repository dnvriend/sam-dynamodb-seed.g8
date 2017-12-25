package $package$

import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.github.dnvriend.dynamodb.repo.{JsonRepository, JsonRepositoryApiGatewayHandler}
import com.github.dnvriend.lambda._
import com.github.dnvriend.lambda.annotation.HttpHandler
import play.api.libs.json.{Json, _}

object Person {
  implicit val format: OFormat[Person] = Json.format[Person]
}
final case class Person(name: String, id: Option[String] = None)

@HttpHandler(path = "/person", method = "put")
class CreatePerson extends JsonRepositoryApiGatewayHandler[Person]("people") {
  override def handle(person: Option[Person], repo: JsonRepository, request: HttpRequest, ctx: SamContext): HttpResponse = {
    person.fold(HttpResponse.validationError.withBody(Json.toJson("Could not deserialize person"))) { person =>
      val id: String = repo.id
      repo.put(id, person)
      HttpResponse.ok.withBody(Json.toJson(person.copy(id = Option(id))))
    }
  }
}

@HttpHandler(path = "/person/{id}", method = "get")
class ReadPerson extends JsonRepositoryApiGatewayHandler[Person]("people") {
  override def handle(person: Option[Person], repo: JsonRepository, request: HttpRequest, ctx: SamContext): HttpResponse = {
    request.pathParamsOpt[Map[String, String]].getOrElse(Map.empty).get("id")
      .fold(HttpResponse.notFound.withBody(Json.toJson("No id found in path")))(id => {
        repo.find[Person](id).fold(HttpResponse.validationError.withBody(Json.toJson(s"No person for id '$id'"))) { person =>
          HttpResponse.ok.withBody(Json.toJson(person))
        }
      })
  }
}

@HttpHandler(path = "/person/{id}", method = "post")
class UpdatePerson extends JsonRepositoryApiGatewayHandler[Person]("people") {
  override def handle(personOpt: Option[Person], repo: JsonRepository, request: HttpRequest, ctx: SamContext): HttpResponse = {
    val personAndId = for {
      id <- request.pathParamsOpt[Map[String, String]].getOrElse(Map.empty).get("id")
      person <- personOpt
    } yield (id, person)

    personAndId.fold(HttpResponse.validationError.withBody(Json.toJson("Could not deserialize person or id"))) {
      case (id, person) =>
      repo.update(id, person)
      HttpResponse.ok.withBody(Json.toJson(person.copy(id = Option(id))))
    }
  }
}

@HttpHandler(path = "/person/{id}", method = "delete")
class DeletePerson extends JsonRepositoryApiGatewayHandler[Person]("people") with RequestStreamHandler {
  override def handle(person: Option[Person], repo: JsonRepository, request: HttpRequest, ctx: SamContext): HttpResponse = {
    request.pathParamsOpt[Map[String, String]].getOrElse(Map.empty).get("id")
      .fold(HttpResponse.notFound.withBody(Json.toJson("No id found in path")))(id => {
        repo.delete(id)
        HttpResponse.ok
      })
  }
}

@HttpHandler(path = "/person", method = "get")
class ListPersons extends JsonRepositoryApiGatewayHandler[Person]("people") {
  override def handle(person: Option[Person], repo: JsonRepository, request: HttpRequest, ctx: SamContext): HttpResponse = {
    val listOfPerson: List[(String, Person)] = repo.list[Person]()
    val response: List[JsValue] = listOfPerson.map { case (id, p) => Json.obj("id" -> id) ++ Json.toJsObject(p) }
    HttpResponse.ok.withBody(Json.toJson(response))
  }
}

