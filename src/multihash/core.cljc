(ns multihash.core
  "Core multihash type definition and helper methods."
  (:refer-clojure :exclude [test])
  (:require
    [alphabase.base58 :as b58]
    [alphabase.bytes :as bytes]
    [alphabase.hex :as hex]
    [clojure.string :as str])
  #?(:clj (:import
            java.io.InputStream
            java.nio.ByteBuffer
            java.security.MessageDigest)))


;; ## Hash Function Algorithms

(def ^:const algorithm-codes
  "Map of information about the available content hashing algorithms."
  {:sha1     0x11
   :sha2-256 0x12
   :sha2-512 0x13
   :sha3     0x14
   :blake2b  0x40
   :blake2s  0x41})


(defn app-code?
  "True if the given code number is assigned to the application-specfic range.
  Returns nil if the argument is not an integer."
  [code]
  (when (integer? code)
    (< 0 code 0x10)))


(defn get-algorithm
  "Looks up an algorithm by keyword name or code number. Returns `nil` if the
  value does not map to any valid algorithm."
  [value]
  (cond
    (keyword? value)
      (when-let [code (get algorithm-codes value)]
        {:code code, :name value})

    (not (integer? value))
      nil

    (app-code? value)
      {:code value, :name (keyword (str "app-" value))}

    :else
      (some #(when (= value (val %))
               {:code value, :name (key %)})
            algorithm-codes)))



;; ## Multihash Type

;; Multihash identifiers have two properties:
;;
;; - `_code` is a numeric code for an algorithm entry in `algorithm-codes` or an
;;   application-specific algorithm code.
;; - `_digest` is a string holding the hex-encoded algorithm output.
;;
;; Digest values also support metadata.
#?(:clj
    (deftype Multihash
      [^long _code ^String _digest _meta]

      Object

      (toString
        [this]
        (str "hash:" (name (:name (get-algorithm _code))) \: _digest))

      (equals
        [this that]
        (cond
          (identical? this that) true
          (instance? Multihash that)
            (and (= _code   (._code   ^Multihash that))
                 (= _digest (._digest ^Multihash that)))
          :else false))

      (hashCode
        [this]
        (hash-combine _code _digest))


      Comparable

      (compareTo
        [this that]
        (cond
          (= this that) 0
          (< _code (._code ^Multihash that)) -1
          (> _code (._code ^Multihash that)) 1
          :else (compare _digest (._digest ^Multihash that))))


      clojure.lang.ILookup

      (valAt
        [this k]
        (.valAt this k nil))

      (valAt
        [this k not-found]
        (case k
          :code _code
          :algorithm (:name (get-algorithm _code))
          :length (/ (count _digest) 2)
          :digest (hex/decode _digest)
          :hex-digest _digest
          not-found))


      clojure.lang.IObj

      (meta [_] _meta)

      (withMeta
        [_ meta-map]
        (Multihash. _code _digest meta-map)))


   :cljs
    (deftype Multihash
      [_code _digest _meta]

      ; Ref: https://github.com/clojure/clojurescript/blob/master/src/main/cljs/cljs/core.cljs#L430

      Object

      (toString
        [this]
        (str "hash:" (name (:name (get-algorithm _code))) \: _digest))


      IEquiv

      (-equiv
        [this that]
        (cond
          (identical? this that) true
          (instance? Multihash that)
            (and (= _code   (:code that))
                 (= _digest (:hex-digest that)))
          :else false))


      IHash

      (-hash
        [this]
        (hash-combine _code _digest))


      IComparable

      (-compare
        [this that]
        (cond
          (= this that) 0
          (< _code (:code that)) -1
          (> _code (:code that)) 1
          :else (compare _digest (:hex-digest that))))


      ILookup

      (-lookup
        [this k]
        (-lookup this k nil))

      (-lookup
        [this k not-found]
        (case k
          :code _code
          :algorithm (:name (get-algorithm _code))
          :length (/ (count _digest) 2)
          :digest (hex/decode _digest)
          :hex-digest _digest
          not-found))


      IMeta

      (-meta [_] _meta)


      IWithMeta

      (-with-meta
        [_ meta-map]
        (Multihash. _code _digest meta-map))))


