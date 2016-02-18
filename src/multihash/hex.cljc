(ns multihash.hex
  "Functions to encode and decode bytes as hexadecimal."
  (:require
    [clojure.string :as str]))


(defn- byte->hex
  "Converts a single byte value to a two-character hex string."
  [value]
  (let [hex #?(:clj (Integer/toHexString (if (neg? value) (+ 256 value) value))
               :cljs (.toString value 16))]
    (if (= 1 (count hex))
      (str "0" hex)
      hex)))


(defn- hex->byte
  "Converts a two-character hex string into a byte value."
  [hex]
  #?(:clj (let [value (Integer/parseInt hex 16)]
            (if (< 127 value) (- value 256) value))
     :cljs (js/parseInt hex 16)))


(defn encode
  "Converts a byte array into a lowercase hexadecimal string. Returns nil for
  empty inputs."
  ^String
  [^bytes data]
  (when (and data (pos? (alength data)))
    (->> (range (alength data))
         (map #(byte->hex (aget data %)))
         (str/join))))


(defn decode
  "Parses a hexadecimal string into a byte array. Ensures that the resulting
  array is zero-padded to match the hex string length."
  ^bytes
  [^String data]
  (when-not (empty? data)
    (let [length (/ (count data) 2)
          data #?(:clj (byte-array length)
                  :cljs (js/Uint8Array. (js/ArrayBuffer. length)))]
      (dotimes [i length]
        (let [hex (subs data (* 2 i) (* 2 (inc i)))
              b (hex->byte hex)]
          (aset data i (byte b))))
      data)))


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
