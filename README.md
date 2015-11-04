clj-multihash
=============

[![Join the chat at https://gitter.im/greglook/clj-multihash](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/greglook/clj-multihash?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/greglook/clj-multihash.svg?branch=develop)](https://travis-ci.org/greglook/clj-multihash)
[![Coverage Status](https://coveralls.io/repos/greglook/clj-multihash/badge.svg?branch=develop)](https://coveralls.io/r/greglook/clj-multihash?branch=develop)
[![Dependency Status](https://www.versioneye.com/clojure/mvxcvi:multihash/badge.svg)](https://www.versioneye.com/clojure/mvxcvi:multihash)
[![API codox](http://b.repl.ca/v1/doc-API-blue.png)](https://greglook.github.io/clj-multihash/api/multihash.core.html)
[![marginalia docs](http://b.repl.ca/v1/doc-marginalia-blue.png)](https://greglook.github.io/clj-multihash/marginalia/uberdoc.html)

A Clojure library implementing the
[multihash](https://github.com/jbenet/multihash) standard. This provides an
extensible, efficient binary representation for cryptographic hashes.

## Installation

Library releases are published on Clojars. To use the latest version with
Leiningen, add the following dependency to your project definition:

[![Clojars Project](http://clojars.org/mvxcvi/multihash/latest-version.svg)](http://clojars.org/mvxcvi/multihash)

## Usage

The main focus of this library is the `Multihash` type, which serves as an
immutable representation of the output digest for a specific hash algorithm.
Multihashes are sortable, support metadata, and implement value-based equality
and hash-code generation.

```clojure
(require '[multihash.core :as multihash])

; List the known hash functions:
=> (keys multihash/algorithm-codes)
(:sha1 :sha2-256 :sha2-512 :sha3 :blake2b :blake2s)

; Manually create a multihash value:
=> (multihash/create :sha1 "0f1e2d3c4b5a6978")
#<Multihash hash:sha1:0f1e2d3c4b5a6978>

; Or use one of the constructors for built-in Java implementations:
=> (keys multihash/functions)
(:sha1 :sha2-256 :sha2-512)

=> (def mhash (multihash/sha2-256 "foo bar baz"))
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

## License

This is free and unencumbered software released into the public domain.
See the UNLICENSE file for more information.
