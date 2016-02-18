(ns multihash.hex-test
  (:require
    [clojure.test :refer :all]
    [multihash.hex :as hex]
    [multihash.test-utils :refer :all]))


(deftest encoding-test
  (is (nil? (hex/encode (byte-array 0)))
      "empty byte array encodes as nil")
  (is (= "00" (hex/encode (byte-array 1)))
      "single zero byte encodes as two zero chars")
  (is (= "007f" (hex/encode (doto (byte-array 2)
                              (aset-byte 1 127))))))


(deftest decoding-test
  (is (bytes= (byte-array 0) (hex/decode ""))
      "empty string decodes as empty byte array")
  (is (bytes= (byte-array 1) (hex/decode "00"))
      "single zero char decodes as single zero byte"))


(deftest reflexive-encoding
  (dotimes [i 10]
    (let [data (random-bytes 30)
          encoded (hex/encode data)
          decoded (hex/decode encoded)]
      (is (bytes= data decoded)
          (str "Hex coding is reflexive for "
               (.toString (BigInteger. 1 data) 16))))))
