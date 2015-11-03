(ns multihash.hex
  "Functions to encode and decode bytes as hexadecimal."
  (:require
    [clojure.string :as str]))


(defn- pad-str
  "Pads a string with leading zeroes up to the given width."
  [width value]
  (let [string (str value)]
    (if (<= width (count string))
      string
      (-> width
          (- (count string))
          (repeat "0")
          (str/join)
          (str string)))))


(defn- pad-bytes
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


(defn encode
  "Converts a byte array into a lowercase hexadecimal string."
  ^String
  [^bytes value]
  (when value
    (if (pos? (alength value))
      (let [width (* 2 (alength value))
            hex (-> (BigInteger. 1 value)
                    (.toString 16)
                    (str/lower-case))]
        (pad-str width hex))
      "")))


(defn decode
  "Parses a hexadecimal string into a byte array. Ensures that the resulting
  array is zero-padded to match the hex string length."
  ^bytes
  [^String value]
  (when value
    (if (empty? value)
      (byte-array 0)
      (let [length (/ (count value) 2)
            int-bytes (.toByteArray (BigInteger. value 16))]
        (pad-bytes length int-bytes)))))


(defn validate
  "Checks a string to determine whether it's well-formed hexadecimal digest.
  Returns an error message if the argument is invalid."
  ^String
  [digest]
  (cond
    (not (string? digest))
      (str "Value is not a string: " (pr-str digest))

    (not (re-matches #"^[0-9a-fA-F]*$" digest))
      (str "String '" digest "' is not a valid digest: "
           "contains illegal characters")

    (< (count digest) 2)
      (str "Digest must contain at least one byte")

    (> (count digest) 254)
      (str "Digest exceeds maximum supported length of 127: " (/ (count digest) 2))

    (odd? (count digest))
      (str "String '" digest "' is not a valid digest: "
           "number of characters (" (count digest) ") is odd")

    :else nil))


(defn valid?
  "Returns true if the string is valid hexadecimal."
  [digest]
  (nil? (validate digest)))
