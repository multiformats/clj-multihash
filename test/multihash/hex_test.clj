(ns multihash.hex-test
  (:require
    [clojure.test :refer :all]
    [multihash.hex :as hex]
    [multihash.test-utils :refer :all]))


(deftest encoding-test
  (is (= ""   (hex/encode (byte-array 0)))
      "empty byte array encodes as empty string")
  (is (= "00"  (hex/encode (byte-array 1)))
      "single zero byte encodes as two zero chars")
  (is (= "007f" (hex/encode (doto (byte-array 2)
                              (aset-byte 1 127))))))


(deftest decoding-test
  (is (bytes= (byte-array 0) (hex/decode ""))
      "empty string decodes as empty byte array")
  (is (bytes= (byte-array 1) (hex/decode "00"))
      "single zero char decodes as single zero byte"))


(deftest reflexive-encoding
  (dotimes [i 100]
    (let [data (random-bytes 30)]
      (is (bytes= data (hex/decode (hex/encode data)))
          (str "Hex coding is reflexive for "
               (.toString (BigInteger. 1 data) 16))))))
