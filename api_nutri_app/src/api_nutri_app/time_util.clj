(ns api-nutri-app.time-util)                              ;; Arquivo para manusear datas
(:import [java.time LocalDateTime LocalDate LocalTime]
         [java.time.format DateTimeFormatter])


(defn now []
  (LocalDateTime/now))

(defn format-date [data]
  (let [fmt (DateTimeFormatter/ofPattern "dd-mm-yyyy hh:mm:ss")]
    (.format (data) fmt))
  )