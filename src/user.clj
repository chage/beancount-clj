(ns user
  (:require
    [beancount.api]
    [beancount.core]
    [clojure.tools.namespace.repl :as tn]
    [malli.dev :as mdev]
    [malli.dev.pretty :as mpretty]))


(defn start
  []
  (tn/refresh-all)
  (mdev/start! {:report (mpretty/reporter)}))


(defn stop
  []
  (mdev/stop!))