;; Remove automatic constructor function.
(ns-unmap *ns* '->Multihash)


(defn create
  "Constructs a new Multihash identifier. Accepts either a numeric algorithm
  code or a keyword name as the first argument. The digest may either by a byte
  array or a hex string."
  [algorithm digest]
  (let [algo (get-algorithm algorithm)]
    (when-not (integer? (:code algo))
      (throw (ex-info
               (str "Argument " (pr-str algorithm) " does not "
                    "represent a valid hash algorithm.")
               {:algorithm algorithm})))
    (let [digest (if (string? digest) digest (hex/encode digest))]
      (when-let [err (hex/validate digest)]
        (throw (ex-info err {:digest digest})))
      (Multihash. (:code algo) digest nil))))



;; ## Encoding and Decoding

(defn encode
  "Encodes a multihash into a binary representation."
  ^bytes
  [mhash]
  (let [length (:length mhash)
        encoded (byte-array (+ length 2))]
    (aset-byte encoded 0 (byte (:code mhash)))
    (aset-byte encoded 1 (byte length))
    (System/arraycopy (:digest mhash) 0 encoded 2 length)
    encoded))


(defn hex
  "Encodes a multihash into a hexadecimal string."
  ^String
  [mhash]
  (when mhash
    (hex/encode (encode mhash))))


(defn base58
  "Encodes a multihash into a Base-58 string."
  ^String
  [mhash]
  (when mhash
    (b58/encode (encode mhash))))


(defn decode-array
  "Decodes a byte array directly into multihash. Throws `ex-info` with a `:type`
  of `:multihash/bad-input` if the data is malformed or invalid."
  [^bytes encoded]
  (let [encoded-size (alength encoded)
        min-size 3]
    (when (< encoded-size min-size)
      (throw (ex-info
               (str "Cannot read multihash from byte array: " encoded-size
                    " is less than the minimum of " min-size)
               {:type :multihash/bad-input}))))
  (let [code (aget encoded 0)
        length (aget encoded 1)
        payload (- (alength encoded) 2)]
    (when-not (pos? length)
      (throw (ex-info
               (str "Encoded length " length " is invalid")
               {:type :multihash/bad-input})))
    (when (< payload length)
      (throw (ex-info
               (str "Encoded digest length " length " exceeds actual "
                    "remaining payload of " payload " bytes")
               {:type :multihash/bad-input})))
    (let [digest (byte-array length)]
      (System/arraycopy encoded 2 digest 0 length)
      (create code digest))))


(defn- read-stream-digest
  "Reads a byte digest array from an input stream. First reads a byte giving
  the length of the digest data to read. Throws an ex-info if the length is
  invalid or there is an error reading from the stream."
  ^bytes
  [^InputStream input]
  (let [length (.read input)]
    (when-not (pos? length)
      (throw (ex-info
               (format "Byte %02x is not a valid digest length." length)
               {:type :multihash/bad-input})))
    (let [digest (byte-array length)]
      (loop [offset 0
             remaining length]
        (let [n (.read input digest offset remaining)]
          (if (< n remaining)
            (recur (+ offset n) (- remaining n))
            digest))))))


(defprotocol Decodable
  "This protocol provides a method for data sources which a multihash can be
  read from."

  (decode
    [source]
    "Attempts to read a multihash value from the data source."))


(extend-protocol Decodable

  (class (byte-array 0))

  (decode
    [source]
    (decode-array source))


  String

  (decode
    [source]
    (decode-array
      (if (hex/valid? source)
        (hex/decode source)
        (b58/decode source))))


  InputStream

  (decode
    [source]
    (let [code (.read source)
          digest (read-stream-digest source)]
      (create code digest))))
