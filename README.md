# Address Lookup

## Overview
REST microservice that implements the lookup of postal and BFPO (British Forces Post Office) address details within the UK.

## Documentation
Please [browse the docs](docs) for more info, or dive straight into
the [**address-lookup API definition**](docs/address-lookup/address-lookup-api.md).


## Dependencies
* SBT
* Java 8
* Play Framework

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
