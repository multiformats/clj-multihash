(ns multihash.base58
  "Native Base-58 encoding implementation.")


(def alphabet "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz")
(def base 58N)


(defn- bytes->chars
  "Encode a byte array into a sequence of alphabet characters."
  [^bytes data]
  (loop [n (bigint (BigInteger. 1 data))
         s (list)]
    (if (< n base)
      (conj s (get alphabet n))
      (let [r (mod n base)]
        (recur
          (/ (- n r) base)
          (conj s (get alphabet r)))))))


(defn- chars->bytes
  "Decode a sequence of characters into a byte array."
  ^bytes
  [^String data]
  (->>
    (reverse data)
    (map vector (iterate (partial * base) 1))
    (reduce
      (fn [n [b c]]
        (let [v (.indexOf alphabet (str c))]
          (when (neg? v)
            (throw (IllegalArgumentException.
                     (str "Invalid character: '" c "' is not in Base58 alphabet"))))
          (+ n (* (bigint v) b))))
      0N)
    (.toBigInteger)
    (.toByteArray)))


(defn encode
  "Converts a byte array into a Base-58 string."
  ^String
  [^bytes data]
  (apply str
    (let [zeroes (count (take-while zero? data))]
      (concat
        (repeat zeroes (first alphabet))
        (if (< zeroes (count data))
          (bytes->chars data))))))


(defn decode
  "Decodes a Base-58 string into a byte array."
  ^bytes
  [^String data]
  (let [zeroes (count (take-while #{(first alphabet)} data))]
    (if (= zeroes (count data))
      (byte-array zeroes)
      (let [byte-data (chars->bytes data)
            strip-sign? (and (> (count byte-data) 1)
                          (zero? (aget byte-data 0))
                          (neg? (aget byte-data 1)))
            result (byte-array (cond-> (+ zeroes (count byte-data))
                                 strip-sign? (dec)))]
        (System/arraycopy
          byte-data
          (if strip-sign? 1 0)
          result
          zeroes
          (- (count result) zeroes))
        result))))
