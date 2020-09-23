# pprint

Experiment to make clojure.pprint work well with GraalVM native-image.

Run `script/graal-test` to compile a `hello-world` binary and inspect the size.

The size should not be much bigger than 8-10MB, but by compiling with clojure.pprint as it, the binary becomes around 27-30MB.

Identified paths that bloat the binary:

- Line 3204: `(.isArray (class obj)) (pprint-array obj)` 
- Line 3218: `(use-method simple-dispatch clojure.lang.IPersistentSet pprint-set)` 
- Line 3219: `(use-method simple-dispatch clojure.lang.PersistentQueue pprint-pqueue)`
