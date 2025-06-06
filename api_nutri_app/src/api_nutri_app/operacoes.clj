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

(def consultar_extrato_exercicios (partial consultar_extrato 2))
(def consultar_extrato_alimentos  (partial consultar_extrato 1))

(registrar_usuario 176 82 23 "M")
(def alimentos
  [{:alimento "150g de arroz", :calorias 191.10000000000002, :data "25/05/2025"}
   {:alimento "80g de banana", :calorias 71.52000000000001, :data "26/05/2025"}
   {:alimento "50g de aveia", :calorias 184.15, :data "26/05/2025"}
   {:alimento "180g de salmao", :calorias 375.65999999999997, :data "27/05/2025"}
   {:alimento "100g de brocolis", :calorias 35.0, :data "27/05/2025"}
   {:alimento "120g de batata_doce", :calorias 93.0, :data "28/05/2025"}
   {:alimento "140g de ovos", :calorias 202.02, :data "28/05/2025"}
   {:alimento "90g de quinoa", :calorias 109.62, :data "29/05/2025"}
   {:alimento "60g de espinafre", :calorias 13.98, :data "30/05/2025"}
   {:alimento "200g de abacate", :calorias 334.4, :data "31/05/2025"}
   {:alimento "70g de cenoura", :calorias 23.799999999999997, :data "01/06/2025"}
   {:alimento "150g de atum", :calorias 199.95000000000002, :data "01/06/2025"}
   {:alimento "130g de lentilha", :calorias 146.64000000000001, :data "02/06/2025"}
   {:alimento "85g de tomate", :calorias 15.469999999999999, :data "03/06/2025"}
   {:alimento "50g de amendoim", :calorias 289.25, :data "05/06/2025"}]
  )

(def exercicios
    [{:exercicio "corrida por 30 minutos", :calorias -240.0, :data "25/05/2025"}
     {:exercicio "musculacao por 45 minutos", :calorias -180.0, :data "25/05/2025"}
     {:exercicio "natacao por 60 minutos", :calorias -420.0, :data "26/05/2025"}
     {:exercicio "yoga por 40 minutos", :calorias -120.0, :data "26/05/2025"}
     {:exercicio "ciclismo por 90 minutos", :calorias -540.0, :data "27/05/2025"}
     {:exercicio "pilates por 50 minutos", :calorias -150.0, :data "27/05/2025"}
     {:exercicio "caminhada por 25 minutos", :calorias -100.0, :data "28/05/2025"}
     {:exercicio "crossfit por 35 minutos", :calorias -280.0, :data "28/05/2025"}
     {:exercicio "alongamento por 15 minutos", :calorias -45.0, :data "29/05/2025"}
     {:exercicio "boxe por 45 minutos", :calorias -360.0, :data "29/05/2025"}
     {:exercicio "futebol por 75 minutos", :calorias -525.0, :data "30/05/2025"}
     {:exercicio "tenis por 60 minutos", :calorias -300.0, :data "30/05/2025"}
     {:exercicio "danca por 40 minutos", :calorias -160.0, :data "31/05/2025"}
     {:exercicio "escalada por 80 minutos", :calorias -640.0, :data "31/05/2025"}
     {:exercicio "basquete por 55 minutos", :calorias -330.0, :data "01/06/2025"}
     {:exercicio "volei por 50 minutos", :calorias -200.0, :data "01/06/2025"}
     {:exercicio "spinning por 45 minutos", :calorias -315.0, :data "02/06/2025"}
     {:exercicio "jiu_jitsu por 70 minutos", :calorias -490.0, :data "02/06/2025"}
     {:exercicio "hidroginastica por 35 minutos", :calorias -140.0, :data "03/06/2025"}
     {:exercicio "skate por 40 minutos", :calorias -200.0, :data "05/06/2025"}])

(defn calcular_balanco_calorico [data_ini data_fim]
  (let [transacoes (concat (alimento/consultar_alimentos) (exercicio/consultar_exercicios))
        transacaoes_periodo (filter #(inInterval? (:data %) data_ini data_fim) transacoes)]
    (println transacoes)
    (println transacaoes_periodo)
    (format "%.2f cal" (reduce + (map #(:calorias %) transacaoes_periodo)))
    )
  )

(println (calcular_balanco_calorico "25/05/2025" "01/06/2025"))