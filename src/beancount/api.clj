(ns beancount.api
  (:require
    [beancount.core :as bc]
    [tick.core :as t]
    [tick.protocols :as t.p]))


;; --------------- API ---------------
(defn save-beancount
  {:malli/schema [:=> [:cat
                       :string
                       [:vector :any]
                       [:map
                        [:beancount-types [:vector [:enum [:open :transaction :custom]]]]]]
                  :nil]}
  [filename entries & {beancount-types :beancount-types
                       :or {beancount-types [:open :transaction :custom]}}]
  ;; TODO: validate before save
  (run! (fn [beancount-type]
          (spit (format "%s-%s.beancount" filename (name beancount-type))
                (with-out-str
                  (mapv #(println (bc/->file %))
                        (sort-by #(format "%s %s"
                                          (get % :date "9999-12-31")
                                          (get-in % [:meta :time] "00:00:00"))
                                 (get entries beancount-type))))))
        beancount-types))


(defn load-beancount
  [filename]
  (bc/load-beancount filename))


;; https://github.com/beancount/fava/blob/main/src/fava/core/group_entries.py#L52
(defn parse-entries
  {:malli/schema [:=> [:cat [:sequential
                             {:description "A list of pyobject"}
                             :any]]
                  [:map
                   [:balance {:optional true} :any]
                   [:close {:optional true} :any]
                   [:commodity {:optional true} :any]
                   [:custom {:optional true} :any]
                   [:document {:optional true} :any]
                   [:event {:optional true} :any]
                   [:note {:optional true} :any]
                   [:open {:optional true} :any]
                   [:pad {:optional true} :any]
                   [:price {:optional true} :any]
                   [:query {:optional true} :any]
                   [:transaction {:optional true} :any]]]}
  [records]
  ;; ref: https://github.com/beancount/fava/blob/main/src/fava/core/__init__.py#L299
  ;; ref: https://github.com/beancount/fava/blob/main/src/fava/core/group_entries.py#L52
  (let [grouped-entries (group-by #(.get_python_type %) records)]
    (into {:beancount/type :beancount}
          (map (fn [[entry-type entries]]
                 {entry-type (map bc/parse-entry entries)})
               grouped-entries))))


(defn stats
  [entries]
  (into {}
        (map (fn [entry-type]
               (let [entry-list (get entries entry-type)
                     filename-list (->> entry-list
                                        (map #(get-in % [:meta :filename]))
                                        distinct)]
                 {entry-type {:count (count entry-list)
                              :filename-list filename-list}}))
             (keys entries))))

(defn generate-entry
  [{:keys [created-at from to number currency note payee narration]
    :or {created-at (t/now)
         number 0
         currency "TWD"}}]
  (if (or (empty? from) (empty? to))
    (throw (Exception. "Lack of Account"))
    (let [datetime (if (string? created-at)
                     (t.p/parse created-at)
                     created-at)
          the-date (t/date (t/format (t/formatter "yyyy-MM-dd") datetime))
          the-time (t/format (t/formatter "HH:mm:ss") datetime)]
      {:beancount/type :transaction
       :date the-date
       :meta (cond-> {:beancount/type :dict
                      :time the-time}
               note (conj {:note note}))
       :payee payee
       :narration narration
       :postings [{:beancount/type :posting :account from
                   :units {:beancount/type :amount :number (* -1 number) :currency currency}}
                  {:beancount/type :posting :account to
                   :units {:beancount/type :amount :number number :currency currency}}]})))


;; FIXME: useless?
(defn transaction->file-entry
  [entry]
  (bc/->file (generate-entry entry)))


;; --------------- Usage ---------------


(comment
  (def test-records
    (load-beancount "test.beancount"))

  (println (transaction->file-entry {:created-at "2023-10-23 13:13:25"
                                     :from "Assets:Test"
                                     :to "Expenses:Test"
                                     :number 123.0
                                     :narration "description ..."}))

  (save-beancount (format "tmp-%s" (random-uuid))
                  (parse-entries test-records)
                  {:beancount-types [:transaction]})

  (def cached-entries
    (-> (load-beancount "test.beancount")
        parse-entries
        delay))

  ;; --------------- developing helper ---------------
  (save-beancount (str "tmp-" (t/date)) @cached-entries
                  {:beancount-types [:transaction]})

  (stats @cached-entries)
  (ns-unmap *ns* 'bc/->file))
