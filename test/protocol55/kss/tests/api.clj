(ns protocol55.kss.tests.api
  (:require [protocol55.kss.core :as kss]
            [protocol55.kss.specs :as kss-specs]
            [clojure.spec.test.alpha :as st]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io])
    (:use clojure.test))

(st/instrument)

(def sample-css
"/*
A button suitable for giving stars to someone.


:hover             - Subtle hover highlight.
.stars-given       - A highlight indicating you’ve already given a star.
.stars-given:hover - Subtle hover highlight on top of stars-given styling.
.disabled          - Dims the button to indicate it cannot be used.



Styleguide 2.1.3.
*/
a.button.star{
  …
}
a.button.star.stars-given{
  …
}
a.button.star.disabled{
  …
}")

(def sample-file (io/file "test/protocol55/kss/tests/fixtures/property-colors.less"))

(deftest test-parse-lines
  (is (s/valid? ::kss-specs/docs (kss/parse-lines (clojure.string/split-lines sample-css)))))

(deftest test-parse-string
  (is (s/valid? ::kss-specs/docs (kss/parse-string sample-css))))

(deftest test-parse-file
  (is (s/valid? ::kss-specs/docs (kss/parse-file sample-file))))

(deftest test-parse-stream
  (is (with-open [rdr (io/reader sample-file)] (doall (kss/parse-stream rdr)))))
