(require 'cljs.repl)
(require 'cljs.repl.node)

(cljs.repl/repl
  (cljs.repl.node/repl-env)
  :watch "src"
  :output-dir "target")
