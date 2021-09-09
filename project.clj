(defproject io.github.frenchy64/fully-satisfies "1.0.0-SNAPSHOT"
  :description "A variant of clojure.core/satisfies? that checks all methods are implemented."
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]]
  :profiles {:bench
             {:source-paths ["scripts"]
              :dependencies [[com.clojure-goes-fast/clj-async-profiler "0.5.1"]
                             [criterium/criterium "0.4.6"]]
              :jvm-opts ["-Djdk.attach.allowAttachSelf"]}}
  :repl-options {:init-ns io.github.frenchy64.fully-satisfies})
