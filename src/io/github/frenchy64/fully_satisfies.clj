(ns io.github.frenchy64.fully-satisfies
  (:import [clojure.lang IMeta Var]
           [java.lang.reflect Method]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn fully-satisfies?
  "Returns true if value v extends protocol p and
  implements every method in protocol p, otherwise false.

  A value is considered to 'extend' protocol p either if:
  - p implements the protocols interface, or
  - p extends the protocol via clojure.core/extend, or
  - p implements at least one method via metadata if supported
    by the protocol"
  [p v]
  (let [c (class v)
        ^Class i (:on-interface p)
        ims (.getMethods i)]
    (if (instance? i v)
      (let [l (alength ims)]
        (loop [idx 0]
          (if (< idx l)
            (let [^Method im (aget ims idx)
                  cm (.getMethod c (.getName im) (.getParameterTypes im))]
              (if (zero? (bit-and (.getModifiers cm)
                                  ;; abstract flag
                                  0x0400))
                (recur (unchecked-inc-int idx))
                false))
            true)))
      (let [cimpl (when-some [impls (:impls p)]
                    (or (get impls c)
                        (when (and c (not (identical? Object c)))
                          (let [dfs-for-interface (fn dfs-for-interface [^Class c]
                                                    (when-not (identical? Object c)
                                                      (or (when (.isInterface c)
                                                            (get impls c))
                                                          ;; order of interfaces is strange for reify, normalize it
                                                          (->> (.getInterfaces c)
                                                               ;; mutates
                                                               (sort-by #(.getName ^Class %))
                                                               (some dfs-for-interface))
                                                          (when (not (.isInterface c))
                                                            (recur (.getSuperclass c))))))]
                            (or (loop [^Class c (.getSuperclass c)]
                                  (when-not (identical? Object c)
                                    (or (get impls c)
                                        (recur (.getSuperclass c)))))
                                (dfs-for-interface c)
                                (get impls Object))))))]
        (if cimpl
          (or (.equals ^Object (count cimpl) (alength ims))
              (if-some [vm (when (:extend-via-metadata p) (meta v))]
                (let [^Var pvar (:var p)
                      nstr (-> pvar .ns .name name)]
                  (every? (fn [mmap-key]
                            (or (get cimpl mmap-key)
                                (get vm (symbol nstr (name mmap-key)))))
                          (-> p :method-map keys)))
                false))
          (if-some [vm (and (:extend-via-metadata p)
                            (meta v))]
            (if-some [method-map-keys (-> p :method-map keys seq)]
              (let [^Var pvar (:var p)
                    nstr (-> pvar .ns .name name)]
                (every? (fn [mmap-key]
                          (get vm (symbol nstr (name mmap-key))))
                        method-map-keys))
              false)
            false))))))
