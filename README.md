# Apache Fineract CN Provisioner [![Build Status](https://api.travis-ci.com/apache/fineract-cn-provisioner.svg?branch=develop)](https://travis-ci.com/apache/fineract-cn-provisioner) [![Docker Cloud Build Status](https://img.shields.io/docker/cloud/build/apache/fineract-cn-provisioner)](https://hub.docker.com/r/apache/fineract-cn-provisioner/builds)


This service provisions services for tenants of an Apache Fineract CN installation.
[Read more](https://cwiki.apache.org/confluence/display/FINERACT/Fineract+CN+Project+Structure#FineractCNProjectStructure-provisioner).

## Requirements

### Fineract CN Libraries

01. fineract-cn-lang
02. fineract-cn-postgresql
03. fineract-cn-anubis
04. fineract-cn-permitted-feign-client
05. fineract-cn-identity
06. fineract-cn-api
07. fineract-cn-async
08. fineract-cn-cassandra
09. fineract-cn-crypto
10. fineract-cn-test

### Environment Variables 

The following variables are required for publishg the binary artifacts. Values are examples, change them to fit your environment.
```console
ARTIFACTORY_URL = 'https://url-to-artifactory/artifactory/'
ARTIFACTORY_USER = 'user'
ARTIFACTORY_PASSWORD = 'password'
ARTIFACTORY_REPOKEY = 'libs-snapshot-local'
```

## Abstract
Apache Fineract CN is an application framework for digital financial services, a system to support nationwide and cross-national financial transactions and help to level and speed the creation of an inclusive, interconnected digital economy for every nation in the world.

## Versioning
The version numbers follow the [Semantic Versioning](http://semver.org/) scheme.

In addition to MAJOR.MINOR.PATCH the following postfixes are used to indicate the development state.

* BUILD-SNAPSHOT - A release currently in development.
* M - A _milestone_ release include specific sets of functions and are released as soon as the functionality is complete.
* RC - A _release candidate_ is a version with potential to be a final product, considered _code complete_.
* RELEASE - indicates that this release is the best available version and is recommended for all usage.

The versioning layout is {MAJOR}.{MINOR}.{PATCH}-{INDICATOR}[.{PATCH}]. Only milestones and release candidates can  have patch versions. Some examples:

1.2.3-BUILD-SNAPSHOT
1.3.5-M.1
1.5.7-RC.2
2.0.0-RELEASE

## License
See [LICENSE](LICENSE) file.
