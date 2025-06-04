(ns api-nutri-app.operacoes
  (:require [clj-http.client :as http-client]
            [api-nutri-app.usuario :as usuario]
            [api-nutri-app.alimento :as alimento]
            [api-nutri-app.exercicio :as exercicio])
  (:import [java.time LocalDateTime LocalDate]
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
(def api-url-exercicio "https://calories-burned-by-api-ninjas.p.rapidapi.com/v1/caloriesburned")


(defn traducao_direta_pt_en [palavra_em_pt]
  (try
    (let [response (http-client/post
                     api-url-traducao
                     {:headers      {:x-rapidapi-key  traducao-api-key
                                     :x-rapidapi-host "google-translate113.p.rapidapi.com"}
                      :form-params  {:text palavra_em_pt
                                     :from "pt"
                                     :to   "en"}
                      :as           :json})]
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
       :data    nil})
    )
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
            (throw (Exception. "Nenhum item encontrado, provável tradução desconhecida"))
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

(defn converte_kg_pounds [peso_kg]
  (* (double peso_kg) 2.20462)
  )

(defn calcular_gasto_calorico [atividade duracao]
  (let [usuario (usuario/getUsuario_por_id 1)] ;; Trabalhamos com o usuário 1
    (if usuario
      (let [peso_pounds (converte_kg_pounds (:peso usuario))]
        (try
          (let [response (http-client/get api-url-exercicio
                                          {:headers
                                           {:x-rapidapi-key traducao-api-key
                                            :x-rapidapi-host "calories-burned-by-api-ninjas.p.rapidapi.com"}
                                           :query-params {:activity atividade
                                                          :duration duracao
                                                          :weight peso_pounds}
                                           :as :json})
                atividades (:body response)]

            (if
              (empty? atividades )
              (throw (Exception. (str "Nenhuma atividade correspondente a '" atividade "' encontrada na API")))
              (do
                (let [primeira-atividade (first atividades)
                        calorias (:total_calories primeira-atividade)]
                    {:success true
                     :data calorias
                     :status (:status response)}
                    )
                  )
              )
            )

          (catch Exception e
            {:success false
             :error (.getMessage e)
             :data nil}
            )
          )
        )
      (throw (Exception. "Usuário ainda não cadastrado"))
      )
    )
  )


(defn registrar_alimento [alimento porcao data]
  (try
    (let [alimento_em_ingles (:data (traducao_direta_pt_en alimento))
          calorias_consumidas (calcular_calorias_por_porcao porcao alimento_em_ingles)]
      (alimento/registrar_alimento {:alimento (str porcao "g de " alimento)
                                    :calorias calorias_consumidas
                                    :data     data})
      "Alimento Registrado com sucesso")
    (catch Exception e
      (.getMessage e)
      )
    )
  )

(defn registrar_exercicio [atividade duracao data]
  (try
    (let [atividade_em_ingles (:data (traducao_direta_pt_en atividade))
          resposta (calcular_gasto_calorico atividade_em_ingles duracao )]

      (if (:success resposta)
        (do
          (let [calorias_perdidas (:data resposta)]
            (exercicio/registrar_exercicio
              {:exercicio (str atividade " por " duracao " minutos")
               :calorias (* -1.0 (double calorias_perdidas))
               :data data})
            )
          "Exercicio cadastrado com sucesso"
          )
        (throw (Exception. (str (:error resposta))))
        )
      )

    (catch Exception e
      (str "Erro ao registrar exercício: " (.getMessage e))
      )
    )
  )


(defn inInterval? [data_ref data_ini data_fim]
  (let [formatter_data (DateTimeFormatter/ofPattern "dd/MM/yyyy")]
    (and
      (.isBefore (LocalDate/parse data_ref formatter_data) (.plusDays (LocalDate/parse data_fim formatter_data) 1))
      (.isAfter (LocalDate/parse data_ref formatter_data ) (.minusDays (LocalDate/parse data_ini formatter_data) 1))
      )
    )
  )

(defn sortExtrato [registros]
  (let [formatter_data (DateTimeFormatter/ofPattern "dd/MM/yyyy")]
    (sort-by #(LocalDate/parse (:data %) formatter_data) registros)))

(defn consultar_extrato
  ([tipo data_ini data_fim]
   (cond
     (= tipo 1) (sortExtrato (filter #(inInterval? (:data %) data_ini data_fim) (alimento/consultar_alimentos)))
     (= tipo 2) (sortExtrato (filter #(inInterval? (:data %) data_ini data_fim) (exercicio/consultar_exercicios)))
     :else "Comando desconhecido"
     )
   )
  ([data_ini data_fim]
    (let [transacoes (concat (alimento/consultar_alimentos) (exercicio/consultar_exercicios))]
       (sortExtrato (filter #(inInterval? (:data %) data_ini data_fim) transacoes))
   )
   )
  )

;(defn calcular_balanco_calorico [data_ini data_fim])

(def consultar_extrato_exercicios (partial consultar_extrato 2))
(def consultar_extrato_alimentos  (partial consultar_extrato 1))

(println (registrar_usuario 176 82 23 "M") )
(println (registrar_alimento "arroz" 150 "25/05/2025"))
(println (registrar_alimento "frango_grelhado" 200 "25/05/2025"))
(println (registrar_alimento "banana" 80 "26/05/2025"))
(println (registrar_alimento "aveia" 50 "26/05/2025"))
(println (registrar_alimento "salmao" 180 "27/05/2025"))
(println (registrar_alimento "brocolis" 100 "27/05/2025"))
(println (registrar_alimento "batata_doce" 120 "28/05/2025"))
(println (registrar_alimento "ovos" 140 "28/05/2025"))
(println (registrar_alimento "iogurte_grego" 170 "29/05/2025"))
(println (registrar_alimento "quinoa" 90 "29/05/2025"))
(println (registrar_alimento "espinafre" 60 "30/05/2025"))
(println (registrar_alimento "peito_peru" 160 "30/05/2025"))
(println (registrar_alimento "abacate" 200 "31/05/2025"))
(println (registrar_alimento "castanha_para" 40 "31/05/2025"))
(println (registrar_alimento "cenoura" 70 "01/06/2025"))
(println (registrar_alimento "atum" 150 "01/06/2025"))
(println (registrar_alimento "manga" 110 "02/06/2025"))
(println (registrar_alimento "lentilha" 130 "02/06/2025"))
(println (registrar_alimento "tomate" 85 "03/06/2025"))
(println (registrar_alimento "amendoim" 50 "05/06/2025"))
(println (registrar_exercicio "corrida" 30 "25/05/2025"))
(println (registrar_exercicio "musculacao" 45 "25/05/2025"))
(println (registrar_exercicio "natacao" 60 "26/05/2025"))
(println (registrar_exercicio "yoga" 40 "26/05/2025"))
(println (registrar_exercicio "ciclismo" 90 "27/05/2025"))
(println (registrar_exercicio "pilates" 50 "27/05/2025"))
(println (registrar_exercicio "caminhada" 25 "28/05/2025"))
(println (registrar_exercicio "crossfit" 35 "28/05/2025"))
(println (registrar_exercicio "alongamento" 15 "29/05/2025"))
(println (registrar_exercicio "boxe" 45 "29/05/2025"))
(println (registrar_exercicio "futebol" 75 "30/05/2025"))
(println (registrar_exercicio "tenis" 60 "30/05/2025"))
(println (registrar_exercicio "danca" 40 "31/05/2025"))
(println (registrar_exercicio "escalada" 80 "31/05/2025"))
(println (registrar_exercicio "basquete" 55 "01/06/2025"))
(println (registrar_exercicio "volei" 50 "01/06/2025"))
(println (registrar_exercicio "spinning" 45 "02/06/2025"))
(println (registrar_exercicio "jiu_jitsu" 70 "02/06/2025"))
(println (registrar_exercicio "hidroginastica" 35 "03/06/2025"))
(println (registrar_exercicio "skate" 40 "05/06/2025"))




(println (consultar_extrato "29/05/2025" "03/06/2025"))
(println "-----------------------------\n")
(println (consultar_extrato_exercicios "29/05/2025" "03/06/2025"))
(println "-----------------------------\n")
(print (consultar_extrato_alimentos "29/05/2025" "03/06/2025"))