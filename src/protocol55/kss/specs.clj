(ns protocol55.kss.specs
  (:require [clojure.spec.alpha :as s]
            [protocol55.kss.specs.gen :as kss-gen]))

(s/def ::header (s/nilable string?))
(s/def ::description (s/nilable string?))
(s/def ::compatibility string?)
(s/def ::deprecated? boolean?)
(s/def ::experimental? boolean?)
(s/def ::reference string?)
(s/def ::markup string?)
(s/def ::weight float?)
(s/def ::name (s/nilable string?))




;; modifiers

(s/def ::modifier (s/keys :opt-un [::description] :req-un [::name]))
(s/def ::modifiers (s/coll-of ::modifier))




;; parameters

(s/def ::default-value (s/nilable string?))
(s/def ::parameter (s/keys :opt-un [::default-value ::description] :req-un [::name]))
(s/def ::parameters (s/coll-of ::parameter))




;; colors

(s/def :css/color string?)
(s/def ::color (s/keys :opt-un [::name ::description] :req [:css/color]))
(s/def ::colors (s/coll-of ::color))




;; source

(s/def ::base string?)
(s/def ::path string?)
(s/def ::source (s/nilable (s/keys :opt-un [::base ::name ::path])))





;; kss

(s/def ::kss
  (s/keys :opt-un [::header ::description ::compatibility
                   ::deprecated? ::experimental? ::reference ::markup ::weight 
                   ::modifiers ::parameters ::colors ::source]))




;; parser yield

(s/def ::type #{:single :multi :doc})
(s/def ::raw string?)
(s/def ::text string?)
(s/def ::ln number?)
(s/def ::block-ln (s/nilable number?))
(s/def ::continued-parameter-or-modifier-description string?)
(s/def ::continued-description string?)
(s/def ::continued-header string?)
(s/def ::continued-compatibility string?)
(s/def ::markup-marker string?)
(s/def ::block-yield
  (s/nilable (s/keys :opt-un [::raw ::text ::type ::ln ::block-ln])))
(s/def ::syntax-yield
  (s/nilable
    (s/merge ::kss
             (s/keys :opt-un [::continued-parameter-or-modifier-description
                              ::continued-description
                              ::continued-compatibility
                              ::continued-header
                              ::markup-marker]))))
(s/def ::yield (s/merge ::syntax-yield ::block-yield))




;; block state

(s/def ::single-block? boolean?)
(s/def ::multi-block? boolean?)
(s/def ::doc-block? boolean?)
(s/def ::indent (s/nilable (s/or :off boolean? :spaces string?)))
(s/def ::line string?)
(s/def ::lines (s/spec (s/coll-of string?) :gen kss-gen/comment-lines-gen))
(s/def ::remaining (s/nilable ::lines))
(s/def ::end boolean?)
(s/def ::block-parser-state
  (s/keys :opt-un [::remaining ::ln ::block-ln ::indent ::single-block? ::multi-block? ::doc-block? ::yield ::end]))




;; syntax state

(s/def ::markup-block? boolean?)
(s/def ::weight-block? boolean?)
(s/def ::colors-block? boolean?)
(s/def ::parameters-block? boolean?)
(s/def ::modifiers-block? boolean?)
(s/def ::description-block? boolean?)
(s/def ::header-block? boolean?)
(s/def ::parsing-block-ln ::block-ln)
(s/def ::syntax-parser-state
  (s/keys :opt-un [::yield ::markup-block? ::weight-block? ::colors-block?
                   ::block-ln ::parsing-block-ln ::parameters-block? ::modifiers-block?
                   ::description-block? ::header-block?]))




;; state

(s/def ::state (s/merge ::block-parser-state ::syntax-parser-state))




;; private api

(s/fdef protocol55.kss.core/block-parser
        :args (s/cat :state ::block-parser-state)
        :ret ::block-parser-state)

(s/fdef protocol55.kss.core/syntax-parser
        :args (s/cat :state ::syntax-parser-state)
        :ret ::syntax-parser-state)




;; public api

(s/def ::docs (s/coll-of ::kss))

(s/fdef protocol55.kss.core/parse-lines
        :args (s/cat :lines ::lines)
        :ret ::docs)

(s/def ::css (s/spec string? :gen kss-gen/comment-gen))

(s/fdef protocol55.kss.core/parse-string
        :args (s/cat :s ::css)
        :ret ::docs)

(s/fdef protocol55.kss.core/parse-file
        :args (s/cat :file #(instance? java.io.File %))
        :ret ::docs)

(s/fdef protocol55.kss.core/parse-stream
        :args (s/cat :file #(instance? java.io.BufferedReader %))
        :ret ::docs)
