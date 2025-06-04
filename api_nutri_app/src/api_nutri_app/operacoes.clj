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


(defn converte_kg_pounds [peso_kg]
  (* (double peso_kg) 2.20462)
  )

(defn calcular_gasto_calorico [atividade duracao]
  (let [usuario (usuario/getUsuario_por_id 1)] ;; Trabalhamos com o usuário 1
    (if (not (nil? usuario))
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

            (if (or (nil? atividades) (empty? atividades))
              (throw (Exception. (str "Nenhuma atividade correspondente a '" atividade "' encontrada na API")))

              (let [primeira-atividade (first atividades)
                    calorias (:total_calories primeira-atividade)]
                {:success true
                 :data calorias
                 :status (:status response)})
              )
            )

          (catch Exception e
            {:success false
             :error (.getMessage e)
             :data nil})))
      "Usuário ainda não cadastrado")))


(defn registrar_alimento [alimento porcao data]
  (try
    (let [alimento_em_ingles (:data (traducao_direta_pt_en alimento))
          calorias_consumidas (calcular_calorias_por_porcao porcao alimento_em_ingles)]
      (alimento/registrar_alimento {:alimento (str porcao "g de " alimento)
                                    :calorias calorias_consumidas
                                    :data     data})
      "Alimento Registrado com sucesso")
    (catch Exception e
      "Erro ao registrar alimento")
    )
  )

(defn registrar_exercicio [atividade duracao data]
  (try
    (let [atividade_em_ingles (:data (traducao_direta_pt_en atividade))
          calorias_perdidas (:data (calcular_gasto_calorico atividade_em_ingles duracao))] ;; verificar quais atividades vamos dispor para selecionar no front
      (exercicio/registrar_exercicio {:exercicio (str atividade " por " duracao " minutos") :calorias (* -1.00 calorias_perdidas) :data data})
      "Exercício Registrado com Sucesso"
      )
    (catch Exception e
     (.getMessage e)
    )
  )
  )

(def lista_alimentos_temp                                   ;; Feito para se basear nas consultas de extrato sem fazer requisicoes
  [{:alimento "150g de arroz", :calorias 191.10000000000002, :data "25/05/2025"}
   {:alimento "200g de frango_grelhado", :calorias "Nenhum item encontrado", :data "25/05/2025"}
   {:alimento "80g de banana", :calorias 71.52000000000001, :data "26/05/2025"}
   {:alimento "50g de aveia", :calorias 184.15, :data "26/05/2025"}
   {:alimento "180g de salmao", :calorias 375.65999999999997, :data "27/05/2025"}
   {:alimento "100g de brocolis", :calorias 35.0, :data "27/05/2025"}
   {:alimento "120g de batata_doce", :calorias 93.0, :data "28/05/2025"}
   {:alimento "140g de ovos", :calorias 202.02, :data "28/05/2025"}
   {:alimento "170g de iogurte_grego", :calorias "Nenhum item encontrado", :data "29/05/2025"}
   {:alimento "90g de quinoa", :calorias 109.62, :data "29/05/2025"}
   {:alimento "60g de espinafre", :calorias 13.98, :data "30/05/2025"}
   {:alimento "160g de peito_peru", :calorias "Nenhum item encontrado", :data "30/05/2025"}
   {:alimento "200g de abacate", :calorias 334.4, :data "31/05/2025"}
   {:alimento "40g de castanha_para", :calorias "Nenhum item encontrado", :data "31/05/2025"}
   {:alimento "70g de cenoura", :calorias 23.799999999999997, :data "01/06/2025"}
   {:alimento "150g de atum", :calorias 199.95000000000002, :data "01/06/2025"}
   {:alimento "110g de manga", :calorias "Nenhum item encontrado", :data "02/06/2025"}
   {:alimento "130g de lentilha", :calorias 146.64000000000001, :data "02/06/2025"}
   {:alimento "85g de tomate", :calorias 15.469999999999999, :data "03/06/2025"}
   {:alimento "50g de amendoim", :calorias 289.25, :data "05/06/2025"}])


(defn consultar_extrato
  ([tipo data_ini data_fim])



  ([data_ini data_fim]
    (let [transacoes lista_alimentos_temp                                       ;; (conj (alimento/consultar_alimentos) (exercicio/consultar_exercicios))
          formater_data (java.time.format.DateTimeFormatter/ofPattern "dd/MM/yyyy")]
       (filter (fn [registro]
                ;;(.format (LocalDate/parse (:data registro)) formater_data)
                (and
                  (.isBefore (LocalDate/parse (:data registro) formater_data) (.plusDays (LocalDate/parse data_fim formater_data) 1))
                  (.isAfter (LocalDate/parse (:data registro) formater_data ) (.minusDays (LocalDate/parse data_ini formater_data) 1))
                  )
                ) transacoes)
       )

   )

  )

(def consultar_extrato_exercicios (consultar_extrato partial 2)) ;;Exercicios serão do tipo 2
(def consultar_extrato_alimentos (consultar_extrato partial 1)) ;;Alimentos serão do tipo 1





;(println (alimento/consultar_alimentos))
(println (consultar_extrato "29/05/2025" "03/06/2025"))
