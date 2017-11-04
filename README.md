clj-multihash
=============

[![Circle CI](https://img.shields.io/circleci/project/github/multiformats/clj-multihash.svg?style=flat-square)](https://circleci.com/gh/multiformats/clj-multihash)
[![codecov.io](https://img.shields.io/codecov/c/github/multiformats/clj-multihash.svg?style=flat-square&branch=develop)](https://codecov.io/github/multiformats/clj-multihash?branch=develop)
[![API codox](https://img.shields.io/badge/doc-API-blue.svg)](https://multiformats.github.io/clj-multihash/api/multihash.core.html)
[![marginalia docs](https://img.shields.io/badge/doc-marginalia-blue.png)](https://multiformats.github.io/clj-multihash/marginalia/uberdoc.html)

[![](https://img.shields.io/badge/freenode-%23ipfs-blue.svg?style=flat-square)](https://webchat.freenode.net/?channels=%23ipfs)
[![](https://img.shields.io/badge/readme%20style-standard-brightgreen.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)
[![](https://img.shields.io/badge/project-multiformats-blue.svg?style=flat-square)](https://github.com/multiformats/multiformats)

> Clojure implementation of the Multihash spec

A Clojure library implementing the
[multihash](https://github.com/multiformats/multihash) standard. This provides an
extensible, efficient binary representation for cryptographic hashes.

## Table of Contents

- [Install](#install)
- [Usage](#usage)
  - [Serialization](#serialization)
- [Maintainers](#maintainers)
- [Contribute](#contribute)
- [License](#license)

## Install

Library releases are published on Clojars. To use the latest version with
Leiningen, add the following dependency to your project definition:

[![Clojars Project](https://clojars.org/mvxcvi/multihash/latest-version.svg)](https://clojars.org/mvxcvi/multihash)

## Usage

The main focus of this library is the `Multihash` type, which serves as an
immutable representation of the output digest for a specific hash algorithm.
Multihashes are sortable, support metadata, and implement value-based equality
and hash-code generation.

```clojure
(require '[multihash.core :as multihash]
         '[multihash.digest :as digest])

; List the known hash functions:
=> (keys multihash/algorithm-codes)
(:sha1 :sha2-256 :sha2-512 :sha3 :blake2b :blake2s)

; Manually create a multihash value:
=> (multihash/create :sha1 "0f1e2d3c4b5a6978")
#<Multihash hash:sha1:0f1e2d3c4b5a6978>

; Or use one of the constructors for built-in Java implementations:
=> (keys digest/functions)
(:sha1 :sha2-256 :sha2-512)

=> (def mhash (digest/sha2-256 "foo bar baz"))
#'user/mhash

; Properties can be accessed using keyword lookups:
=> (:algorithm mhash)
:sha2-256
=> (:code mhash)
18
=> (:length mhash)
32 ; bytes

; :digest returns a *copy* of the digest bytes:
=> (:digest mhash)
#bin "29MYwcRiruhy9BEJpN/TBIhxoD3t0P4OdXztV9rW8tc="
```

### Serialization

One of the appeals of the multihash standard is that it can be serialized
without specifying an encoding for the resulting byte representation. This
library provides several formats for multihashes:

```clojure
; Multihashes render as URN-like strings:
=> (str mhash)
"hash:sha2-256:dbd318c1c462aee872f41109a4dfd3048871a03dedd0fe0e757ced57dad6f2d7"

; Directly encode a multihash into a byte array:
=> (multihash/encode mhash)
#bin "EiDb0xjBxGKu6HL0EQmk39MEiHGgPe3Q/g51fO1X2tby1w=="

; Read data to decode a multihash:
=> (= mhash (multihash/decode *1))
true

; Full hex encoding is supported:
=> (multihash/hex mhash)
"1220dbd318c1c462aee872f41109a4dfd3048871a03dedd0fe0e757ced57dad6f2d7"
=> (= mhash (multihash/decode *1))
true

; As is base58 (compatible with IPFS):
=> (multihash/base58 mhash)
"Qmd8kgzaFLGYtTS1zfF37qKGgYQd5yKcQMyBeSa8UkUz4W"
=> (= mhash (multihash/decode *1))
true
```

Decoding is implemented as a protocol, so it can be extended to other data
source types as needed.

## Maintainers

Captain: [@greglook](https://github.com/greglook).

## Contribute

Contributions welcome. Please check out [the issues](https://github.com/multiformats/clj-multihash/issues).

Check out our [contributing document](https://github.com/multiformats/multiformats/blob/master/contributing.md)
for more information on how we work, and about contributing in general. Please
be aware that all interactions related to multiformats are subject to the IPFS
[Code of Conduct](https://github.com/ipfs/community/blob/master/code-of-conduct.md).

Small note: If editing the README, please conform to the
[standard-readme](https://github.com/RichardLitt/standard-readme) specification.

## License

This is free and unencumbered software released into the public domain.
See the [UNLICENSE](UNLICENSE) file for more information.
