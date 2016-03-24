(defproject mvxcvi/multihash "2.0.0-SNAPSHOT"
  :description "Native Clojure implementation of the multihash standard."
  :url "https://github.com/greglook/clj-multihash"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :deploy-branches ["master"]

  :aliases {"cljs-repl" ["run" "-m" "clojure.main" "cljs_repl.clj"]}

  :plugins
  [[lein-cljsbuild "1.1.2"]
   [lein-cloverage "1.0.6"]
   [lein-doo "0.1.6"]]

  :dependencies
  [[mvxcvi/alphabase "0.2.0"]]

  :cljsbuild
  {:builds {:test {:source-paths ["src" "test"]
                   :compiler {:output-dir "target/cljs/out"
                              :output-to "target/cljs/tests.js"
                   :main multihash.test-runner
                   :optimizations :whitespace}}}}

  :codox
  {:metadata {:doc/format :markdown}
   :source-uri "https://github.com/greglook/clj-multihash/blob/master/{filepath}#L{line}"
   :doc-paths ["doc/extra"]
   :output-path "doc/api"}

  :whidbey
  {:tag-types {'multihash.core.Multihash {'data/hash 'multihash.core/base58}}}

  :profiles
  {:dev {:dependencies
         [[org.clojure/clojure "1.8.0"]
          [org.clojure/clojurescript "1.8.34"]]}})
