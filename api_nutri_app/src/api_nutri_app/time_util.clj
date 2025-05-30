(ns api-nutri-app.time-util
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter])
  )


(defn agora []
  (LocalDateTime/now))

(defn formata-data-string [data]
  (let [fmt (DateTimeFormatter/ofPattern "dd/MM/yyyy HH:mm")]
    (.format data fmt)))

   ;; Remove the parentheses around data

(defn formata-data-objto [data-str]
  (let [fmt (DateTimeFormatter/ofPattern "dd/MM/yyyy HH:mm")]
    (LocalDateTime/parse data-str fmt)))

