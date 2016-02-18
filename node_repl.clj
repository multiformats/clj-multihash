(require 'cljs.repl)
(require 'cljs.build.api)
(require 'cljs.repl.node)

#_
(cljs.build.api/build "src"
  {:main 'hello-world.core
   :output-to "target/main.js"
   :verbose true})

(cljs.repl/repl (cljs.repl.node/repl-env)
  :watch "src"
  :output-dir "target")
