(ns multihash.core
  "Core multihash type definition and helper methods."
  (:refer-clojure :exclude [hash])
  (:require
    [clojure.string :as str])
  (:import
    java.security.MessageDigest))


;; ## Hash Function Algorithms

(def ^:const algorithms
  "Map of content hashing algorithms to system names and byte encodings."
  {:sha1     {:code 0x11, :length 20, :system "SHA-1"}
   :sha2-256 {:code 0x12, :length 32, :system "SHA-256"}
   :sha2-512 {:code 0x13, :length 64, :system "SHA-512"}
   :sha3     {:code 0x14, :length 64}
   :blake2b  {:code 0x40, :length 64}
   :blake2s  {:code 0x41, :length 32}})


(defn app-code?
  "True if the given code number is assigned to the application-specfic range."
  [code]
  (< 0 code 0x10))


(defn get-algorithm
  "Looks up an algorithm by keyword name or code number. Returns `nil` if the
  value does not map to any valid algorithm."
  [value]
  (cond
    (keyword? value)
    (when-let [data (get algorithms value)]
      (assoc data :name value))

    (not (integer? value))
    nil

    (app-code? value)
    {:code value, :name (keyword (str "app-" value))}

    :else
    (some #(when (= value (:code (val %)))
             (assoc (val %) :name (key %)))
          algorithms)))



;; ## Utilities

(defn- zero-pad
  "Pads a string with leading zeroes up to the given width."
  [width value]
  (let [string (str value)]
    (if (<= width (count string))
      string
      (-> width
          (- (count string))
          (repeat "0")
          str/join
          (str string)))))


(defn- bytes->hex
  "Converts a byte array into a lowercase hexadecimal string."
  [^bytes value]
  (when (and value (pos? (alength value)))
    (let [width (* 2 (alength value))
          hex (-> (BigInteger. 1 value)
                  (.toString 16)
                  str/lower-case)]
      (zero-pad width hex))))


(defn- hex->bytes
  "Parses a hexadecimal string into a byte array. Ensures that the resulting
  array is zero-padded to match the hex string length."
  ^bytes
  [^String value]
  (when (and value (not (empty? value)))
    (when (odd? (count value))
      (throw (IllegalArgumentException.
               (str "Input string '" value "' is not valid hex: number of "
                    "characters (" (count value) ") is odd"))))
    (when-not (re-matches #"^[0-9a-fA-F]+$" value)
      (throw (IllegalArgumentException.
               (str "Input string '" value "' is not valid hex: "
                    "contains illegal characters"))))
    (let [length (/ (count value) 2)
          int-bytes (.toByteArray (BigInteger. value 16))
          int-length (alength int-bytes)]
      (if (= length int-length)
        int-bytes
        (let [data (byte-array length)]
          (if (< int-length length)
            (System/arraycopy int-bytes 0 data (- length int-length) int-length)
            (System/arraycopy int-bytes (- int-length length) data 0 length))
          data)))))



;; ## Multihash Type

;; Multihash identifiers have two properties:
;;
;; - `:code` is a numeric code for an algorithm entry in `algorithms` or an
;;   application-specific algorithm code.
;; - `:digest` is a string holding the hex-encoded algorithm output.
(deftype Multihash
  [^long code ^String digest _meta]

  Object

  (toString [this]
    (str "hash:" (name (:name (get-algorithm code))) \: digest))

  (equals [this that]
    (cond
      (identical? this that) true
      (instance? Multihash that)
        (and (= code   (.code   ^Multihash that))
             (= digest (.digest ^Multihash that)))
      :else false))

  (hashCode [this]
    (hash-combine code digest))


  Comparable

  (compareTo [this that]
    (cond
      (= this that) 0
      (< code (:code that)) -1
      (> code (:code that)) 1
      :else (compare digest (:digest that))))


  clojure.lang.ILookup

  (valAt [this k not-found]
    (case k
      :code code
      :algorithm (:name (get-algorithm code))
      :length (/ (count digest) 2)
      :digest digest
      :bytes (hex->bytes digest)
      not-found))

  (valAt [this k]
    (.valAt this k nil))


  clojure.lang.IMeta

  (meta [_] _meta)


  clojure.lang.IObj

  (withMeta [_ m]
    (Multihash. code digest m)))


;; Remove automatic constructor function.
(ns-unmap *ns* '->Multihash)


(defn create
  "Constructs a new Multihash identifier. Accepts either a numeric algorithm
  code or a keyword name as the first argument. The digest may either by a byte
  array or a hex string."
  [algorithm digest]
  (let [code (:code (get-algorithm algorithm))
        digest (if (string? digest)
                 (bytes->hex (hex->bytes digest))
                 (bytes->hex digest))
        length (/ (count digest) 2)]
    (when-not (integer? code)
      (throw (IllegalArgumentException.
               (str "Algorithm argument " (pr-str algorithm) " does not "
                    "represent a valid hash algorithm."))))
    (when (< length 1)
      (throw (IllegalArgumentException.
               (str "Digest must contain at least one byte"))))
    (when (> length 127)
      (throw (IllegalArgumentException.
               (str "Digest exceeds maximum supported length of 127: "
                    length))))
    (Multihash. code digest nil)))


(defn encode
  "Encodes a multihash into a binary representation."
  [mhash]
  (let [length (:length mhash)
        encoded (byte-array (+ length 2))]
    (aset encoded 0 (byte (:code mhash)))
    (aset encoded 1 (byte length))
    (System/arraycopy (:bytes mhash) 0 encoded 2 length)
    encoded))


(defn encode-hex
  "Encodes a multihash into a hexadecimal string."
  [mhash]
  (bytes->hex (encode mhash)))


(defn decode
  "Decodes a hex string or byte array into a multihash value."
  [encoded]
  (let [encoded (if (string? encoded)
                  (hex->bytes encoded)
                  encoded)
        code (aget encoded 0)
        length (aget encoded 1)
        payload-length (- (alength encoded) 2)]
    (when (not= length payload-length)
      (throw (IllegalArgumentException.
               (str "Encoded digest length " length " does not match actual "
                    "digest payload of " payload-length " bytes"))))
    (let [digest (byte-array length)]
      (System/arraycopy encoded 2 digest 0 length)
      (create code digest))))


; TODO: (read! InputStream)


; TODO: multimethod?
(defn hash
  "Calculates the digest of the given byte array and returns a `Multihash`."
  [algorithm ^bytes content]
  #_
  (let [hex-digest (-> (algorithms algorithm)
                       MessageDigest/getInstance
                       (.digest content)
                       bytes->hex)]
    (Multihash. algorithm hex-digest nil)))
