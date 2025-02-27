(ns io.github.frenchy64.fully-satisfies-test
  (:require [clojure.test :refer :all]
            [io.github.frenchy64.fully-satisfies :refer :all]))

(defn te* [top-levels body]
  (let [g (gensym "clojure.test-clojure.protocols.gensym")]
    ((binding [*ns* *ns*]
       (eval `(do (ns ~g
                    (:require ~'[clojure.test :refer :all]
                              ~'[io.github.frenchy64.fully-satisfies :refer :all]))
                  ~@top-levels
                  (fn [] (do ~@body))))))
    (remove-ns g)
    nil))

(defmacro te [top-levels & body]
  `(te* '~top-levels '~body))

(defprotocol A
  (aA [this])
  (bA [this]))

(deftype NoExtended [])
(deftype Extended [])
(deftype ExtendedA [])
(deftype ExtendedAB [])
(extend-protocol A
  Extended
  (aA [_])
  ExtendedA
  (aA [_])
  ExtendedAB
  (aA [_])
  (bA [_]))

(defprotocol PWithPartialNilImpl
  (aPWithPartialNilImpl [this])
  (bPWithPartialNilImpl [this]))

(extend-protocol PWithPartialNilImpl
  nil
  (aPWithPartialNilImpl [this]))

(defprotocol PWithFullNilImpl
  (aPWithFullNilImpl [this])
  (bPWithFullNilImpl [this]))

(extend-protocol PWithFullNilImpl
  nil
  (aPWithFullNilImpl [this])
  (bPWithFullNilImpl [this]))

(defprotocol PWithPartialObjectImpl
  (aPWithPartialObjectImpl [this])
  (bPWithPartialObjectImpl [this]))

(extend-protocol PWithPartialObjectImpl
  Object
  (aPWithPartialObjectImpl [this] :default))

(defprotocol PWithFullObjectImpl
  (aPWithFullObjectImpl [this])
  (bPWithFullObjectImpl [this]))

(extend-protocol PWithFullObjectImpl
  Object
  (aPWithFullObjectImpl [this])
  (bPWithFullObjectImpl [this]))

(defprotocol PExtendViaMetadata
  :extend-via-metadata true
  (aPExtendViaMetadata [this])
  (bPExtendViaMetadata [this]))

(defprotocol PExtendViaMetadataWithPartialObjectImpl
  :extend-via-metadata true
  (aPExtendViaMetadataWithPartialObjectImpl [this])
  (bPExtendViaMetadataWithPartialObjectImpl [this]))

(extend-protocol PExtendViaMetadataWithPartialObjectImpl
  Object
  (aPExtendViaMetadataWithPartialObjectImpl [this] :default))

(defprotocol PExtendViaMetadataWithFullObjectImpl
  :extend-via-metadata true
  (aPExtendViaMetadataWithFullObjectImpl [this])
  (bPExtendViaMetadataWithFullObjectImpl [this]))

(extend-protocol PExtendViaMetadataWithFullObjectImpl
  Object
  (aPExtendViaMetadataWithFullObjectImpl [this] :default)
  (bPExtendViaMetadataWithFullObjectImpl [this] :default))

(defrecord AssumptionExtendsZeroMethodsPExtendViaMetadata
  [])
(defrecord AssumptionExtendsAPExtendViaMetadata
  [])
(extend-protocol PExtendViaMetadata
  AssumptionExtendsZeroMethodsPExtendViaMetadata
  AssumptionExtendsAPExtendViaMetadata
  (aPExtendViaMetadata [this] :extend))

(defprotocol PNumberPartialExtended
  (aPNumberPartialExtended [this])
  (bPNumberPartialExtended [this]))

(extend-protocol PNumberPartialExtended
  Number
  (aPNumberPartialExtended [this] :extend-number))

(defprotocol PNumberFullyExtended
  (aPNumberFullyExtended [this])
  (bPNumberFullyExtended [this]))

(extend-protocol PNumberFullyExtended
  Number
  (aPNumberFullyExtended [this] :extend-number)
  (bPNumberFullyExtended [this] :extend-number))

(definterface IInterface)

(defprotocol PIInterfaceExtendViaMetaPartialExtended
  :extend-via-metadata true
  (aPIInterfaceExtendViaMetaPartialExtended [this])
  (bPIInterfaceExtendViaMetaPartialExtended [this])
  (cPIInterfaceExtendViaMetaPartialExtended [this]))

(extend-protocol PIInterfaceExtendViaMetaPartialExtended
  IInterface
  (aPIInterfaceExtendViaMetaPartialExtended [this] :extend))

(defprotocol PIInterfaceExtendViaMetaFullyExtended
  :extend-via-metadata true
  (aPIInterfaceExtendViaMetaFullyExtended [this])
  (bPIInterfaceExtendViaMetaFullyExtended [this]))

(extend-protocol PIInterfaceExtendViaMetaFullyExtended
  IInterface
  (aPIInterfaceExtendViaMetaFullyExtended [this] :extend)
  (bPIInterfaceExtendViaMetaFullyExtended [this] :extend))

(deftest partially-satisfies?-test
  (te [(defprotocol A)
       (deftype T1 [] A)
       (deftype T2 [])]
      (is (partially-satisfies? A (->T1)))
      (is (not (partially-satisfies? A (->T2)))))
  (te [(defprotocol A
         :extend-via-metadata true)
       (defrecord T1 [] A)
       (defrecord T2 []) ]
      (is (partially-satisfies? A (->T1)))
      (is (not (partially-satisfies? A (->T2)))))
  (te [(defprotocol A
         :extend-via-metadata true
         (foo [this]))
       (defrecord T1 [] A)
       (defrecord T2 [])
       (def this-nstr (-> *ns* ns-name name))]
      (is (partially-satisfies? A (->T1)))
      (is (not (partially-satisfies? A (->T2))))
      (is (partially-satisfies? A (with-meta (->T2)
                                             {(symbol this-nstr "foo") identity})))))

;; TODO test abstract class implementing interface used as super. ensures
;; we use the correct getMethods vs getDeclaredMethods.
;; TODO test omitted implements shadows meta
(deftest fully-satisfies?-test
  ;; implemented directly
  (te [(defprotocol A
         (a [this])
         (b [this]))]
      (let [v (reify A)]
        (is (thrown? AbstractMethodError (a v)))
        (is (thrown? AbstractMethodError (b v)))
        (is (satisfies? A v))
        (is (not (fully-satisfies? A v))))
      (let [v (reify A (a [this] :a/reify))]
        (is (= :a/reify (a v)))
        (is (thrown? AbstractMethodError (b v)))
        (is (satisfies? A v))
        (is (not (fully-satisfies? A v))))
      (let [v (reify A (a [this] :a/reify) (b [this] :b/reify))]
        (is (= :a/reify (a v)))
        (is (= :b/reify (b v)))
        (is (satisfies? A v))
        (is (fully-satisfies? A v))))
  ;; partially implemented directly with a complete Object impl
  (is (not (fully-satisfies? PWithFullObjectImpl (reify PWithFullObjectImpl (aPWithFullObjectImpl [this])))))
  ;; via extend
  (is (not (fully-satisfies? A (->NoExtended))))
  (is (not (fully-satisfies? A (->Extended))))
  (is (not (fully-satisfies? A (->ExtendedA))))
  (is (fully-satisfies? A (->ExtendedAB)))
  ;; nil
  (is (not (fully-satisfies? A nil)))
  (is (not (fully-satisfies? PWithPartialNilImpl nil)))
  (is (fully-satisfies? PWithFullNilImpl nil))
  (is (not (fully-satisfies? PWithPartialObjectImpl nil)))
  (is (not (fully-satisfies? PWithFullObjectImpl nil)))
  ;; Object
  (is (not (fully-satisfies? A (reify))))
  (is (not (fully-satisfies? PWithPartialObjectImpl (reify))))
  (is (fully-satisfies? PWithFullObjectImpl (reify)))
  (is (fully-satisfies? PWithFullObjectImpl (Object.)))
  (is (not (fully-satisfies? PWithPartialNilImpl (reify))))
  (is (not (fully-satisfies? PWithFullNilImpl (reify))))
  (is (not (fully-satisfies? PWithPartialNilImpl (Object.))))
  (is (not (fully-satisfies? PWithFullNilImpl (Object.))))
  ;; :extend-via-metadata
  (is (not (fully-satisfies? A (with-meta {}
                                          {`aA (fn [this])
                                           `bA (fn [this])}))))
  (is (not (fully-satisfies? PExtendViaMetadata
                             (with-meta {}
                                        {`aPExtendViaMetadata (fn [this])}))))
  (is (fully-satisfies? PExtendViaMetadata
                        (with-meta {}
                                   {`aPExtendViaMetadata (fn [this])
                                    `bPExtendViaMetadata (fn [this])})))
  ;; :extend-via-metadata + Object impl
  (is (fully-satisfies? PExtendViaMetadataWithFullObjectImpl
                        (with-meta {}
                                   {`aPExtendViaMetadataWithFullObjectImpl (fn [this])})))
  ;;    no `b` impl
  (is (not (fully-satisfies? PExtendViaMetadataWithPartialObjectImpl
                             (with-meta {}
                                        {`aPExtendViaMetadataWithPartialObjectImpl (fn [this])}))))
  ;;   `a` implemented by Object
  (is (fully-satisfies? PExtendViaMetadataWithPartialObjectImpl
                        (with-meta {}
                                   {`bPExtendViaMetadataWithPartialObjectImpl (fn [this])})))
  (is (fully-satisfies? PExtendViaMetadataWithFullObjectImpl
                        (with-meta {}
                                   {`aPExtendViaMetadataWithFullObjectImpl (fn [this])
                                    `bPExtendViaMetadataWithFullObjectImpl (fn [this])})))
  (is (fully-satisfies? PExtendViaMetadataWithPartialObjectImpl
                        (with-meta {}
                                   {`aPExtendViaMetadataWithPartialObjectImpl (fn [this])
                                    `bPExtendViaMetadataWithPartialObjectImpl (fn [this])})))
  ;; `a` implemented via extend, `b` via metadata
  (let [v (with-meta (->AssumptionExtendsAPExtendViaMetadata)
                     {`bPExtendViaMetadata (fn [this] :meta)})]
    (is (= :extend (aPExtendViaMetadata v)))
    (is (= :meta (bPExtendViaMetadata v)))
    (is (fully-satisfies?
          PExtendViaMetadata
          v)))
  (doseq [v [1 1.0 1/3]]
    ;; partially extended superclass != Object
    (testing v
      (is (= :extend-number (aPNumberPartialExtended v)))
      (is (thrown? IllegalArgumentException (bPNumberPartialExtended v)))
      (is (not (fully-satisfies? PNumberPartialExtended v))))
    ;; fully extended superclass != Object
    (testing v
      (is (= :extend-number (aPNumberFullyExtended v)))
      (is (= :extend-number (bPNumberFullyExtended v)))
      (is (fully-satisfies? PNumberFullyExtended v))))
  (let [v (reify IInterface)]
    (is (= :extend (aPIInterfaceExtendViaMetaPartialExtended v)))
    (is (thrown? IllegalArgumentException (bPIInterfaceExtendViaMetaPartialExtended v)))
    (is (thrown? IllegalArgumentException (cPIInterfaceExtendViaMetaPartialExtended v)))
    (is (not (fully-satisfies? PIInterfaceExtendViaMetaPartialExtended v))))
  ;; partially extended via metadata inherits extend to partially satisfy
  (let [v (with-meta
            (reify IInterface)
            {`bPIInterfaceExtendViaMetaPartialExtended
             (fn [this] :meta)})]
    (is (= :extend (aPIInterfaceExtendViaMetaPartialExtended v)))
    (is (= :meta (bPIInterfaceExtendViaMetaPartialExtended v)))
    (is (thrown? IllegalArgumentException (cPIInterfaceExtendViaMetaPartialExtended v)))
    (is (not (fully-satisfies? PIInterfaceExtendViaMetaPartialExtended v))))
  ;; partially extended via metadata inherits extend to completely satisfy
  (let [v (with-meta
            (reify IInterface)
            {`bPIInterfaceExtendViaMetaPartialExtended
             (fn [this] :meta)
             `cPIInterfaceExtendViaMetaPartialExtended
             (fn [this] :meta)})]
    (is (= :extend (aPIInterfaceExtendViaMetaPartialExtended v)))
    (is (= :meta (bPIInterfaceExtendViaMetaPartialExtended v)))
    (is (= :meta (cPIInterfaceExtendViaMetaPartialExtended v)))
    (is (fully-satisfies? PIInterfaceExtendViaMetaPartialExtended v)))
  (let [v (with-meta
            (reify IInterface)
            {`bPIInterfaceExtendViaMetaPartialExtended
             (fn [this] :meta)})
        _ (extend-type (class v)
            PIInterfaceExtendViaMetaPartialExtended
            (cPIInterfaceExtendViaMetaPartialExtended [this] :direct-extend))]
    (is (thrown? IllegalArgumentException (aPIInterfaceExtendViaMetaPartialExtended v)))
    (is (= :meta (bPIInterfaceExtendViaMetaPartialExtended v)))
    (is (= :direct-extend (cPIInterfaceExtendViaMetaPartialExtended v)))
    (is (not (fully-satisfies? PIInterfaceExtendViaMetaPartialExtended v))))
  (te [(defprotocol A)]
      (let [v 1]
        (is (not (satisfies? A v)))
        (is (not (fully-satisfies? A v)))))
  (te [(defprotocol A)
       (extend-protocol A
         Number)]
      (let [v 1]
        (is (satisfies? A v))
        (is (fully-satisfies? A v))))
  (te [(defprotocol A
         :extend-via-metadata true)]
      (let [v 1]
        (is (not (satisfies? A v)))
        (is (not (fully-satisfies? A v)))))
  (te [(defprotocol A
         :extend-via-metadata true)]
      (let [v 'a]
        (is (not (satisfies? A v)))
        (is (not (fully-satisfies? A v)))))
  (te [(defprotocol A
         :extend-via-metadata true)]
      (let [v (with-meta 'a {})]
        (is (not (satisfies? A v)))
        (is (not (fully-satisfies? A v)))))
  (te [(defprotocol A
         :extend-via-metadata true)]
      (let [v (with-meta 'a {:a 1})]
        (is (not (satisfies? A v)))
        (is (not (fully-satisfies? A v)))))
  )

;;https://clojure.atlassian.net/browse/CLJ-2656
(deftest protocol-nondeterminism
  ;; reify, proxy, et. al all seem to sort the interfaces by name
  ;; before creating the class. That means A will always be before B
  ;; in .getInterfaces on the reified class. So first we make A a parital
  ;; implementation, then make B a partial implementation.
  (dotimes [_ 100]
    (te [(definterface A)
         (definterface B)
         (defprotocol P
           (a [this])
           (b [this]))
         (extend-protocol P
           ;; A is partial (chosen first)
           A
           (a [this] :a)
           B
           (a [this] :b)
           (b [this] :b))]
        (is (not (fully-satisfies? P (reify B A))))
        (is (not (fully-satisfies? P (reify A B))))))
  (dotimes [_ 100]
    (te [(definterface A)
         (definterface B)
         (defprotocol P
           (a [this])
           (b [this]))
         (extend-protocol P
           B
           (a [this] :a)
           ;; A is complete (chosen first)
           A
           (a [this] :b)
           (b [this] :b))]
        (is (fully-satisfies? P (reify B A)))
        (is (fully-satisfies? P (reify A B))))))

(deftest protocol-assumptions
  (is (= :a
         (aPExtendViaMetadataWithPartialObjectImpl
           (with-meta {}
                      {`aPExtendViaMetadataWithPartialObjectImpl (fn [this] :a)
                       `bPExtendViaMetadataWithPartialObjectImpl (fn [this] :b)}))))
  (is (= :default
         (aPExtendViaMetadataWithPartialObjectImpl
           (with-meta {}
                      {`bPExtendViaMetadataWithPartialObjectImpl (fn [this] :b)}))))
  (is (thrown? IllegalArgumentException
               (bPExtendViaMetadataWithPartialObjectImpl
                 {})))
  (testing "missing direct implementation overrides metadata"
    (is (= :a
           (aPExtendViaMetadataWithPartialObjectImpl
             (with-meta (reify
                          PExtendViaMetadataWithPartialObjectImpl
                          (aPExtendViaMetadataWithPartialObjectImpl [this] :a))
                        {`aPExtendViaMetadataWithPartialObjectImpl (fn [this] :b)}))))
    (is (thrown?
          AbstractMethodError
          (aPExtendViaMetadataWithPartialObjectImpl
            (with-meta (reify
                         PExtendViaMetadataWithPartialObjectImpl)
                       {`aPExtendViaMetadataWithPartialObjectImpl (fn [this] :b)})))))
  (testing "metadata overrides extend"
    (te [(defprotocol A
           :extend-via-metadata true
           (foo [this]))
         (defrecord B [])
         (extend-protocol A
           B
           (foo [_] :extend))
         (def this-nstr (-> *ns* ns-name name))]
        (is (= :meta
               (foo (with-meta (->B)
                               {(symbol this-nstr "foo") (fn [_] :meta)}))))
        (is (= :extend
               (foo (->B))))))
  (testing "missing extends implementation drops to metadata"
    ;; sanity check
    (is (thrown?
          IllegalArgumentException
          (aPExtendViaMetadata
            (->AssumptionExtendsZeroMethodsPExtendViaMetadata))))
    (is (= :b
           (aPExtendViaMetadata
             (with-meta (->AssumptionExtendsZeroMethodsPExtendViaMetadata)
                        {`aPExtendViaMetadata (fn [this] :b)}))))))
