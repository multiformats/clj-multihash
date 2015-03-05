(ns multihash.core-test
  (:require
    [clojure.string :as str]
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


(deftest get-algorithms
  (is (nil? (multihash/get-algorithm 0)))
  (is (nil? (multihash/get-algorithm 0x100)))
  (is (nil? (multihash/get-algorithm :foo/bar)))
  (doseq [code (range 0x01 0x10)]
    (let [algorithm (multihash/get-algorithm code)]
      (is (= code (:code algorithm)))
      (is (keyword? (:name algorithm)))))
  (doseq [[algorithm data] multihash/algorithms]
    (let [by-name (multihash/get-algorithm algorithm)
          by-code (multihash/get-algorithm (:code data))]
      (is (= algorithm (:name by-name)))
      (is (= (:code data) (:code by-code)))
      (is (= by-name by-code)))))


(deftest constructor-validation
  (is (thrown? IllegalArgumentException
               (multihash/create :no-such-algo "0beec7b8"))
      "Unknown algorith keyword should be rejected")
  (is (thrown? IllegalArgumentException
               (multihash/create 0x2F "0beec7b8"))
      "Unknown numeric code should be rejected")
  (is (thrown? IllegalArgumentException
               (multihash/create :sha1 nil))
      "Nil digest should be rejected")
  (is (thrown? IllegalArgumentException
               (multihash/create :sha1 ""))
      "Empty digest should be rejected")
  (is (thrown? IllegalArgumentException
               (multihash/create :sha1 (byte-array 128)))
      "Digest length should be limited to 127")
  (is (thrown? IllegalArgumentException
               (multihash/create :sha1 "012"))
      "Odd digest length should be rejected"))


(deftest value-semantics
  (let [a (multihash/create 0x11 "0beec7b8")
        b (multihash/create 0x11 "94a1be0c")
        c (multihash/create 0x12 "00a8b94e")
        c' (multihash/create 0x12 "00a8b94e")]
    (is (= a a) "Identical values are equal")
    (is (= c c') "Values with same code and digest are equal")
    (is (integer? (hash b)) "Hash code returns an integer")
    (is (= (hash c) (hash c')) "Equivalent objects return same hashcode")
    (is (= [a b c] (sort [c b a])) "Multihashes sort in code/digest order")))


; TODO: toString and print-method
(deftest multihash-rendering
  (is (= "hash:sha1:0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33"
         (str (multihash/create :sha1 "0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33")))))


; TODO: exercise metadata


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

   "40040006b46b"
   [0x40 :blake2b 4 "0006b46b"]})


(deftest example-coding
  (testing "Encoding is reflexive"
    (let [mhash (multihash/create 0x02 "0beec7b8")]
      (is (= mhash (multihash/decode (multihash/encode mhash))))))
  (testing "Encoded multihashes match expected hex"
    (doseq [[encoded [code algorithm length digest]] examples]
      (let [mhash (multihash/create algorithm digest)]
        (is (= encoded (multihash/encode-hex mhash))))))
  (testing "Decoding examples gives expected properties"
    (doseq [[encoded [code algorithm length digest]] examples]
      (let [mhash (multihash/decode encoded)]
        (is (= code (:code mhash)))
        (is (= algorithm (:algorithm mhash)))
        (is (= length (:length mhash)))
        (is (= digest (:digest mhash)))))))
