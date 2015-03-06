(ns multihash.core
  "Core multihash type definition and helper methods."
  (:require
    [clojure.string :as str])
  (:import
    java.security.MessageDigest))


;; ## Hash Function Algorithms

(def ^:const algorithms
  "Map of information about the available content hashing algorithms."
  {:sha1     {:code 0x11, :length 20}
   :sha2-256 {:code 0x12, :length 32}
   :sha2-512 {:code 0x13, :length 64}
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



;; ## Utility Functions

(defn- zero-pad-str
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


(defn- zero-pad-bytes
  "Pads a byte array with leading zeroes (or trims them) to ensure it has the
  given length. Returns the same array if the length is already correct, or a
  new resized array if not."
  [width ^bytes array]
  (let [array-length (alength array)]
    (if (= width array-length)
      array
      (let [array' (byte-array width)]
        (if (< array-length width)
          (System/arraycopy array 0 array' (- width array-length) array-length)
          (System/arraycopy array (- array-length width) array' 0 width))
        array'))))


(defn- bytes->hex
  "Converts a byte array into a lowercase hexadecimal string."
  [^bytes value]
  (when (and value (pos? (alength value)))
    (let [width (* 2 (alength value))
          hex (-> (BigInteger. 1 value)
                  (.toString 16)
                  str/lower-case)]
      (zero-pad-str width hex))))


(defn- hex->bytes
  "Parses a hexadecimal string into a byte array. Ensures that the resulting
  array is zero-padded to match the hex string length."
  ^bytes
  [^String value]
  (when (and value (not (empty? value)))
    (let [length (/ (count value) 2)
          int-bytes (.toByteArray (BigInteger. value 16))]
      (zero-pad-bytes length int-bytes))))


(defn- validate-digest
  "Checks a string to determine whether it's well-formed hexadecimal digest.
  Returns an error message if the argument is invalid."
  [digest]
  (let [length (count digest)]
    (cond
      (not (string? digest))
      (str "Value is not a string: " (pr-str digest))

      (odd? length)
      (str "String '" digest "' is not a valid digest: "
           "number of characters (" length ") is odd")

      (not (re-matches #"^[0-9a-fA-F]+$" digest))
      (str "String '" digest "' is not a valid digest: "
           "contains illegal characters")

      (< length 2)
      (str "Digest must contain at least one byte")

      (> length 254)
      (str "Digest exceeds maximum supported length of 127: " (/ length 2))

      :else nil)))



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



;; ## Constructors

;; Remove automatic constructor function.
(ns-unmap *ns* '->Multihash)


(defn create
  "Constructs a new Multihash identifier. Accepts either a numeric algorithm
  code or a keyword name as the first argument. The digest may either by a byte
  array or a hex string."
  [algorithm digest]
  (let [code (:code (get-algorithm algorithm))]
    (when-not (integer? code)
      (throw (IllegalArgumentException.
               (str "Argument " (pr-str algorithm) " does not "
                    "represent a valid hash algorithm."))))
    (let [digest (if (string? digest) digest (bytes->hex digest))]
      (when-let [err (validate-digest digest)]
        (throw (IllegalArgumentException. ^String err)))
      (Multihash. code digest nil))))


(defmacro ^:private defhash
  "Defines a new convenience hashing function for the given algorithm and system
  digest name."
  [algorithm digest-name]
  `(defn ~(symbol (name algorithm))
     ~(str "Calculates the " digest-name " digest of the given byte array and "
           "returns a multihash.")
     [~(vary-meta 'content assoc :tag 'bytes)]
     (let [algo# (MessageDigest/getInstance ~digest-name)]
       (create ~algorithm (.digest algo# ~'content)))))


(defhash :sha1     "SHA-1")
(defhash :sha2-256 "SHA-256")
(defhash :sha2-512 "SHA-512")



;; ## Encoding and Decoding

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
  (let [^bytes encoded (if (string? encoded)
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
