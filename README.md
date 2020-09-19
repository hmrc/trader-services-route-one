# Trader Services 

Backend microservice exposing an API of Trader Services on MDTP.

[ ![Download](https://api.bintray.com/packages/hmrc/releases/trader-services/images/download.svg) ](https://bintray.com/hmrc/releases/trader-services/_latestVersion)

## Running the tests

    sbt test it:test

## Running the tests with coverage

    sbt clean coverageOn test it:test coverageReport

## Running the app locally

    sm --start TRADER_SERVICES_ALL
    sm --stop TRADER_SERVICES_ROUTE_ONE
    sbt run

It should then be listening on port 9380

### License


This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
