(ns multihash.base58-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer :all]
    [multihash.base58 :as b58])
  (:import
    java.security.SecureRandom))


(defn bytes=
  [a b]
  (and (= (count a) (count b))
       (every? true? (map = a b))))


(deftest encoding-test
  (is (= ""   (b58/encode (byte-array 0)))
      "empty byte array encodes as empty string")
  (is (= "1"  (b58/encode (byte-array 1)))
      "single zero byte encodes as a zero char")
  (is (= "11" (b58/encode (byte-array 2)))
      "two zero bytes encode as two zero chars"))


(deftest decoding-test
  (is (bytes= (byte-array 0) (b58/decode ""))
      "empty string decodes as empty byte array")
  (is (bytes= (byte-array 1) (b58/decode "1"))
      "single zero char decodes as single zero byte")
  (is (bytes= (byte-array 2) (b58/decode "11"))
      "two zero chars decode as two zero bytes"))


(deftest reflexive-encoding
  (dotimes [i 100]
    (let [size (rand-int 30)
          data (byte-array size)]
      (.nextBytes (SecureRandom.) data)
      (is (bytes= data (b58/decode (b58/encode data)))
          (str "Base58 coding is reflexive for "
               (.toString (BigInteger. 1 data) 16))))))
