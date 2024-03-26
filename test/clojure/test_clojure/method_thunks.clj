;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
; Authors: Fogus

(ns clojure.test-clojure.method-thunks
  (:use clojure.test)
  (:import (clojure.lang Compiler Tuple)
           (java.util Arrays UUID Locale)
           clojure.lang.IFn$LL))

(set! *warn-on-reflection* true)

(deftest method-arity-selection
  (is (= '([] [] [])
         (take 3 (repeatedly ^[] Tuple/create))))
  (is (= '([1] [2] [3])
         (map ^[_] Tuple/create [1 2 3])))
  (is (= '([1 4] [2 5] [3 6])
         (map ^[_ _] Tuple/create [1 2 3] [4 5 6]))))

(deftest method-signature-selection
  (is (= [1.23 3.14]
         (map ^[double] Math/abs [1.23 -3.14])))
  (is (= [(float 1.23)  (float 3.14)]
         (map ^[float] Math/abs [1.23 -3.14])))
  (is (= [1 2 3]
         (map ^[long] Math/abs [1 2 -3])))
  (is (= [#uuid "00000000-0000-0001-0000-000000000002"]
         (map ^[long long] UUID/new [1] [2])))
  (is (= '("a" "12")
         (map ^[Object] String/valueOf ["a" 12])))
  (is (= ["A" "B" "C"]
         (map ^[java.util.Locale] String/toUpperCase ["a" "b" "c"] (repeat java.util.Locale/ENGLISH))))
  (is (thrown? ClassCastException
               (doall (map ^[long] String/valueOf [12 "a"])))))

(def mt ^[_] Tuple/create)
(def mts {:fromString ^[_] UUID/fromString})

(deftest method-thunks-in-structs
  (is (= #uuid "00000000-0000-0001-0000-000000000002"
         ((:fromString mts) "00000000-0000-0001-0000-000000000002")))
  (is (= [1] (mt 1))))

(deftest primitive-hinting
  (is (instance? clojure.lang.IFn$DO ^[double] String/valueOf))
  (is (instance? clojure.lang.IFn$LL ^[long] Math/abs)))

(deftest qualified-method-resolution-exact-matches
  (is (= [1 2 3] (map Math/abs [(int -1) (int -2) (int 3)])))
  (is (= ["A" "B"] (map String/toUpperCase ["a" "b"])))
  (is (= 0 (apply java.util.UUID/version [#uuid "00000000-0000-0001-0000-000000000002"])))
  (is (= [[1] [2] [3]] (map clojure.lang.Tuple/create [1 2 3])))
  (is (not (apply Double/isNaN [(double 42)])))  ;; currently dispatches to static
  (is (not (apply Double/isNaN [(Double. "42.1")]))) ;; currently dispatches to static
  (is (= #uuid "00000000-0000-0001-0000-000000000002" (apply UUID/new [1 2]))))

#_(deftest qualified-method-resolution-failing-cases
  (is (= [1 2 3] (map Math/abs [-1 -2 3])))
  (is (= 1 (apply java.util.Arrays/binarySearch [(int-array [10 20 30]) (int 20)])))
  (is (= "" (apply String/format ["Hello %s" (to-array ["World"])]))))
