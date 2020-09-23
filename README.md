# pprint

Experiment to make clojure.pprint work well with GraalVM native-image.

Run `script/graal-test` to compile a `hello-world` binary and inspect the size.

The size should not be much bigger than 8-10MB (in my experience with similar libraries), but by compiling with clojure.pprint as is, the binary becomes around 27-30MB.

## Tl;dr version

`table-ize` uses `find-var` which tends to bloat GraalVM native images.

The `find-var` can be avoided by making `write-option-table` a table of keywords to vars.
If we do that, the binary ends up being only 10MB and GraalVM memory usage during compilation is reduced significantly.

## Longer version

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
Ok: `(struct compiled-directive (:foo #_(:generator-fn def) params offset) nil #_def nil #_params nil #_offset)`
Problem seems to be in referring to `def`.
In `process-directive-table-element` commenting out `generator-fn` helps:
`(concat '(fn [ params offset]) nil #_generator-fn)` 
So maybe there is one generator-fn triggering bloating.
Commented out pretty much everything in `defdirectives`. Left with:
``` 
(apply (fn [& args] (even? (count args)))#_write arg bindings)
```
so it seems like the reference to `write` is causing bloat.
In `write` commenting out the body doesn't solve the bloat, so maybe it's caused by `binding-map` or `tabl-ize`.
Turns out it's `table-ize`. And there we have it... `find-var`!!!
``` 
(defn- table-ize [t m]
  (apply hash-map (mapcat
                   #(when-let [v (get t (key %))] [(find-var v) (val %)])
                   m)))
``` 

The `find-var` can be avoided by making `write-option-table` a table of keywords to vars.
