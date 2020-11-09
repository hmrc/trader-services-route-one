![GitHub release (latest by date)](https://img.shields.io/github/v/release/hmrc/trader-services-route-one) ![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/hmrc/trader-services-route-one)

# Trader Services 

Backend microservice exposing an API of Trader Services on MDTP.

## API

Method | Path | Description | Authorization
---|---|---|---
`POST` | `/create-case` | create new case in the PEGA system or report duplicate | any GovernmentGateway authorized user

Header | Description
---|---
`x-correlation-id` | message correlation UUID (optional)

Response status | Description
---|---
201| when created, body payload will be `{ "result" : "$CaseID" }`
400| when payload invalid or has not passed the validation
409| when duplicate case

Example request payload 

    {
        "declarationDetails" : {
            "epu" : "123",
            "entryNumber" : "000000Z",
            "entryDate" : "2020-10-05"
        },
        "questionsAnswers" : {
            "export" : {
            "requestType" : "New",
            "routeType" : "Route2",
            "freightType" : "Air",
            "vesselDetails" : {
                "vesselName" : "Foo Bar",
                "dateOfArrival" : "2020-10-19",
                "timeOfArrival" : "10:09:00"
            },
            "contactInfo" : {
                "contactName" : "Bob",
                "contactEmail" : "name@somewhere.com",
                "contactNumber" : "01234567891"
            }
            }
        },
        "uploadedFiles" : [ {
            "downloadUrl" : "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
            "uploadTimestamp" : "2018-04-24T09:30:00Z",
            "checksum" : "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
            "fileName" : "test.pdf",
            "fileMimeType" : "application/pdf"
        } ],
        "eori" : "GB123456789012345"
    }

Example 201 success response payload

    {
        "correlationId" : "4327cf1f-5bcc-4c4a-acae-391588567d87",
        "result" : "330XGBNZJO04"
    }

Example 400 error response payload

    {
        "correlationId" : "7fedc2d5-1bba-434b-87e6-4d4ec1757e31",
        "error" : {
            "errorCode" : "400",
            "errorMessage" : "invalid phone number"
        }
    } 

Example 409 duplicate case error payload

    {
        "correlationId" : "7fedc2d5-1bba-434b-87e6-4d4ec1757e31",
        "error" : {
            "errorCode" : "409",
            "errorMessage" : "XYZ1234567890"
        }
    }


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
