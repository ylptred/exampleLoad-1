package load
import io.gatling.core.scenario.Simulation
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import java.util.concurrent.ThreadLocalRandom

class LoadScript extends Simulation {
  val search_feeder = csv("search.csv").random

  val search =
    exec(http("Home").get("/"))
      .pause(1)
      .feed(search_feeder)
      .exec(
        http("Search")
          .get("/catalog/?q=#{searchCriterion}")
      )
      .pause(1)
      .exec(http("Select").get("#{searchItemName}").check(status.is(200)))
      .pause(1)


  val pages = List(
    "/supermarket",
    "/landing/megarasprodazha",
    "/aktsii-i-promokody",
    "/help",
    "/multicart"
  )

  val browse =
    repeat(3, "i") {
      exec(http(s"Page $pages(#{i})").get(s"$pages(#{i})")).pause(1)
    }


  val edit_feeder = csv("auth.csv").random

  val seller_auth =
    tryMax(2) {
      exec(http("Auth Home").get("/auth"))
        .pause(1)
        .feed(edit_feeder)
        .exec(
          http("Auth")
            .post("/auth")
            .formParam("smm-input-1", "#{login}")
            .formParam("smm-input-2", "#{password}")
            .check(
              status.is { session =>
                200 + ThreadLocalRandom.current().nextInt(2)
              }
            )
        )
    }
      .exitHereIfFailed

  val httpUser = http
    .baseURL("https://sbermegamarket.ru/")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val httpSeller = http
    .baseURL("https://partner.sbermegamarket.ru/")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val users = scenario("Users").exec(search, browse)
  val sellers = scenario("Seller").exec(search, browse, seller_auth)

  setUp(

    users.inject(rampUsers(1000) over (10 minutes))
      .protocols(httpUser) ,
    sellers.inject(rampUsers(1000) over (10 minutes))
      .protocols(httpSeller)

  ).maxDuration(11 minutes)

}