(ns protocol55.kss.tests.generative
  (:require [protocol55.kss.core :as kss]
            [protocol55.kss.specs :as kss-specs]
            [clojure.spec.test.alpha :as st]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io])
    (:use clojure.test))

(def check-opts {:clojure.spec.test.check/opts {:num-tests 50 :max-size 50}})

(st/instrument)

(defn passes-check? [sym]
  (= 1 (:check-passed (st/summarize-results (st/check sym check-opts)))))

(deftest ^:check test-block-parser-check
  (is (passes-check? `kss/block-parser)))

(deftest ^:check test-syntax-parser-check
  (is (passes-check? `kss/syntax-parser)))

(deftest ^:check test-parse-lines-check
  (is (passes-check? `kss/parse-lines)))

(deftest ^:check test-parse-string-check
  (is (passes-check? `kss/parse-lines)))
