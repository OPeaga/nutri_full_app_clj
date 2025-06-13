;; handler
(ns api-nutri-app.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.util.response :refer [response]]
            [api-nutri-app.operacoes :as ops]))

(defroutes app-routes
           (GET "/" []
             (response {:mensagem "Bem-vindo à API Nutri App"}))

           (GET "/usuario/consulta" [] (response (ops/consultar_usuario)))

           (GET "/usuario/consulta/:id" [id] (response (ops/consultar_dados_usuario :id)))


           (POST "/usuario/cadastra" req
             (let [{:keys [altura peso idade sexo]} (:body req)]
               (response (ops/registrar_usuario altura peso idade sexo))))

           (POST "/alimento/cadastra" req
             (let [{:keys [alimento porcao data]} (:body req)]
               (response (ops/registrar_alimento alimento porcao data))))

           (POST "/exercicio/cadastra" req
             (let [{:keys [atividade duracao data]} (:body req)]
               (response (ops/registrar_exercicio atividade duracao data))))

           (GET "/extrato" [data-inicio data-fim]
             (response (ops/consultar_extrato_alimentos data-inicio data-fim)))

           (GET "/extrato/alimentos" [data-inicio data-fim]
             (response (ops/consultar_extrato_alimentos data-inicio data-fim)))

           (GET "/extrato/exercicios" [data-inicio data-fim]
             (response (ops/consultar_extrato_exercicios data-inicio data-fim)))

           ;(GET "/saldo" req ;; temos que ajeitar esse get tbm
           ;  (let [{:keys [data-inicio data-fim]} (:body req)
           ;        saldo (ops/calcular_balanco_calorico data-inicio data-fim) ]
           ;    (response {:saldo-calorico saldo})))

           (route/not-found (response {:erro "Rota não encontrada"})))

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})
      wrap-json-response
      (wrap-defaults api-defaults)))