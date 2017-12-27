package $package$

import java.util.UUID

import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.github.dnvriend.lambda._
import com.github.dnvriend.lambda.annotation.HttpHandler
import com.github.dnvriend.repo.JsonRepository
import play.api.libs.json.{Json, _}

object Person {
  implicit val format: OFormat[Person] = Json.format[Person]
}

final case class Person(name: String, id: Option[String] = None)

@HttpHandler(path = "/person", method = "put")
class CreatePerson extends JsonDynamoRepoApiGatewayHandler[Person]("people") {
  override def handle(person: Option[Person], pathParams: Map[String, String], requestParams: Map[String, String], request: HttpRequest, ctx: SamContext, repo: JsonRepository): HttpResponse = {
    person.fold(HttpResponse.validationError.withBody(Json.toJson("Could not deserialize person"))) { person =>
      val id: String = UUID.randomUUID().toString
      repo.put(id, person)
      HttpResponse.ok.withBody(Json.toJson(person.copy(id = Option(id))))
    }
  }
}

@HttpHandler(path = "/person/{id}", method = "get")
class ReadPerson extends JsonDynamoRepoApiGatewayHandler[Person]("people") {
  override def handle(person: Option[Person], pathParams: Map[String, String], requestParams: Map[String, String], request: HttpRequest, ctx: SamContext, repo: JsonRepository): HttpResponse = {
    pathParams.get("id").fold(HttpResponse.notFound.withBody(Json.toJson("No id found in path")))(id => {
      repo.find[Person](id).fold(HttpResponse.validationError.withBody(Json.toJson("No person for id " + id))) { person =>
        HttpResponse.ok.withBody(Json.toJson(person))
      }
    })
  }
}

@HttpHandler(path = "/person/{id}", method = "post")
class UpdatePerson extends JsonDynamoRepoApiGatewayHandler[Person]("people") {
  override def handle(personOpt: Option[Person], pathParams: Map[String, String], requestParams: Map[String, String], request: HttpRequest, ctx: SamContext, repo: JsonRepository): HttpResponse = {
    val personAndId = for {
      id <- pathParams.get("id")
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
class DeletePerson extends JsonDynamoRepoApiGatewayHandler[Person]("people") with RequestStreamHandler {
  override def handle(personOpt: Option[Person], pathParams: Map[String, String], requestParams: Map[String, String], request: HttpRequest, ctx: SamContext, repo: JsonRepository): HttpResponse = {
    pathParams.get("id").fold(HttpResponse.notFound.withBody(Json.toJson("No id found in path")))(id => {
      repo.delete(id)
      HttpResponse.ok
    })
  }
}

@HttpHandler(path = "/person", method = "get")
class ListPersons extends JsonDynamoRepoApiGatewayHandler[Person]("people") {
  override def handle(personOpt: Option[Person], pathParams: Map[String, String], requestParams: Map[String, String], request: HttpRequest, ctx: SamContext, repo: JsonRepository): HttpResponse = {
    val listOfPerson: List[(String, Person)] = repo.list[Person]()
    val response: List[JsValue] = listOfPerson.map { case (id, p) => Json.obj("id" -> id) ++ Json.toJsObject(p) }
    HttpResponse.ok.withBody(Json.toJson(response))
  }
}

