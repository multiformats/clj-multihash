(defproject mvxcvi/multihash "0.1.1"
  :description "Native Clojure implementation of the multihash standard."
  :url "https://github.com/greglook/clj-multihash"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :deploy-branches ["master"]

  :plugins [[lein-cloverage "1.0.2"]]

  :dependencies [[org.clojure/clojure "1.6.0"]]

  :codox {:defaults {:doc/format :markdown}
          :output-dir "doc/api"
          :src-dir-uri "https://github.com/greglook/clj-multihash/blob/master/"
          :src-linenum-anchor-prefix "L"})
