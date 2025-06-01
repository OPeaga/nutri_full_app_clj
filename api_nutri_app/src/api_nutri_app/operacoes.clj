(ns api-nutri-app.operacoes
  (:require [clj-http.client :as http-client]
            [api-nutri-app.usuario :as usuario]
            [api-nutri-app.alimento :as alimento])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter])
  )

(def alimento-api-key (System/getenv "CALORIE_NINJAS_API_KEY")) ;; Transformei a api-key numa variavel de ambiente no pc
(def traducao-api-key (System/getenv "TRANSLATE_API_KEY"))

(when (nil? alimento-api-key)
  (throw (Exception. "CALORIE_NINJAS_API_KEY variavel de ambiente não definida!")))

(when (nil? traducao-api-key)
  (throw (Exception. "TRANSLATE_API_KEY variavel de ambiente não definida!")))

(def api-url-alimento "https://api.calorieninjas.com/v1/nutrition?query=")
(def api-url-traducao "https://google-translate113.p.rapidapi.com/api/v1/translator/text")

(defn traducao_direta_pt_en [palavra_em_pt]
  (try
    (let [response (http-client/post
                     api-url-traducao
                     {:headers      {:x-rapidapi-key  "babe4338b3msh09ac1243ab63506p1c7aa3jsn4d927b873ca0"
                                     :x-rapidapi-host "google-translate113.p.rapidapi.com"}
                      :form-params  {:text palavra_em_pt
                                     :from "pt"
                                     :to   "en"}
                      :as           :json
                      }
                     )]
      {:success true
       :data (get-in response [:body :trans])
       :status (:status response)}

      )
    (catch Exception e
      {:success false
       :error   (.getMessage e)
       :data    nil}))
  )


(defn consultar_nutricao [query]
  (try
    (let [response (http-client/get
                     (str api-url-alimento query)
                     {:headers {"X-Api-Key" alimento-api-key}
                      :as      :json})]

      {:success true
       :data    (:body response)
       :status  (:status response)}

      )
    (catch Exception e
      {:success false
       :error   (.getMessage e)
       :data    nil}))
  )

(defn calcular_calorias_por_porcao [porcao alimento]
  (let [resposta (consultar_nutricao alimento)]
    (if (:success resposta)
      (let [items (get-in resposta [:data :items])
            item (first items)]
          (if item
            (let [porcao_referencia (:serving_size_g item)
                  calorias_referencia (:calories item)
                  calorias_consumidas (* (double (/ porcao porcao_referencia) ) calorias_referencia )]

              calorias_consumidas
              )
            "Nenhum item encontrado"
            )
        )
      )
    )
  )

(defn registrar_usuario [altura peso idade sexo]
  (let [usuarios (usuario/consultar_usuarios)
        id_maximo (if (empty? usuarios)
                 0
                 (apply max (map :id usuarios)))
        novo_id (inc id_maximo)
        data_atual_formatada (.format (LocalDateTime/now) (DateTimeFormatter/ofPattern "dd/MM/yyyy"))]
    (usuario/registrar_usuario {:id novo_id
                                :altura altura
                                :peso peso
                                :idade idade
                                :sexo sexo
                                :data_registro data_atual_formatada})))


(defn registrar_alimento [alimento porcao]
  (let [alimento_em_ingles (:data (traducao_direta_pt_en alimento))
        calorias_consumidas (calcular_calorias_por_porcao porcao alimento_em_ingles)
        data_atual_formatada (.format (LocalDateTime/now) (DateTimeFormatter/ofPattern "dd/MM/yyyy"))]
    (alimento/registrar_alimento {:alimento (str porcao "g de " alimento) :calorias calorias_consumidas :data data_atual_formatada})
    )
  )

;;(def pt "arroz")
;;(registrar_alimento pt 150 )
;;(println (alimento/consultar_lista))