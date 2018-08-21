(ns protocol55.kss.specs.gen
  (:require [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))

(s/def ::opacity (s/double-in :min 0.0 :max 1.0 :infinite? false :NaN? false))

(defn descriptor-gen []
  (gen/fmap #(str % ":")
            (s/gen (s/and string? #(not (empty? %))))))

(defn rgb-gen []
  (gen/fmap #(str "rgb(" (clojure.string/join "," %) ")")
            (s/gen (s/tuple int? int? int?))))

(defn rgba-gen []
  (gen/fmap #(str "rgba(" (clojure.string/join "," %) ")")
            (s/gen (s/tuple int? int? int? ::opacity))))

(defn append-percent-sign [s] (str s "%"))

(defn hsl-gen []
  (gen/fmap #(str "hsl(" (clojure.string/join "," (map append-percent-sign %)) ")")
            (s/gen (s/tuple int? int? int?))))

(defn hsla-gen []
  (gen/fmap #(str "hsla(" (clojure.string/join
                            "," (concat (map append-percent-sign (butlast %))
                                        (take-last 1 %)))
                  ")")
            (s/gen (s/tuple int? int? int? ::opacity))))

(s/def ::hex-color-char #{"f" "F"})
(s/def ::hex-color-int (s/int-in 0 10))

(defn hex-gen []
  (gen/fmap
    #(apply str "#" %)
    (s/gen (s/tuple ::hex-color-char ::hex-color-int
                    ::hex-color-char ::hex-color-int
                    ::hex-color-char ::hex-color-int))))

(defn web-color-gen []
  (s/gen #{"ghostwhite" "red" "rebeccapurple" "midnightblue"}))

(defn color-value-gen []
  (gen/one-of [(rgb-gen) (rgba-gen) (hsl-gen) (hsla-gen) (hex-gen) (web-color-gen)]))

(defn text-gen []
  (gen/fmap
    #(clojure.string/join " " %)
    (s/gen (s/coll-of (s/and string? #(not (empty? %)))))))

(defn color-gen []
  (gen/fmap
    (fn [[n color name description]]
      (case n
        0 color
        1 (str color " - " description)
        2 (str name " " color)
        3 (str name " " color " - " description)))
    (gen/tuple
      (s/gen (s/int-in 0 4))
      (color-value-gen)
      (descriptor-gen)
      (text-gen))))

(defn color-block-gen []
  (gen/fmap
    #(clojure.string/join "\n" %)
    (gen/list (color-gen))))

(defn modifier-or-parameter-gen [{:keys [name-gen]}]
  (gen/fmap
    (fn [[n name description]]
      (case n
        0 (str name " - ")
        1 (str name " - " description)))
    (gen/tuple
      (s/gen (s/int-in 0 2))
      name-gen
      (text-gen))))

(defn modifier-gen []
  (modifier-or-parameter-gen
    {:name-gen (gen/fmap #(clojure.string/join "." %)
                         (s/gen (s/coll-of #{"foo" "bar" "is-disabled"})))}))

(defn parameter-gen []
  (modifier-or-parameter-gen
    {:name-gen (gen/fmap
                 (fn [[n name default-value]]
                   (case n
                     0 name
                     1 (str name " = " default-value)))
                 (gen/tuple
                   (s/gen (s/int-in 0 2))
                   (s/gen #{"$foo" "--bar"})
                   (s/gen string?)))}))

(defn comment-model-gen []
  (gen/fmap
    (fn [[n header description desc-prefix colors modifiers parameters markup styleguide styleguide-sep reference]]
      {:id n
       :header header
       :description (str desc-prefix description)
       :colors (into ["colors:"] colors)
       :modifiers modifiers
       :parameters parameters
       :markup (into ["markup:"] markup)
       :styleguide (str styleguide styleguide-sep (clojure.string/join "." reference))})
    (gen/tuple
      (s/gen (s/int-in 0 9))
      (s/gen string?)                                                     ;; header
      (s/gen string?)                                                     ;; description
      (s/gen #{"" "Experimental: " "Deprecated: "})                       ;; desc-prefix
      (gen/list (color-gen))                                              ;; colors
      (gen/list (modifier-gen))                                           ;; modifiers
      (gen/list (parameter-gen))                                          ;; parameters
      (s/gen (s/coll-of #{"<div>foo</div>" "  <span>bar</span>"}))        ;; markup
      (s/gen #{"styleguide" "style guide"})                               ;; styleguide
      (s/gen #{" " " - " ": "})                                           ;; styleguide-sep
      (s/gen (s/coll-of int?))                                            ;; reference
      )))

(defn comment-block-composition [{:keys [id]}]
  (case id
    0 [:header :styleguide]
    1 [:header :description :styleguide]
    2 [:header :description :modifiers :markup :styleguide]
    3 [:header :description :modifiers :markup :colors :styleguide]
    4 [:description :modifiers :markup :colors :styleguide]
    5 [:modifiers :markup :colors :styleguide]
    6 [:markup :colors :styleguide]
    7 [:colors :styleguide]
    8 [:styleguide]))

(defn assemble-comment-block [parts comment-type]
  (let [prefix
        (case comment-type
          0 "// "
          1 " "
          2 " * "
          3 "")

        block
        (->> parts
             (mapcat #(if (string? %) [% (str "\n" prefix)] (concat % [(str "\n" prefix)])))
             (butlast)
             (map #(str prefix %))
             (clojure.string/join "\n"))]
    (case comment-type
      0 block
      1 (str "/*\n" block "\n */")
      2 (str "/**\n" block "\n */")
      3 block)))

(defn comment-gen []
  (gen/fmap
    (fn [[comment-type model]]
      (let [composition (comment-block-composition model)]
        (assemble-comment-block
          (map model composition)
          comment-type)))
    (gen/tuple
      (s/gen (s/int-in 0 3))
      (comment-model-gen))))

(defn parsed-comment-gen
  "comment text with no comment markup"
  []
  (gen/fmap
    (fn [model]
      (let [composition (comment-block-composition model)]
        (assemble-comment-block
          (map model composition)
          4)))
    (comment-model-gen)))

(defn comment-lines-gen []
  (gen/fmap #(clojure.string/split-lines %) (comment-gen)))

(comment
 (gen/sample (comment-lines-gen)))
