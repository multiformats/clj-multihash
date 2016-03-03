(require 'cljs.repl)
(require 'cljs.repl.rhino)

(cljs.repl/repl
  (cljs.repl.rhino/repl-env)
  :watch "src"
  :output-dir "target")
