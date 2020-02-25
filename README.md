# Address Lookup

## Overview
REST microservice that implements the lookup of postal and BFPO (British Forces Post Office) address details within the UK.

## Documentation
* [~~API v1~~](https://github.com/hmrc/addresses/blob/master/docs/address-lookup/v1/address-lookup-api.md)
* [API v2](https://github.com/hmrc/addresses/blob/master/docs/address-lookup/v2/address-lookup-api.md)

## Dependencies
* SBT
* Java 8
* Play Framework
* Elasticsearch

## Development
The unit tests can be run as follows:
```bash
sbt test
```

The integration tests can be run as follows:
```bash
sbt it:test
```

## Local Demo Mode
```bash
sbt "run 9022"
```

## Associated Repositories
| Repository                                                                                                   | Description                             |
| ------------------------------------------------------------------------------------------------------------ | --------------------------------------- |
| [https://github.com/hmrc/addresses/](https://github.com/hmrc/addresses/)                                     | Documentation                           |
| [https://github.com/hmrc/address-reputation-ingester/](https://github.com/hmrc/address-reputation-ingester/) | Automated ingestion                     |
| [https://github.com/hmrc/address-reputation-store/](https://github.com/hmrc/address-reputation-store/)       | Abstraction layer for accessing MongoDB |

## License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
