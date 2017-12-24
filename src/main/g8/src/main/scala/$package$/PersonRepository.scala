package $package$

import java.util.UUID

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, PutItemRequest, ReturnValue}
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.github.dnvriend.lambda._
import com.github.dnvriend.lambda.annotation.HttpHandler
import play.api.libs.json.{Json, _}

import scala.collection.JavaConverters._

object Person {
  implicit val format: Format[Person] = Json.format[Person]
}
final case class Person(name: String, id: Option[String] = None)

class PersonRepository(tableName: String, ctx: SamContext) {
  val table: String = ctx.dynamoDbTableName(tableName)
  val db: AmazonDynamoDB = AmazonDynamoDBClientBuilder.defaultClient()

  def id: String = UUID.randomUUID.toString

  def put(id: String, person: Person): Unit = {
    db.putItem(
      new PutItemRequest()
        .withTableName(table)
        .withReturnValues(ReturnValue.NONE)
        .withItem(
          Map(
            "id" -> new AttributeValue(id),
            "json" -> new AttributeValue(Json.toJson(person.copy(id = Option(id))).toString)
          ).asJava
        )
    )
  }

  def get(id: String): Person = {
    val json = db.getItem(table, Map("id" -> new AttributeValue(id)).asJava)
      .getItem.get("json").getS
    Json.parse(json).as[Person]
  }

  def list: List[Person] = {
    db.scan(table, List("json").asJava)
      .getItems.asScala.flatMap(_.values().asScala).map(_.getS).toList
      .map(Json.parse)
      .map(_.as[Person])
  }
}

@HttpHandler(path = "/person", method = "post")
class PostPerson extends ApiGatewayHandler {
  override def handle(request: HttpRequest, ctx: SamContext): HttpResponse = {
    val repo = new PersonRepository("people", ctx)
    val id: String = repo.id
    val person = request.bodyOpt[Person].get
    repo.put(id, person)
    HttpResponse.ok.withBody(Json.toJson(person.copy(id = Option(id))))
  }
}

@HttpHandler(path = "/person", method = "get")
class GetListOfPerson extends ApiGatewayHandler {
  override def handle(request: HttpRequest, ctx: SamContext): HttpResponse = {
    val repo = new PersonRepository("people", ctx)
    HttpResponse.ok.withBody(Json.toJson(repo.list))
  }
}

@HttpHandler(path = "/person/{id}", method = "get")
class GetPerson extends ApiGatewayHandler {
  override def handle(request: HttpRequest, ctx: SamContext): HttpResponse = {
    val repo = new PersonRepository("people", ctx)
    request.pathParamsOpt[Map[String, String]].getOrElse(Map.empty).get("id")
      .fold(HttpResponse.notFound.withBody(Json.toJson("Person not found")))(id => {
        HttpResponse.ok.withBody(Json.toJson(repo.get(id)))
      })
  }
}