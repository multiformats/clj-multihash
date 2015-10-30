(defproject mvxcvi/multihash "1.0.0"
  :description "Native Clojure implementation of the multihash standard."
  :url "https://github.com/greglook/clj-multihash"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :deploy-branches ["master"]

  :plugins
  [[lein-cloverage "1.0.6"]]

  :dependencies
  [[org.clojure/clojure "1.7.0"]]

  :codox {:metadata {:doc/format :markdown}
          :source-uri "https://github.com/greglook/clj-multihash/blob/master/{filepath}#L{line}"
          :doc-paths ["doc/extra"]
          :output-path "doc/api"})
