(ns brdloush.zonky-homework.core
  (:gen-class)
  (:require [clj-http.lite.client :as client]
            [clojure.data.json :as json]
            [clojure.pprint :refer [print-table]]
            [taoensso.timbre :refer [info debug]]
            [clojure.string :as str])
  (:import [java.util TimerTask Timer Date Locale]
           [java.text SimpleDateFormat NumberFormat]
           [java.time ZonedDateTime]
           [java.time.format DateTimeFormatter]))

(def page-size 10)
(def max-pages-to-seek 3)
(def check-time-interval-ms 5000)
(defonce task-atom (atom nil))

(defn zoned-date-time->iso-str [zdt]
  (.format zdt DateTimeFormatter/ISO_OFFSET_DATE_TIME))

(defn format-czech-ccy [n]
  (.format (NumberFormat/getCurrencyInstance (Locale. "cs" "CZ")) n))

(defn parse-and-reformat-str-dt [str-dt]
  (let [d (Date/from (.toInstant (ZonedDateTime/parse str-dt)))]
    (.format (SimpleDateFormat. "yyyy/dd/MM HH:mm:ss") d)))

(defn schedule-at-interval! [ms-interval f]
  (let [task (proxy [TimerTask] []
               (run [] (f)))]
    (.scheduleAtFixedRate (Timer.) task (Date.) ms-interval)
    task))

(defn fetch-page-descsorted-by-pubdate! [page-num page-size max-date-published-str]
  (debug "getting marketplace loans page #" page-num)
  (-> (client/get "https://api.zonky.cz/loans/marketplace"
                  {:headers {"X-Page" (str page-num)
                             "X-Size" (str page-size)
                             "X-Order" "-datePublished"
                             "User-Agent" "Zonky-homework/1.0.0-snapshot"}
                   :query-params {"datePublished__lte" max-date-published-str}})
      :body
      (json/read-json true)))

(defn newest-loans-before-loan-id! [loan-id page-size max-pages-to-seek]
  (let [max-date-published-str (zoned-date-time->iso-str (ZonedDateTime/now))]
    (transduce
     (comp
      (mapcat #(fetch-page-descsorted-by-pubdate! % page-size max-date-published-str))
      (take-while #(not= (:id %) loan-id))
      (map #(select-keys % [:datePublished :id :name :purpose :amount])))
     conj
     (range max-pages-to-seek))))

(defn print-loans! [loans]
  (let [printable-loans (->> loans
                             (map (fn [loan]
                                    (-> loan
                                        (update :datePublished parse-and-reformat-str-dt)
                                        (update :amount format-czech-ccy)
                                        (update :name str/trim)))))]
    (info "Found" (count loans) "new loans:")
    (info (with-out-str (print-table printable-loans)))))

(defn check-new-orders!
  [{:keys [most-recent-seen-loan-id] :as _check-status}]
  (let [new-loans (newest-loans-before-loan-id!
                   most-recent-seen-loan-id
                   page-size
                   max-pages-to-seek)
        new-most-recent-seen-loan-id (or (-> new-loans first :id) most-recent-seen-loan-id)]
    (if (not-empty new-loans)
      (print-loans! new-loans)
      (info "No new loans found"))
    {:most-recent-seen-loan-id new-most-recent-seen-loan-id}))

(defn -main [& _args]
  (info "Zonky homework app started")
  (let [check-status-atom (atom {})
        run-check-fn (fn [] (reset! check-status-atom (check-new-orders! @check-status-atom)))]
    (reset!
     task-atom
     (schedule-at-interval! check-time-interval-ms run-check-fn))))

(comment
  (-main)
  (when-let [task @task-atom] (.cancel task)))