(ns protocol55.kss.core
  (:require [clojure.string :as s]
            [clojure.java.io :as io]))

(def single-comment-regex #"^\s*\/\/.*$")
(def multi-finish-comment-regex #"^\s*\*\/\s*$")
(def multi-start-comment-regex #"^\s*\/\*+\s*$")
(def doc-block-start-comment-regex #"^\s*\/\*\*\s*$")

(defn block-end? [line {:keys [single-block? multi-block? doc-block?]}]
 (or single-block?
     (and (or multi-block? doc-block?)
          (boolean (re-matches multi-finish-comment-regex line)))))

(defn parse-single? [line {:keys [multi-block? doc-block?]}]
  (and (not multi-block?)
       (not doc-block?)
       (boolean (re-matches single-comment-regex line))))

(def reference-regex #"(?i)^style\s?guide\s?[-:]?\s?(.*?)\.?$")

(defn reference? [s]
  (and (string? s) (boolean (re-find reference-regex s))))

(defn reference-str [s]
  (let [s (s/trim s)]
    (when (reference? s)
      (let [[_ reference] (re-find reference-regex s)]
        (when-not (s/blank? reference)
          reference)))))

(defn has-prefix? [prefix s]
  (and (string? s)
       (boolean (re-find (re-pattern (str "(?i)^\\s*" (name prefix) "\\:")) s))))

(defn prefix-str [prefix s]
  (let [s (s/trim s)]
    (when (has-prefix? prefix s)
      (let [v (s/replace s (re-pattern (str "(?i)^\\s*" (name prefix) "\\:")) "")]
        (when-not (s/blank? v)
          (s/trim v))))))

(defn parse-weight [s]
  (try
    (Float/parseFloat s)
    (catch Exception e 0.0)))

(def css4-color-regex
  #"(?im)^(?:(\S+)\s*:\s*)?([a-zA-Z]+|#[0-9a-f]{3}|#(?:[0-9a-f]{2}){2,4}|(?:rgb|hsl)a?\((?:-?\d+(?:E-\d+)?%?[,\s]+){2,3}\s*-?[\d\.]+(?:E-\d+)?%?\))(?:\h*-\h*(.*))?$")

(defn parse-color [s]
  (let [match (re-find css4-color-regex (s/trim s))]
    (zipmap [:name :css/color :description] (rest match))))

(def parameter-or-modifier-regex #"^\s*(.+?)(?:\s*=\s?(.+))?\s+\-\s+(.+)\s*$")

(defn parameter-or-modifier? [s]
  (and (string? s)
       (boolean (re-find parameter-or-modifier-regex s))))

(defn parse-parameter-or-modifier [s]
  (let [match (re-find parameter-or-modifier-regex (s/trim s))]
    (zipmap [:name :default-value :description] (rest match))))

(defn first-line? [{:keys [block-ln ln] block-type :type :as yield}]
  (when (and block-ln ln block-type)
    (case block-type
      :single
      (= block-ln ln)

      (:doc :multi)
      (= block-ln (dec ln))

      nil)))

(def compatibility-regex #"^\s*(Compatible|Compatibility)")

(defn compatibility? [s]
  (and (string? s)
       (boolean (re-find compatibility-regex s))))

(def reset-syntax-flags
  {:markup-block? false :weight-block? false :colors-block? false
   :parameters-or-modifiers-block? false :description-block? false
   :compatibility-block? false :header-block? false})

(defn syntax-parser
  [{{:keys [text] :as yield} :yield
    :keys [markup-block? parsing-block-ln block-ln weight-block? colors-block?
           parameters-or-modifiers-block? description-block? compatibility-block?
           header-block?]
    :as state}]
  (cond
    (not= parsing-block-ln block-ln)
    (recur (merge (assoc state :parsing-block-ln block-ln) reset-syntax-flags))

    ;; reset on line breaks
    (s/blank? text)
    (merge state reset-syntax-flags)

    (reference? text)
    (if-some [reference (reference-str text)]
      (update state :yield assoc :reference (reference-str text))
      state)

    ;; check markup
    (has-prefix? :markup text)
    (if-some [v (prefix-str :markup text)]
      (update state :yield assoc :markup text)
      (-> (update state :yield assoc :markup-marker text)
          (assoc :markup-block? true)))

    ;; check markup-block?
    markup-block?
    (update state :yield assoc :markup text)

    ;; check weight
    (has-prefix? :weight text)
    (if-some [v (prefix-str :weight text)]
      (update state :yield assoc :weight (parse-weight v))
      (assoc state :weight-block? true))

    weight-block?
    (update state :yield assoc :weight (parse-weight text))

    ;; check colors
    (has-prefix? :colors text)
    (assoc state :colors-block? true)

    colors-block?
    (update state :yield assoc :color (parse-color text))

    (parameter-or-modifier? text)
    (-> (update state :yield assoc :parameter-or-modifier (parse-parameter-or-modifier text))
        (assoc :parameters-or-modifiers-block? true))

    parameters-or-modifiers-block?
    ;; multi-line parameter
    (update state :yield assoc :continued-parameter-or-modifier-description (s/trim text))

    (compatibility? text)
    ;; compatibility
    (-> (update state :yield assoc :compatibility text)
        (assoc :compatibility-block? true))

    compatibility-block?
    (update state :yield assoc :continued-compatibility text)

    ;; check experimental - store as description or header
    (has-prefix? :experimental text)
    (let [[kss-key state-key] (if (first-line? yield)
                                [:header :header-block?]
                                [:description :description-block?])]
      (-> (update state :yield assoc :experimental? true kss-key text)
          (assoc state-key true)))

    ;; check deprecated - store as description or header
    (has-prefix? :deprecated text)
    (let [[kss-key state-key] (if (first-line? yield)
                                [:header :header-block?]
                                [:description :description-block?])]
      (-> (update state :yield assoc :deprecated? true kss-key text)
          (assoc state-key true)))

    description-block?
    (update state :yield assoc :continued-description text)

    header-block?
    (update state :yield assoc :continued-header text)

    (first-line? yield)
    (-> (update state :yield assoc :header text)
        (assoc :header-block? true))

    :else
    (-> (update state :yield assoc :description text)
        (assoc :description-block? true))))

(defn block-parser
  [{[line & remaining :as stream] :remaining
    :keys [ln block-ln indent single-block? multi-block? doc-block?]
    :or {ln 0 single-block? false multi-block? false doc-block? false}
    :as state}]
  (if line
    (let [line (s/trim line)]
      (cond
        (parse-single? line state)
        (let [block-ln (if single-block? block-ln ln)]
          (assoc state
                 :remaining remaining
                 :ln (inc ln)
                 :block-ln block-ln
                 :single-block? true
                 :yield {:raw line
                         :text (s/replace-first line #"^\s*\/\/\s?" "")
                         :type :single
                         :ln ln
                         :block-ln block-ln}))

        (block-end? line state)
        (if single-block?
          (recur ;; reparse this line after setting flags
            (assoc state
                   :remaining stream
                   :block-ln nil
                   :single-block? false))
          (assoc state
                 :remaining remaining
                 :ln (inc ln)
                 :yield nil
                 :block-ln nil
                 :multi-block? false
                 :doc-block? false
                 :indent nil))

        (re-matches doc-block-start-comment-regex line)
        (assoc state
               :remaining remaining
               :ln (inc ln)
               :yield nil
               :block-ln ln
               :doc-block? true)

        doc-block?
        (assoc state
               :remaining remaining
               :ln (inc ln)
               :yield {:raw line
                       :text (s/replace line #"^\s*\*\s?" "")
                       :type :doc
                       :ln ln
                       :block-ln block-ln})

        (re-matches multi-start-comment-regex line)
        (assoc state
               :remaining remaining
               :ln (inc ln)
               :yield nil
               :block-ln ln
               :multi-block? true)

        multi-block?
        (let [indent (or indent (re-find #"^\s*" line))]
          (assoc state
                 :remaining remaining
                 :ln (inc ln)
                 :yield {:raw line
                         :text (s/replace-first line (re-pattern (str "^" indent)) "")
                         :type :multi
                         :ln ln
                         :block-ln block-ln}))

        :else
        (assoc state
               :remaining remaining
               :ln (inc ln)
               :yield nil)))
    (assoc state
           :end true
           :remaining remaining)))

(defn update-last [ms k f & args]
  (conj (pop ms) (apply update (peek ms) k f args)))

(defn assemble-yields [xs]
  (let [markup? (some #(or (some? (:markup %)) (:markup-marker %)) xs)]
    (reduce
      (fn [ret yield]
        (reduce
          (fn [ret [k v]]
            (case k
              (:header :compatibility :reference :weight
               :experimental? :deprecated?)
              (assoc ret k v)

              :description
              (if (contains? ret :description)
                (update ret :description str "\n\n" v)
                (assoc ret :description v))

              :color
              (update ret :colors (fnil conj []) v)

              :parameter-or-modifier
              (update ret (if markup? :modifiers :parameters) (fnil conj [])
                      (if markup? (dissoc v :default-value) v))

              :continued-parameter-or-modifier-description
              (update ret (if markup? :modifiers :parameters) update-last :description str " " v)

              :continued-compatibility
              (update ret :compatibility str " " v)

              :continued-header
              (update ret :header str " " v)

              :continued-description
              (update ret :description str "\n" v)

              :markup
              (if (contains? ret :markup)
                (update ret :markup str " " v)
                (assoc ret :markup v))

              ;; else
              ret))
          ret
          (select-keys
            yield
            [:header :continued-header :description :continued-description
             :markup :markup-marker :color :reference
             :parameter-or-modifier :continued-parameter-or-modifier-description
             :weight :experimental? :deprecated?
             :compatibility :continued-compatibility?])))
      {}
      xs)))

(defn parse-lines
  "Returns a lazy collection of KSS documentation maps from lines of css."
  [coll]
  (->> {:remaining coll}
       (iterate #(-> % block-parser syntax-parser))
       (take-while #(not (contains? % :end)))
       (map :yield)
       (remove #(or (nil? %) (s/blank? (:text %))))
       (partition-by :block-ln)
       (pmap assemble-yields)))

(defn parse-string
  "Returns a lazy collection of KSS documentation maps from a css string."
  [s]
  (parse-lines (s/split s #"\n")))

(defn parse-stream
  "Returns a lazy collection of KSS documentation maps from a java.io.BufferedReader."
  [rdr]
  (parse-lines (line-seq rdr)))

(defn parse-file
  "Returns a collection of KSS documentation maps from java.io.File. Not lazy."
  [file]
  (let [source {:base (.getParent file) :name (.getName file) :path (.getPath file)}]
    (with-open [rdr (io/reader file)]
      (->> (parse-stream rdr)
           (pmap #(assoc % :source source))
           (doall)))))

(comment
  (require 'protocol55.kss.specs)
  (require 'protocol55.kss.specs.gen)
  (require '[clojure.spec.test.alpha :as st])
  (require '[clojure.spec.alpha :as sp])

  (parse-file (io/file "test/fixtures/property-header.less"))

  (st/instrument)
  (st/unstrument)

  (st/summarize-results (st/check `block-parser {:clojure.spec.test.check/opts {:num-tests 50 :max-size 50}}))
  (st/summarize-results (st/check `syntax-parser {:clojure.spec.test.check/opts {:num-tests 50 :max-size 50}}))

  (sp/exercise-fn `parse-lines)
  (sp/exercise-fn `parse-string))
