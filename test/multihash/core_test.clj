(ns multihash.core-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer :all]
    [multihash.core :as multihash])
  (:import
    (java.io
      ByteArrayInputStream
      IOException)
    java.nio.ByteBuffer))


(deftest app-specific-codes
  (is (nil? (multihash/app-code? 17.0)))
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
               (multihash/create :sha1 "018zk80q"))
      "Malformed digest should be rejected")
  (is (thrown? IllegalArgumentException
               (multihash/create :sha1 (byte-array 128)))
      "Digest length should be limited to 127")
  (is (thrown? IllegalArgumentException
               (multihash/create :sha1 "012"))
      "Odd digest length should be rejected"))


(deftest hashing-constructors
  (doseq [algorithm (keys multihash/functions)]
    (testing (str (name algorithm) " hashing")
      (let [hash-fn (multihash/functions algorithm)
            content "foo bar baz"
            mh1 (hash-fn content)
            mh2 (hash-fn (.getBytes content))
            mh3 (hash-fn (ByteBuffer/wrap (.getBytes content)))
            mh4 (hash-fn (ByteArrayInputStream. (.getBytes content)))]
        (is (= algorithm
               (:algorithm mh1)
               (:algorithm mh2)
               (:algorithm mh3)
               (:algorithm mh4))
            "Constructed multihash algorithms match")
        (is (= (._digest mh1)
               (._digest mh2)
               (._digest mh3)
               (._digest mh4))
            "Constructed multihash digests match")
        (is (thrown? RuntimeException
                     (hash-fn 123)))))))


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


(deftest multihash-rendering
  (is (= "hash:sha1:0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33"
         (str (multihash/create :sha1 "0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33"))))
  (is (= "hash:sha2-256:2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae"
         (str (multihash/create :sha2-256 "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae"))))
  (is (= "hash:sha1:ea347f3c5b8f0fd07b5bc95d0beecdda3c275da3"
         (pr-str (multihash/create :sha1 "ea347f3c5b8f0fd07b5bc95d0beecdda3c275da3")))))


(deftest exercise-metadata
  (let [a (multihash/create :sha1 "dbc95275da8a3d0d0beeea3f0fd47f3cc7b55bc3")
        a' (vary-meta a assoc :foo :bar/baz)]
    (is (empty? (meta a)) "values start with empty metadata")
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
  (is (thrown? IOException
               (multihash/decode-array (byte-array 2)))
      "byte array must have at least three bytes")
  (is (thrown? IOException
               (multihash/decode-array
                 (doto (byte-array 4)
                   (aset-byte 0 0x11)
                   (aset-byte 1 0))))
      "Encoded length must be positive")
  (is (thrown? IOException
               (multihash/decode-array
                 (doto (byte-array 4)
                   (aset-byte 0 0x11)
                   (aset-byte 1 8))))
      "Encoded length must be within byte content"))


(defn stream-fixture
  "Constructs a stream with certain leading bytes for testing."
  ([code length]
   (stream-fixture code length length))
  ([code length actual]
   (let [buffer (byte-array actual)]
     (aset-byte buffer 0 code)
     (aset-byte buffer 1 length)
     (ByteArrayInputStream. buffer))))


(deftest stream-decoding-failures
  (is (thrown? IOException
               (multihash/decode (stream-fixture 0x11 0 4)))
      "Stream with non-positive length is illegal.")
  (is (thrown? Exception
               (multihash/decode (stream-fixture 0x11 20 5)))
      "Stream without enough data throws exception.."))


(deftest example-coding
  (testing "Encoding is reflexive"
    (let [mhash (multihash/create 0x02 "0beec7b8")]
      (is (= mhash (multihash/decode (multihash/encode mhash))))))
  (doseq [[hex [code algorithm length digest]] examples]
    (let [mhash (multihash/create algorithm digest)]
      (is (= code (:code mhash)))
      (is (= algorithm (:algorithm mhash)))
      (is (= length (:length mhash)))
      (is (= digest (._digest mhash)))
      (is (= hex (multihash/hex mhash))
          "Encoded multihashes match expected hex")
      (is (= mhash (multihash/decode hex))
          "Hex decodes into expected multihash")
      (let [b58 (multihash/base58 mhash)]
        (is (string? b58) "Multihash encodes to a base-58 string")
        (is (= mhash (multihash/decode b58))
            "Multihash round-trips through Base58 encoding"))
      (let [stream (ByteArrayInputStream. (multihash/encode mhash))]
        (is (= mhash (multihash/decode stream))
            "Multihash round-trips through InputStream")))))


(deftest content-validation
  (let [content "baz bar foo"
        mhash (multihash/sha1 content)]
    (is (nil? (multihash/test nil nil)))
    (is (nil? (multihash/test nil content)))
    (is (nil? (multihash/test mhash nil)))
    (is (true? (multihash/test mhash content))
        "Correct multihash returns true")
    (is (false? (multihash/test
                  (multihash/create :sha1 "68a9f54521a5501230e9dc73")
                  content))
        "Incorrect multihash returns false")
    (is (thrown-with-msg? RuntimeException #"^No supported hashing function"
          (multihash/test
            (multihash/create :blake2b "68a9f54521a5501230e9dc73")
            content))
        "Unsupported hash function cannot be validated")))


(deftest multihash-selection
  (let [a (multihash/create :sha1 "37b51d194a7513e45b56f6524f2d51f200000000")
        b (multihash/create :sha1 "73fcffa4b7f6bb68e44cf984c85f6e888843d7f9")
        c (multihash/create :sha1 "73fe285cedef654fccc4a4d818db4cc225932878")
        d (multihash/create :sha1 "acbd18db4cc2f856211de9ecedef654fccc4a4d8")
        e (multihash/create :sha1 "c3c23db5285662ef717963ff4ce2373df0003206")
        f (multihash/create :sha2-256 "285c3c23d662b5ef7172373df0963ff4ce003206")
        hashes [a b c d e f]]
    (are [brs opts] (= brs (multihash/select opts hashes))
         hashes   {}
         [c d e f]  {:after "111473fd2"}
         [b c]      {:prefix "111473"}
         [f]        {:algorithm :sha2-256})))
