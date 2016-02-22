(ns multihash.digest
  "Digest functions for creating new multihash constructors."
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
         (create ~algorithm (digest-content ~digest-name ~'content)))
       (alter-var-root #'functions assoc ~algorithm ~fn-sym))))
