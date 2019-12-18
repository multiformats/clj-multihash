(defproject mvxcvi/multihash "2.1.0-SNAPSHOT"
  :description "Native Clojure implementation of the multihash standard."
  :url "https://github.com/multiformats/clj-multihash"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :deploy-branches ["master"]
  :pedantic? :abort

  :aliases
  {"cljs-repl" ["run" "-m" "clojure.main" "cljs_repl.clj"]
   "cljs-test" ["doo" "phantom" "test" "once"]
   "coverage" ["with-profile" "+test,+coverage" "cloverage"]}

  :plugins
  [[lein-cljsbuild "1.1.7"]
   [lein-doo "0.1.8" :exclusions [org.clojure/clojurescript]]]

  :dependencies
  [[mvxcvi/alphabase "1.0.0"]
   [multiformats/clj-varint "0.1.1"]]

  :cljsbuild
  {:builds {:test {:source-paths ["src" "test"]
                   :compiler {:output-dir "target/cljs/out"
                              :output-to "target/cljs/tests.js"
                   :main multihash.test-runner
                   :optimizations :whitespace}}}}

  :codox
  {:metadata {:doc/format :markdown}
   :source-uri "https://github.com/multiformats/clj-multihash/blob/master/{filepath}#L{line}"
   :output-path "target/doc/api"}

  :whidbey
  {:tag-types {'multihash.core.Multihash {'data/hash 'multihash.core/base58}}}

  :profiles
  {:dev
   {:dependencies [[org.clojure/clojure "1.8.0"]
                   [org.clojure/clojurescript "1.9.946"]]}

   :coverage
   {:plugins [[lein-cloverage "1.0.10"]]
    :dependencies [[org.clojure/tools.reader "1.1.0"]]}})
