# Address Lookup

## Overview
REST microservice that implements the lookup of postal and BFPO (British Forces Post Office) address details within the UK.

## Documentation
Please read the [**address-lookup API definition**](public/api/conf/1.0/docs/address-lookup-api.md) for more information.

## Dependencies
* SBT
* Java 8
* Play Framework
* `address-search-api` 

## Running / testing locally
Run `address-lookup` services using `sm2`:
```bash
sm2 start ADDRESS_LOOKUP_SERVICES
```

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

## License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
