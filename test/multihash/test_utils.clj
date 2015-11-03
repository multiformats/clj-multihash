(ns multihash.test-utils
  (:import
    java.security.SecureRandom))


(defn bytes=
  "Returns true if two byte sequences are the same length and have the same
  byte content."
  [a b]
  (and (= (count a) (count b))
       (every? true? (map = a b))))


(defn random-bytes
  "Returns a byte array between zero and `max-size` bytes long with random
  content."
  [max-size]
  (let [size (rand-int max-size)
        data (byte-array size)]
    (.nextBytes (SecureRandom.) data)
    data))
