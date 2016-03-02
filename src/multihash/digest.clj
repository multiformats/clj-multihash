(ns multihash.digest
  "Digest functions for creating new multihash constructors."
  (:require
    [multihash.core :as multihash])
  (:import
    (java.io
      InputStream
      IOException)
    java.nio.ByteBuffer
    java.security.MessageDigest))


(def functions
  "Map of supported multihash algorithm keys to hashing functions. Each function
  should take a source of binary data as the argument and return a multihash."
  {})


(defn- digest-content
  "Constructs a cryptographic digest for a given algorithm and content. Content
  may be in the form of a raw byte array, a `ByteBuffer`, or a string. Returns
  a byte array with the digest."
  ^bytes
  [digest-name content]
  (let [algo (MessageDigest/getInstance digest-name)]
    (condp instance? content
      String
        (.update algo (.getBytes ^String content))
      (Class/forName "[B")
        (.update algo ^bytes content)
      ByteBuffer
        (.update algo ^ByteBuffer content)
      InputStream
        (let [buffer (byte-array 1024)]
          (loop []
            (let [n (.read ^InputStream content buffer 0 (count buffer))]
              (when (pos? n)
                (.update algo buffer 0 n)
                (recur)))))
      (throw (IllegalArgumentException.
               (str "Don't know how to compute digest from "
                    (class content)))))
    (.digest algo)))


(defmacro defhash
  "Defines a new convenience hashing function for the given algorithm and system
  digest name."
  [algorithm digest-name]
  (let [fn-sym (symbol (name algorithm))]
    `(do
       (defn ~fn-sym
         ~(str "Calculates the " digest-name " digest of the given byte array or "
               "buffer and returns a multihash.")
         [~'content]
         (multihash/create ~algorithm (digest-content ~digest-name ~'content)))
       (alter-var-root #'functions assoc ~algorithm ~fn-sym))))


(defhash :sha1     "SHA-1")
(defhash :sha2-256 "SHA-256")
(defhash :sha2-512 "SHA-512")


(defn test
  "Determines whether a multihash is a correct identifier for some content by
  recomputing the digest for the algorithm specified in the multihash. Returns
  nil if either argument is nil, true if the digest matches, or false if not.
  Throws an exception if the multihash specifies an unsupported algorithm."
  [mhash content]
  (when (and mhash content)
    (if-let [hash-fn (get functions (:algorithm mhash))]
      (= mhash (hash-fn content))
      (throw (ex-info
               (format "No supported hashing function for algorithm %s to validate %s"
                       (:algorithm mhash) mhash)
               {:algorithm (:algorithm mhash)})))))
