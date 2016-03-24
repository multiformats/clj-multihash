(ns multihash.test-runner
  (:require-macros
    [doo.runner :refer [doo-tests]])
  (:require
    doo.runner
    multihash.core-test
    #_ multihash.digest-test))


(doo-tests
  'multihash.core-test
  #_ 'multihash.digest-test)
