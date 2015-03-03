(ns multihash.core
  "Core multihash type definition and helper methods."
  (:refer-clojure :exclude [hash])
  (:require
    [clojure.string :as str])
  (:import
    java.security.MessageDigest))


(def ^:const digest-algorithms
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


(defn- lookup-algo
  "Look up algorithm details by code number."
  [code]
  (some #(when (= code (:code (val %)))
           (assoc (val %) :name (key %)))
        digest-algorithms))


(defn- code->name
  "Return a keyword corresponding to the given algorithm code."
  [code]
  (if (app-code? code)
    (keyword (str "app-" code))
    (:name (lookup-algo code))))



;; ## Multihash Type

;; Multihash identifiers have an `:algorithm` keyword and a `:digest` string giving
;; the hexadecimal output on some byte content.
(deftype Multihash
  [^long code ^String digest _meta]

  Object

  (toString [this]
    (str "hash:" (code->name code) \: digest))

  (equals [this that]
    (cond
      (identical? this that) true
      (instance? Multihash that)
        (and (= code (:code that))
             (= digest (:digest that)))
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
      :algorithm (code->name code)
      :length (/ (count digest) 2)
      :digest digest
      not-found))

  (valAt [this k]
    (.valAt this k nil))


  clojure.lang.IMeta

  (meta [_] _meta)


  clojure.lang.IObj

  (withMeta [_ m]
    (Multihash. code digest m)))


; TODO: constructor
; TODO: encode
; TODO: decode

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


(defn hex-str
  "Converts a byte array into a lowercase hexadecimal string."
  [^bytes value]
  (let [width (* 2 (count value))
        hex (-> (BigInteger. 1 value)
                (.toString 16)
                str/lower-case)]
    (zero-pad width hex)))


(defn hash
  "Calculates the digest of the given byte array and returns a `Multihash`."
  [algorithm ^bytes content]
  #_
  (let [hex-digest (-> (digest-algorithms algorithm)
                       MessageDigest/getInstance
                       (.digest content)
                       hex-str)]
    (Multihash. algorithm hex-digest nil)))
