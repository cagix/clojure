;; This code was used to generate the clojure.lang.FnAdapters class in
;; Clojure 1.12. This code is not intended to be reused but might be
;; useful in the future as a template for other code gen.

(ns gen-fn-invokers
  (:require
    [clojure.string :as str])
  (:import
    [java.io StringWriter Writer]))

(def header
  "/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

package clojure.lang;

public class FnInvokers {

    private static RuntimeException notIFnError(Object f) {
        return new RuntimeException(\"Expected function, but found \" + (f == null ? \"null\" : f.getClass().getName()));
    }

")

(def footer
  "}")

(def invokeO-format
  "    public static Object invoke%sO(Object f0%s) {
        if(f0 instanceof IFn) {
            return ((IFn)f0).invoke(%s);
        } else {
            throw notIFnError(f0);
        }
    }")

(def invokeO-with-l-or-d-arg-format
  "    public static Object invoke%sO(Object f0%s) {
        if(f0 instanceof IFn.%sO) {
            return ((IFn.%sO)f0).invokePrim(%s);
        } else if(f0 instanceof IFn) {
            return ((IFn)f0).invoke(%s);
        } else {
            throw notIFnError(f0);
        }
    }")

(def invokeB-format
  "    public static boolean invoke%sB(Object f0%s) {
        if(f0 instanceof IFn) {
            return RT.booleanCast(((IFn)f0).invoke(%s));
        } else {
            throw notIFnError(f0);
        }
    }")

(def invokeF-format
  "    public static float invoke%sF(Object f0%s) {
        if(f0 instanceof IFn) {
            return RT.FloatCast(((IFn)f0).invoke(%s));
        } else {
            throw notIFnError(f0);
        }
    }")

(def invokeL-format
  "    public static long invoke%sL(Object f0%s) {
        if(f0 instanceof IFn.%sL) {
            return ((IFn.%sL)f0).invokePrim(%s);
        } else if(f0 instanceof IFn) {
            return RT.longCast(((IFn)f0).invoke(%s));
        } else {
            throw notIFnError(f0);
        }
    }")

(def invokeD-format
  "    public static double invoke%sD(Object f0%s) {
        if(f0 instanceof IFn.%sD) {
            return ((IFn.%sD)f0).invokePrim(%s);
        } else if(f0 instanceof IFn) {
            return RT.doubleCast(((IFn)f0).invoke(%s));
        } else {
            throw notIFnError(f0);
        }
    }")

(def invokeI-format
  "    public static int invoke%sI(Object f0%s) {
        if(f0 instanceof IFn.%sL) {
            return RT.intCast(((IFn.%sL)f0).invokePrim(%s));
        } else if(f0 instanceof IFn) {
            return RT.intCast(((IFn)f0).invoke(%s));
        } else {
            throw notIFnError(f0);
        }
    }")

(def alphabet (map char (range 97 122)))

(def arg-types {:D ", double "
                :O ", Object "
                :L ", long "
                :I ", int "
                :B ", boolean "})

(defn gen-invoke [sig]
  (let [formatter (str (last sig))
        args (map str (butlast sig))
        arg-types (map #(get arg-types (keyword %)) args)
        fn-vars (str/join "" (map #(str %1 %2) arg-types (take (count args) alphabet)))
        fn-vars-sans-type (str/join ", " (take (count args) alphabet))
        arg-str (str/join args)]
    (case formatter
      "O" (if (some #{"D" "L"} args)
            (format invokeO-with-l-or-d-arg-format arg-str fn-vars arg-str arg-str fn-vars-sans-type fn-vars-sans-type)
            (format invokeO-format arg-str fn-vars fn-vars-sans-type))
      "L" (format invokeL-format arg-str fn-vars arg-str arg-str fn-vars-sans-type fn-vars-sans-type)
      "I" (format invokeI-format arg-str fn-vars arg-str arg-str fn-vars-sans-type fn-vars-sans-type)
      "D" (format invokeD-format arg-str fn-vars arg-str arg-str fn-vars-sans-type fn-vars-sans-type)
      "B" (format invokeB-format arg-str fn-vars fn-vars-sans-type)
      "F" (format invokeF-format arg-str fn-vars fn-vars-sans-type))))

(defn sigs [args return-types]
  (let [fun-sig-reducer (fn [res ret]
                          (mapcat seq [res (map (fn [params]
                                                  (str params ret)) args)]))]
    (reduce fun-sig-reducer [] return-types)))

(defn gen-sigs []
  (let [single-arity (sigs ["L" "D" "O"] ["L" "I" "B" "D" "O" "F"])
        two-arity (sigs ["LL" "LO" "OL" "DD" "LD" "DL" "OO" "OD" "DO"] ["L" "I" "B" "D" "O" "F"])
        three-arity (sigs ["OOO"] ["B" "O"])
        four-arity  (sigs ["OOOO"] ["B" "O"])
        five-arity  (sigs ["OOOOO"] ["B" "O"])
        six-arity   (sigs ["OOOOOO"] ["B" "O"])
        seven-arity (sigs ["OOOOOOO"] ["B" "O"])
        eight-arity (sigs ["OOOOOOOO"] ["B" "O"])
        nine-arity  (sigs ["OOOOOOOOO"] ["B" "O"])
        ten-arity   (sigs ["OOOOOOOOOO"] ["B" "O"])]
    (mapcat seq [single-arity two-arity three-arity four-arity five-arity six-arity seven-arity eight-arity nine-arity ten-arity])))

(defn gen-invokers []
  (let [sb (StringBuilder. ^String header)
        invoker-signatures (gen-sigs)]
    (doseq [sig invoker-signatures]
      (.append sb (gen-invoker sig))
      (.append sb "\n\n"))
    (.append sb footer)
    (spit "src/jvm/clojure/lang/FnAdapters.java" (.toString sb))))
