(ns beancount.core
  (:require
    [clojure.string :as cstr]
    [clojure.walk :refer [keywordize-keys]]
    [libpython-clj2.python :as py]
    [libpython-clj2.require :refer [require-python]]
    [tick.core :as t]))


(require-python '[beancount :as beancount]
                '[beancount.loader :as bl])


(defmulti get-entry-attrs
  (fn [x]
    (when (= :pyobject (type x))
      (.get_python_type x))))


(defmethod get-entry-attrs :default [_] nil)


(defmethod get-entry-attrs :open
  [_]
  ["meta" "date" "account" "booking" "currencies"])


(defmethod get-entry-attrs :transaction
  [_]
  ["meta" "date" "flag" "payee" "narration" "postings"])


(defmethod get-entry-attrs :posting
  [_]
  ["meta" "flag" "account" "units" "cost" "price"])


(defmethod get-entry-attrs :amount
  [_]
  ["number" "currency"])


(defmethod get-entry-attrs :custom
  [_]
  ["meta" "date" "type" "values"])


(defmethod get-entry-attrs :value-type
  [_]
  ["value"]) ;; dtype


(declare parse-entry)


(defn- zipmap-entry-attrs
  [entry]
  (let [entry-type (.get_python_type entry)
        entry-attrs (get-entry-attrs entry)
        entry-parse-fn (fn [x] (parse-entry (py/get-attr entry x)))]
    (-> entry-attrs
        (zipmap (map entry-parse-fn entry-attrs))
        keywordize-keys
        (assoc :beancount/type entry-type))))


(defmulti parse-entry
  (fn [x]
    (when (= :pyobject (type x))
      (.get_python_type x))))


(defmethod parse-entry :default
  [entry]
  entry)


(defmethod parse-entry :open
  [entry]
  (zipmap-entry-attrs entry))


(defmethod parse-entry :transaction
  [entry]
  (zipmap-entry-attrs entry))


(defmethod parse-entry :posting
  [entry]
  (zipmap-entry-attrs entry))


(defmethod parse-entry :list
  [entry]
  (mapv parse-entry entry))


(defmethod parse-entry :dict
  [entry]
  (assoc
    (into {} entry)
    :beancount/type (.get_python_type entry)))


(defmethod parse-entry :date
  [entry]
  (t/date (py/call-attr entry "isoformat")))


(defmethod parse-entry :amount
  [entry]
  (zipmap-entry-attrs entry))


(defmethod parse-entry :custom
  [entry]
  (zipmap-entry-attrs entry))


(defmethod parse-entry :value-type
  [entry]
  (zipmap-entry-attrs entry))


(defmulti ->file :beancount/type)


(defmethod ->file :default
  [entry]
  (str entry))


(defmethod ->file :beancount
  [entry]
  (mapv #(->file %)
        (concat
          (:open entry)
          (:transaction entry)
          (:custom entry))))


(defmethod ->file :open
  [{:keys [meta date account currencies booking]
    :as _open}]
  (cond-> (format "%s open %s" date account)
    (not-empty currencies) (str " " (cstr/join "," (mapv str currencies)))
    booking (str " " booking)))


(defmethod ->file :transaction
  [{:keys [meta date flag payee narration postings]
    :or {flag "*"}
    :as _transaction}]
  (str (cstr/join "\n" (concat
                         [(cstr/join " " (cond-> [date]
                                           flag (conj flag)
                                           payee (conj (format "\"%s\"" payee))
                                           narration (conj (format "\"%s\"" narration))))]
                         (->file meta)
                         (mapv ->file postings)))
       "\n"))


(defmethod ->file :posting
  [{:keys [_meta account units]
    :as _posting}]
  (format "  %s %s" account (->file units)))


(defmethod ->file :dict
  [dict]
  (let [dict' (dissoc dict :beancount/type :filename :lineno :__tolerances__)]
    (mapv #(format "  %s: \"%s\"" (name %) (get dict' % ""))
          (keys dict'))))


(defmethod ->file :amount
  [{:keys [number currency]
    :as _amount}]
  (format "%s %s" number currency))


(defmethod ->file :custom
  [{:keys [date type values]
    :as _custom}]
  (let [account (->file (first values))
        subtype (->file (second values))
        currency (->file (nth values 2))]
    (format "%s \"%s\" %s \"%s\" %s"
            date type account subtype currency)))


(defmethod ->file :value-type
  [{:keys [value]
    :as _value-type}]
  (->file value))


;; ref: https://github.com/beancount/fava/blob/main/src/fava/core/__init__.py#L281
(defn load-beancount
  [filename]
  (-> (bl/load_file filename)
      first))


;; --------------- Usage ---------------


(comment
  (-> (bl/load_file "test.beancount")
      (nth 2)
      (#(into {} %))
      keywordize-keys)

  ;; --------------- developing helper ---------------
  (defn loop-in
    [records]
    (loop [records records
           result []]
      (if (empty? records)
        result
        (recur (rest records)
               (conj result (first records))))))

  (require '[clojure.pprint :as pp]
           '[clojure.reflect :as reflect])
  (defn inspect-object
    [object]
    (some->> object
             reflect/reflect
             :members
             (sort-by :name)
             (pp/print-table [:name :flags :parameter-types :return-type]))))
