package load
import io.gatling.core.scenario.Simulation
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import java.util.concurrent.ThreadLocalRandom

class LoadScript extends Simulation {
  val search_feeder = csv("search.csv").random // выбирает рандомную строчку из таблицы с данными для поиска


  // Процесс поиска: Домашняя страница -> Поиск -> Уточненный поиск
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


  // Список разделов(страниц) сайта
  val pages = List(
    "/supermarket",
    "/landing/megarasprodazha",
    "/aktsii-i-promokody",
    "/help",
    "/multicart"
  )

  // Браузинг сайта (посещение страниц из списка)
  val browse =
    repeat(4, "i") {
      exec(http(s"Page $pages(#{i})").get(s"$pages(#{i})")).pause(1)
    }


  val auth_feeder = csv("auth.csv").random // выбор рандомных строк из таблицы с аутентификационными данными продавцов

  // Процесс аутентификации продавца: посещение страницы аутентификации и ввод данных
  val seller_auth =
    tryMax(2) {
      exec(http("Auth Home").get("/auth"))
        .pause(1)
        .feed(auth_feeder)
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


  // конфиг http для пользователей
  val httpUser = http
    .baseURL("https://sbermegamarket.ru/")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  // конфиг http для продавцов
  val httpSeller = http
    .baseURL("https://partner.sbermegamarket.ru/")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  // Создание и запуск сценариев
  val users = scenario("Users").exec(search, browse)
  val sellers = scenario("Seller").exec(search, browse, seller_auth)

  // 1000 пользователей для каждого сценария на протяжении 10 минут
  setUp(

    users.inject(rampUsers(1000) over (10 minutes))
      .protocols(httpUser) ,
    sellers.inject(rampUsers(1000) over (10 minutes))
      .protocols(httpSeller)

  ).maxDuration(11 minutes)

}
