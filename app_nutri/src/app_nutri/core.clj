(ns app-nutri.core
  (:require [clj-http.client :as	http-client]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            )
  (:gen-class))

(def url-server "http://localhost:3000")

(defn menu_mostrar_usuarios []
  (let [response (http-client/get (str url-server "/usuario/consulta"))]
    (if (= (:status response) 200)
      (let [body (edn/read-string (:body response))]
        (mapv println (map (fn [a] (str "Id: " (:id a) " Altura: " (:altura a) " Peso: " (:peso a) " Idade: " (:idade a) " Sexo: " (:sexo a) )) body))
        (let [entrada (read)]
          (println "digite o id do usuário")
          ;chamar menus de alimento e exercicio e consultas gerais
          )
        )
      (println "Erro: não foi possível consultar o usuário.")))
  )


(defn menu_registrar_usuario []
  (println "Digite : (altura em cm) (peso em kg) (idade) (M para Masculino e F para Feminino)")
  (let [altura (read)
        peso (read)
        idade (read)
        sexo (read)
        json-data (json/generate-string {:altura altura :peso peso :idade idade :sexo sexo})]
    (http-client/post (str url-server "/usuario/cadastro")
                      {:body json-data
                       :headers {"Content-Type" "application/json"}
                       :content-type :json
                       :accept :json})
    )
  )

(defn menu_usuario [entrada_param]
  (println "Digite uma opção : \n1.Registrar Novo Usuario\n2.Selecionar Usuário\n3.Encerrar Aplicativo")
  (if (= 3 entrada_param)
    nil
    (do
      (let [entrada (read)]
        (cond
          (= 1 entrada) (do (menu_registrar_usuario) (recur entrada) )
          (= 2 entrada) (do (menu_mostrar_usuarios) (recur entrada) )
          )
        )
      )
    )
  )


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Bem-vindos ao App Nutri ---------------------------------")
  (menu_usuario 0)
  )
