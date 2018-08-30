(ns protocol55.kss.tests.properties
  (:require [protocol55.kss.core :as kss]
            [clojure.java.io :as io])
    (:use clojure.test))

(defn fixture-file [s]
  (io/file (str "test/protocol55/kss/tests/fixtures/" s)))

(defn some-section [reference docs]
  (some #(when (= reference (:reference %)) %) docs))

(deftest test-colors
  (let [docs (kss/parse-file (fixture-file "property-colors.less"))]
    (is (= (-> (first docs) :colors)
           [{:name "@shortHexColor", :css/color "#f00", :description "short hexa color"}
            {:name "hex6", :css/color "#e7e7e7", :description "six digit hexa color"}
            {:name "--hex8", :css/color "#ff00FF00", :description "8 digit hexa color with alpha and css custom property"}
            {:name "$rgbColor", :css/color "rgb(255,0,0)", :description "simple rgb"}
            {:name "percentRgb", :css/color "rgb(100%, 0%, 0%)", :description "rgb with percent"}
            {:name "$color-links", :css/color "#0091aa", :description "links color"}
            {:name "rgbCliped", :css/color "rgb(300,0,0)", :description "simple rgb with red overflow"}
            {:name "negativeRGB", :css/color "rgb(255,-10,0)", :description "simple rgb with negative blue"}
            {:name "clipedPercentRGB", :css/color "rgb(110%, 0%, 0%)", :description "simple rgb with percent with red overflow"}
            {:name "rgba", :css/color "rgba(255,0,0,1)", :description nil}
            {:name "rgbaPercent", :css/color "rgba(100%,0%,0%,1)", :description nil}
            {:name "rgbaFloat", :css/color "rgba(0,0,255,0.5)", :description nil}
            {:name "rgbaFloatZeroLess", :css/color "rgba(0,0,255,.5)", :description nil}
            {:name "rgbaPercentFloat", :css/color "rgba(100%, 50%, 0%, 0.1)", :description nil}
            {:name "hsl", :css/color "hsl(0, 100%, 50%)", :description nil}
            {:name "hsl2", :css/color "hsl(120, 100%, 50%)", :description nil}
            {:name "hsl3", :css/color "hsl(120, 75%, 75%)", :description nil}
            {:name "hsl4", :css/color "hsl(120, 100%, 50%)", :description nil}
            {:name "hsla", :css/color "hsla(120, 100%, 50%, 1)", :description nil}
            {:name "hsla2", :css/color "hsla(240, 100%, 50%, 0.5)", :description nil}
            {:name "hsla3", :css/color "hsla(30, 100%, 50%, 0.1)", :description nil}
            {:name "hsla4", :css/color "hsla(30, 100%, 50%, .1)", :description nil}
            {:name "namedGrey", :css/color "grey", :description "color name"}
            {:name nil, :css/color "#facdea", :description "missing name"}
            {:name nil, :css/color "ghostwhite", :description nil}]))))

(deftest test-deprecated-experimental
  (let [docs (kss/parse-file (fixture-file "property-deprecated-experimental.less"))]
    (is (= (-> (some-section "deprecated.in-header" docs)
               (select-keys [:header :deprecated?]))
           {:header "Deprecated: In Header" :deprecated? true}))

    (is (= (-> (some-section "deprecated.in-paragraph" docs)
               (select-keys [:header :description :deprecated?]))
           {:header "In Paragraph (deprecated)" :description "Deprecated: Yup" :deprecated? true}))

    (is (false? (-> (some-section "deprecated.in-modifier" docs)
                    :deprecated?
                    boolean)))

    (is (false? (-> (some-section "deprecated.not-at-beginning" docs)
                    :deprecated?
                    boolean)))

    (is (= (-> (some-section "experimental.in-header" docs)
               (select-keys [:header :experimental?]))
           {:header "Experimental: In Header" :experimental? true}))

    (is (= (-> (some-section "experimental.in-paragraph" docs)
               (select-keys [:header :description :experimental?]))
           {:header "In Paragraph (experimental)" :description "Experimental: Yup" :experimental? true}))

    (is (false? (-> (some-section "experimental.in-modifier" docs)
                    :experimental?
                    boolean)))

    (is (false? (-> (some-section "experimental.not-at-beginning" docs)
                    :experimental?
                    boolean)))))

(deftest test-header
  (let [docs (kss/parse-file (fixture-file "property-header.less"))]
    (is (= (-> (some-section "header.one-line.no-modifiers" docs)
               :header)
           "ONE LINE, NO MODIFIERS"))

    (is (= (-> (some-section "header.one-line.multiple-modifiers" docs)
               :header)
           "ONE LINE, MULTIPLE MODIFIERS"))

    (is (= (-> (some-section "header.description" docs)
               (select-keys [:header :description]))
           {:header "HEADER DETECTION" :description "SEPARATE PARAGRAPH"}))

    (is (= (-> (some-section "header.two-lines" docs)
               (select-keys [:header :description]))
           {:header "TWO LINES, MULTIPLE MODIFIERS LIKE SO"}))

    (is (= (-> (some-section "header.three-paragraphs" docs)
               (select-keys [:header :description]))
           {:header "THREE PARAGRAPHS, NO MODIFIERS" :description "ANOTHER PARAGRAPH\n\nAND ANOTHER"}))))

(deftest test-markup
  (let [docs (kss/parse-file (fixture-file "property-markup.less"))]
    (is (= (-> (some-section "markup.second-paragraph" docs)
               :markup))
        "<a href=\"#\" class=\"{{modifier_class}}\">Hello World</a>")

    (is (= (-> (some-section "markup.below-modifiers" docs)
               :markup))
        "<a href=\"#\" class=\"{{modifier_class}}\">Lorem Ipsum</a>")

    (is (= (-> (some-section "markup.at-top" docs)
               (select-keys [:markup :header :modifiers])))
        {:header "Don't be the header"
         :markup "<h1 class=\"{{modifier_class}}\">Header</h1>"
         :modifiers [{:name ".title" :description "The primary header of the document, should refelect `title` tags."}]})

    (is (= (-> (some-section "markup.multi-line" docs)
               :markup
               clojure.string/split-lines)
           ["<section>"
            "  <header>"
            "    <h1 class=\"{{modifier_class}}\">Heading</h1>"
            "  </header>"
            "</section>"]))))

(deftest test-parameters
  (let [docs (kss/parse-file (fixture-file "property-parameters.less"))]
    (is (= (-> (some-section "parameter.mixinA" docs)
               :parameters)
           [{:name "param1" :default-value nil                    :description "Parameter 1"}
            {:name "param2" :default-value "Default param2 value" :description "Parameter 2"}
            {:name "param3" :default-value "Default param3 value" :description "Parameter 3"}]))

    (is (= (-> (some-section "parameter.mixinD" docs)
               :compatibility)
           "Compatible in IE6+, Firefox 2+, Safari 4+."))

    (is (= (-> (some-section "parameter.mixinE" docs)
               :compatibility)
           "Compatibility untested."))))

(deftest test-modifiers
  (let [docs (kss/parse-file (fixture-file "property-modifiers.less"))]
    (is (nil? (-> (some-section "no-modifiers" docs)
                  :modifiers)))

    (is (= (-> (some-section "modifiers.single-white-space" docs)
               :modifiers)
           [{:name ":hover" :description "HOVER"}
            {:name ":disabled" :description "DISABLED"}]))

    (is (= (-> (some-section "modifiers.variable-white-space" docs)
               :modifiers)
           [{:name ":hover" :description "HOVER"}
            {:name ":disabled" :description "DISABLED"}
            {:name ":focus" :description "INCLUDING MULTIPLE LINES"}
            {:name ":link" :description "WITH TABS"}]))

    (is (= (-> (some-section "modifiers.classes" docs)
               :modifiers)
           [{:name ".red" :description "MAKE IT RED"}
            {:name ".yellow" :description "MAKE IT YELLOW"}
            {:name ".red.yellow" :description "MAKE IT ORANGE"}]))

    (is (= (-> (some-section "modifiers.dashes-in-classes" docs)
               :modifiers)
           [{:name ".red" :description "MAKE IT RED"}
            {:name ".yellow" :description "MAKE IT YELLOW"}
            {:name ".red-yellow" :description "MAKE IT ORANGE"}]))

    (is (= (-> (some-section "modifiers.elements" docs)
               :modifiers)
           [{:name "a" :description "Contains the image replacement"}
            {:name "span" :description "Hidden"}
            {:name "a span" :description "Two elements"}]))

    (is (= (-> (some-section "modifiers.classes-elements" docs)
               :modifiers)
           [{:name "a.big" :description "Bigger"}
            {:name "a.small" :description "Smaller"}
            {:name ".ir span" :description "IR"}
            {:name "a.big .ir" :description "Big IR"}
            {:name "a.small .ir.big" :description "Smaller Big IR"}]))

    (is (= (-> (some-section "modifiers.multiple-dashes" docs)
               :modifiers)
           [{:name ".red" :description "Color - red"}
            {:name ".yellow" :description "Color  -  yellow"}
            {:name ".blue" :description "Color - blue  -  another dash"}]))

    (is (= (-> (some-section "modifiers.after-description" docs)
               :modifiers
               count)
           2))

    (is (= (-> (some-section "modifiers.empty-markup" docs)
               :modifiers
               count)
           2))))
