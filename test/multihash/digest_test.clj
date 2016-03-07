(ns multihash.digest-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer :all]
    [multihash.core :as multihash]
    [multihash.digest :as digest])
  (:import
    java.io.ByteArrayInputStream
    java.nio.ByteBuffer))


(deftest hashing-constructors
  (doseq [algorithm (keys digest/functions)]
    (testing (str (name algorithm) " hashing")
      (let [hash-fn (digest/functions algorithm)
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
        (is (= (:hex-digest mh1)
               (:hex-digest mh2)
               (:hex-digest mh3)
               (:hex-digest mh4))
            "Constructed multihash digests match")
        (is (thrown? RuntimeException
                     (hash-fn 123)))))))


(deftest content-validation
  (let [content "baz bar foo"
        mhash (digest/sha1 content)]
    (is (nil? (digest/test nil nil)))
    (is (nil? (digest/test nil content)))
    (is (nil? (digest/test mhash nil)))
    (is (true? (digest/test mhash content))
        "Correct multihash returns true")
    (is (false? (digest/test
                  (multihash/create :sha1 "68a9f54521a5501230e9dc73")
                  content))
        "Incorrect multihash returns false")
    (is (thrown-with-msg? RuntimeException #"^No supported hashing function"
          (digest/test
            (multihash/create :blake2b "68a9f54521a5501230e9dc73")
            content))
        "Unsupported hash function cannot be validated")))
