clj-multihash
=============

[![Build Status](https://travis-ci.org/greglook/clj-multihash.svg?branch=develop)](https://travis-ci.org/greglook/clj-multihash)
[![Coverage Status](https://coveralls.io/repos/greglook/clj-multihash/badge.svg?branch=develop)](https://coveralls.io/r/greglook/clj-multihash?branch=develop)
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
=> (require '[multihash.core :as multihash])

; List the supported hash functions:
=> (keys multihash/algorithms)
(:sha2-256 :sha2-512 :blake2b :sha3 :sha1 :blake2s)

; Manually create a multihash value:
=> (multihash/create :sha1 "0f1e2d3c4b5a6978")
#<Multihash hash:sha1:0f1e2d3c4b5a6978>

; Or use one of the hash constructors for built-in Java algorithms:
=> (def mhash (multihash/sha2-256 (.getBytes "foo bar baz")))
#'user/mhash

; Multihashes render as URN-like strings:
=> (str mhash)
"hash:sha2-256:dbd318c1c462aee872f41109a4dfd3048871a03dedd0fe0e757ced57dad6f2d7"

; Properties can be accessed using keyword lookups:
=> ((juxt :code :algorithm :length :digest) mhash)
[18 :sha2-256 32 "dbd318c1c462aee872f41109a4dfd3048871a03dedd0fe0e757ced57dad6f2d7"]

; The raw digest bytes are also available if needed:
=> (:bytes mhash)
#bin "29MYwcRiruhy9BEJpN/TBIhxoD3t0P4OdXztV9rW8tc="
```

There are also functions to handle encoding and decoding multihashes for binary
serialization:

```clojure
; Directly encode a multihash into a byte array:
=> (multihash/encode mhash)
#bin "EiDb0xjBxGKu6HL0EQmk39MEiHGgPe3Q/g51fO1X2tby1w=="

; Read data to decode a multihash:
=> (= mhash (multihash/decode *1))
true

; Decode also works on hex strings:
=> (multihash/encode-hex mhash)
"1220dbd318c1c462aee872f41109a4dfd3048871a03dedd0fe0e757ced57dad6f2d7"
=> (= mhash (multihash/decode *1))
true
```

Decoding is implemented as the protocol `multihash.core/Decodable`, so it can be extended to
other data source types like `java.io.InputStream`.

## License

This is free and unencumbered software released into the public domain.
See the UNLICENSE file for more information.
