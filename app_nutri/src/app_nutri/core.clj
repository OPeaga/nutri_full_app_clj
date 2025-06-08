(ns app-nutri.core
  (:require [clj-http.client :as	http-client]
            [cheshire.core :as json]
            )
  (:gen-class))

(def url-server "http://localhost:3000")

(defn menu_selecionar_usuario []
  (let [response (http-client/get (str url-server "/usuario/consulta"))]
    (if (= (:status response) 200)
      (let [body (:body response)
            altura (:altura body)
            peso (:peso body)
            idade (:idade body)
            sexo (:sexo body)
            data-registro (:data_registro body)]
        (println body)
        (println (str "Altura: " altura
                      ", Peso: " peso
                      ", Idade: " idade
                      ", Sexo: " sexo
                      ", Data de Registro: " data-registro)))
      (println "Erro: não foi possível consultar o usuário."))))


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
          (= 2 entrada) (do (menu_selecionar_usuario) (recur entrada) )
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
