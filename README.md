# pprint

Experiment to make clojure.pprint work well with GraalVM native-image.

Run `script/graal-test` to compile a `hello-world` binary and inspect the size.

The size should not be much bigger than 8-10MB, but by compiling with clojure.pprint as it, the binary becomes around 27-30MB.

Identified paths that bloat the binary:

- Line 3204: `(.isArray (class obj)) (pprint-array obj)` (only the `pprint-array` triggers it)
- Line 3218: `(use-method simple-dispatch clojure.lang.IPersistentSet pprint-set)` 
- Line 3219: `(use-method simple-dispatch clojure.lang.PersistentQueue pprint-pqueue)`

All of these use `formatter-out` macro which needs a closer look.

Yup: all triggered by `compile-format`.

=> `(compile-directive (subs s 1) (inc offset))`
=> `(struct compiled-directive ((:generator-fn def) params offset) def params offset)` 
=> `(struct compiled-directive nil #_((:generator-fn def) params offset) def params offset)`
This is ok: `(struct compiled-directive nil #_((:generator-fn def) params offset) nil #_def nil #_params nil #_offset)`
Not ok: `(struct compiled-directive ((:generator-fn def) params offset) nil #_def nil #_params nil #_offset)` 
