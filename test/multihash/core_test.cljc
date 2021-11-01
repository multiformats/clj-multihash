(ns multihash.core-test
  (:require
    #?(:clj [clojure.test :refer :all]
       :cljs [cljs.test :refer-macros [deftest is testing]])
    [alphabase.bytes :as bytes]
    [clojure.string :as str]
    [multihash.core :as multihash])
  #?(:clj (:import
            clojure.lang.ExceptionInfo
            java.io.ByteArrayInputStream
            java.nio.ByteBuffer)))


(deftest app-specific-codes
  (is (nil? (multihash/app-code? 17.3)))
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
  (doseq [[algorithm code] multihash/algorithm-codes]
    (let [by-name (multihash/get-algorithm algorithm)
          by-code (multihash/get-algorithm code)]
      (is (= algorithm (:name by-name)))
      (is (= code (:code by-code)))
      (is (= by-name by-code)))))


(deftest constructor-validation
  (is (thrown? ExceptionInfo
               (multihash/create :no-such-algo "0beec7b8"))
      "Unknown algorith keyword should be rejected")
  (is (thrown? ExceptionInfo
               (multihash/create 0x2F "0beec7b8"))
      "Unknown numeric code should be rejected")
  (is (thrown? ExceptionInfo
               (multihash/create :sha1 nil))
      "Nil digest should be rejected")
  (is (thrown? ExceptionInfo
               (multihash/create :sha1 ""))
      "Empty digest should be rejected")
  (is (thrown? ExceptionInfo
               (multihash/create :sha1 "018zk80q"))
      "Malformed digest should be rejected")
  (is (thrown? ExceptionInfo
               (multihash/create :sha1 (bytes/byte-array 128)))
      "Digest length should be limited to 127")
  (is (thrown? ExceptionInfo
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
    #_(is (= [a b c] (sort [c b a])) "Multihashes sort in code/digest order")))


(deftest multihash-rendering
  (is (= "hash:sha1:0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33"
         (str (multihash/create :sha1 "0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33"))))
  (is (= "hash:sha2-256:2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae"
         (str (multihash/create :sha2-256 "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae"))))
  (is (= "hash:sha1:ea347f3c5b8f0fd07b5bc95d0beecdda3c275da3"
         (str (multihash/create :sha1 "ea347f3c5b8f0fd07b5bc95d0beecdda3c275da3")))))


(deftest exercise-metadata
  (let [a (multihash/create :sha1 "dbc95275da8a3d0d0beeea3f0fd47f3cc7b55bc3")
        a' (vary-meta a assoc :foo :bar/baz)]
    (is (empty? (dissoc (meta a) :type :sci.impl/record)) "values start with empty metadata")
    (is (= :bar/baz (:foo (meta a'))) "metadata can be associated with value")
    (is (= a a') "metadata does not affect equality")))


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


(deftest array-decoding-failures
  (is (thrown? ExceptionInfo
               (multihash/decode-array (bytes/byte-array 2)))
      "byte array must have at least three bytes")
  (is (thrown? ExceptionInfo
               (multihash/decode-array
                 (bytes/init-bytes [0x11 0x00 0x00 0x00])))
      "Encoded length must be positive")
  (is (thrown? ExceptionInfo
               (multihash/decode-array
                 (bytes/init-bytes [0x11 0x08 0x00 0x00])))
      "Encoded length must be within byte content"))


#?(:clj
    (defn stream-fixture
      "Constructs a stream with certain leading bytes for testing."
      ([code length]
       (stream-fixture code length length))
      ([code length actual]
       (let [buffer (bytes/byte-array actual)]
         (bytes/set-byte buffer 0 code)
         (bytes/set-byte buffer 1 length)
         (ByteArrayInputStream. buffer)))))


#?(:clj
    (deftest stream-decoding-failures
      (is (thrown? ExceptionInfo
                   (multihash/decode (stream-fixture 0x11 0 4)))
          "Stream with non-positive length is illegal.")
      (is (thrown? Exception
                   (multihash/decode (stream-fixture 0x11 20 5)))
          "Stream without enough data throws exception..")))


(deftest example-coding
  (testing "Encoding is reflexive"
    (let [mhash (multihash/create 0x02 "0beec7b8")]
      (is (= mhash (multihash/decode (multihash/encode mhash))))))
 (doseq [[hex [code algorithm length digest]] examples]
    (let [mhash (multihash/create algorithm digest)]
      (is (= code (:code mhash)))
      (is (= algorithm (multihash/algorithm mhash)))
      (is (= length (multihash/length mhash)))
      (is (= digest (:hex-digest mhash)))
      (is (= hex (multihash/hex mhash))
          "Encoded multihashes match expected hex")
      (is (= mhash (multihash/decode hex))
          "Hex decodes into expected multihash")
      (let [b58 (multihash/base58 mhash)]
        (is (string? b58) "Multihash encodes to a base-58 string")
        (is (= mhash (multihash/decode b58))
            "Multihash round-trips through Base58 encoding"))
      #?(:clj
          (let [stream (ByteArrayInputStream. (multihash/encode mhash))]
            (is (= mhash (multihash/decode stream))
                "Multihash round-trips through InputStream"))))))
