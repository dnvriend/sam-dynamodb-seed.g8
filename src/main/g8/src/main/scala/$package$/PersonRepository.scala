package $package$

import java.util.UUID

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, PutItemRequest, ReturnValue}
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.amazonaws.services.lambda.runtime.Context
import com.github.dnvriend.lambda.annotation.HttpHandler
import com.github.dnvriend.lambda.{ApiGatewayHandler, HttpRequest, HttpResponse}
import play.api.libs.json._

import scala.collection.JavaConverters._

object Person {
  implicit val format: Format[Person] = Json.format[Person]
}
final case class Person(id: Option[String], name: String, age: Int, lucky_number: Int = 0)

object PersonRepository {
  final val TableName = "sam-seed-dnvriend-people"
  val db: AmazonDynamoDB = AmazonDynamoDBClientBuilder.defaultClient()

  def put(id: String, person: Person): Unit = {
    db.putItem(
      new PutItemRequest()
        .withTableName(TableName)
        .withReturnValues(ReturnValue.NONE)
        .withItem(
          Map(
            "id" -> new AttributeValue(id),
            "json" -> new AttributeValue(Json.toJson(person).toString)
          ).asJava
        )
    )
  }

  def get(id: String): Person = {
    val json = db.getItem(TableName, Map("id" -> new AttributeValue(id)).asJava)
      .getItem.get("json").getS
    Json.parse(json).as[Person]
  }
}

@HttpHandler(path = "/person", method = "post")
class PostPerson extends ApiGatewayHandler {
  override def handle(request: HttpRequest, ctx: Context): HttpResponse = {
    val person = request.bodyOpt[Person].get
    val id = UUID.randomUUID.toString
    PersonRepository.put(id, person)
    HttpResponse(200, Json.toJson(person.copy(id = Option(id))), Map.empty)
  }
}

object PersonId {
  implicit val format: Format[PersonId] = Json.format
}
final case class PersonId(id: String)
@HttpHandler(path = "/person/{id}", method = "get")
class GetPerson extends ApiGatewayHandler {
  override def handle(request: HttpRequest, ctx: Context): HttpResponse = {
    val person = PersonRepository.get(request.pathParamsOpt[PersonId].get.id)
    HttpResponse(200, Json.toJson(person), Map.empty)
  }
}