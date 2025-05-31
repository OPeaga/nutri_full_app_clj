(ns api-nutri-app.operacoes
  (:require [clj-http.client :as http-client]
            [api-nutri-app.usuario :as usuario]
            [api-nutri-app.alimento :as alimento])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter])
  )

(def alimento-api-key (System/getenv "CALORIE_NINJAS_API_KEY")) ;; Transformei a api-key numa variavel de ambiente no pc

(when (nil? alimento-api-key)
  (throw (Exception. "CALORIE_NINJAS_API_KEY variavel de ambiente não definida!")))

(def api-url "https://api.calorieninjas.com/v1/nutrition?query=")

(defn consultar_nutricao [query]
  (try
    (let [response (http-client/get
                     (str api-url query)
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
              ;;(str porcao "g de " alimento " Total de " calorias_consumidas " cal")
              calorias_consumidas
              )
            "Nenhum item encontrado"
            )
        )
      )
    )
  )

(defn operacao_registrar_usuario [altura peso idade sexo]
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

(defn operacao_registra_alimento [alimento porcao]
  (let [calorias_consumidas (calcular_calorias_por_porcao alimento porcao)
        data_atual_formatada (.format (LocalDateTime/now) (DateTimeFormatter/ofPattern "dd/MM/yyyy"))]
    (alimento/regi)
    )
  )

