![GitHub release (latest by date)](https://img.shields.io/github/v/release/hmrc/trader-services-route-one) ![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/hmrc/trader-services-route-one)

# trader-services-route-one

Backend microservice exposing an API of Trader Services on MDTP.

## Dependencies

testing

- PEGA API via IF/EIS
- [file-transmission-synchronous](https://github.com/hmrc/file-transmission-synchronous)

## API

### Create Case

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
        "entryDetails" : {
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
                },
                "reason" : "Humpty Dumpty sat on a wall"
            }
        },
        "uploadedFiles" : [ {
            "upscanReference" : "foo",
            "downloadUrl" : "https://www.w3.org/TR/PNG/iso_8859-1.txt",
            "uploadTimestamp" : "2018-04-24T09:30:00Z",
            "checksum" : "3aff1954277c4fc27603346901e4848b58fe3c8bed63affe6086003dd6c2b9fe",
            "fileName" : "iso_8859-1.txt",
            "fileMimeType" : "text/plain",
            "fileSize" : 6121
        } ],
        "eori" : "GB123456789012345"
    }

Example 201 success response payload

    {
        "correlationId" : "4327cf1f-5bcc-4c4a-acae-391588567d87",
        "result" : {
          "caseId" : "330XGBNZJO04",
          "generatedAt" : "2020-11-03T15:29:28.601Z"
          }
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

### Update Case

Method | Path | Description | Authorization
---|---|---|---
`POST` | `/update-case` | update existing case in the PEGA system | any GovernmentGateway authorized user

Header | Description
---|---
`x-correlation-id` | message correlation UUID (optional)

Response status | Description
---|---
201| when updated, body payload will be `{ "result" : "$CaseID" }`
400| when payload invalid or has not passed the validation

Example request payload 

    {
        "caseReferenceNumber": "PCE201103470D2CC8K0NH3",
        "typeOfAmendment": "WriteResponseAndUploadDocuments",
        "responseText":"An example response.",
        "uploadedFiles" : [ {
            "downloadUrl" : "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
            "uploadTimestamp" : "2018-04-24T09:30:00Z",
            "checksum" : "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
            "fileName" : "test.pdf",
            "fileMimeType" : "application/pdf"
        } ],
        "eori" : "GB123456789012345" <- optional
    }

Example 201 success response payload

    {
        "correlationId" : "4327cf1f-5bcc-4c4a-acae-391588567d87",
        "result" : {
          "caseId" : "330XGBNZJO04",
          "generatedAt" : "2020-11-03T15:29:28.601Z"
          }
    }

Example 400 error response payload

    {
        "correlationId" : "7fedc2d5-1bba-434b-87e6-4d4ec1757e31",
        "error" : {
            "errorCode" : "400",
            "errorMessage" : "invalid phone number"
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
