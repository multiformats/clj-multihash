(ns multihash.core-test
  (:require
    [clojure.test :refer :all]
    [multihash.core :as multihash]))


(deftest app-specific-codes
  (is (false? (multihash/app-code? 0x00)))
  (doseq [code (range 0x01 0x10)]
    (is (true? (multihash/app-code? code))
        (str "Algorithm code " code " is app-specific")))
  (doseq [code (range 0x10 0x100)]
    (is (false? (multihash/app-code? code))
        (str "Algorithm code " code " is not app-specific"))))


(def examples
  "Test case examples."
  {"11140beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33"
   [0x11 :sha1 20 "0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33"]

   "11040beec7b8"
   [0x11 :sha1 4 "0beec7b8"]

   "12202c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae"
   [0x12 :sha2-256 32 "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae"]

   "12042c26b46b"
   [0x12 :sha2-256 4 "2c26b46b"]

   "40042c26b46b"
   [0x40 :blake2b 4 "2c26b46b"]})


(deftest example-decoding
  (doseq [[encoded [code algorithm length digest]] examples]
    (let [mhash (multihash/decode encoded)]
      (is (= code (:code mhash)))
      (is (= algorithm (:algorithm mhash)))
      (is (= length (:length mhash)))
      (is (= digest (:digest mhash))))))


(deftest example-encoding
  (doseq [[encoded [code algorithm length digest]] examples]
    (let [mhash (multihash/create algorithm digest)]
      (is (= encoded (multihash/encode mhash))))))
