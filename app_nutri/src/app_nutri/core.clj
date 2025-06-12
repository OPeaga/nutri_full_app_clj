;; core
(ns app-nutri.core
  (:require [clj-http.client :as http-client]
            [cheshire.core :as json])
  (:gen-class))

(def url-server "http://localhost:3000")

(defn formatador_strings [tipo mapa]
  (cond
    (= 1 tipo) (str "Id: " (:id mapa)
                     ", Altura: " (:altura mapa)
                     ", Peso: " (:peso mapa)
                     ", Idade: " (:idade mapa)
                     ", Sexo: " (:sexo mapa))

    (= 2 tipo) ("String Alimento")
    (= 3 tipo) ("String Exercicio")
    )
  )

(def formatador_strings_usuario (partial formatador_strings 1))

(defn post-json [url data]
  (http-client/post url
                    {:body (json/generate-string data)
                     :headers {"Content-Type" "application/json"}
                     :content-type :json
                     :accept :json}))

(defn get-json [url]
  (let [resp (http-client/get url {:accept :json})]
    (json/parse-string (:body resp) true)))

;; todos os menus funcionais, como o ambiente ta rodando no teu, eu n consegui rodar os endpoints -> que história e essa do bicho tá funcionando se tu não testou fi de uma mãe
;; mas acredito que estejam todos funcionando já que eu segui o padrão do seu que está funcionando

(defn registrar-usuario []
  (println "Digite: altura(cm) peso(kg) idade sexo(M/F)")
  (let [altura (read)
        peso   (read)
        idade  (read)
        sexo   (read)
        resp (post-json (str url-server "/usuario/cadastra")
                        {:altura altura :peso peso :idade idade :sexo sexo})]
    (println "Usuario Criado:" (formatador_strings_usuario (first (:body resp))))
    ))

(defn listar-usuarios []
  (println "\n==== MENU APP NUTRI - LISTA DE USUARIOS ==== ")
  (let [usuarios (get-json (str url-server "/usuario/consulta"))]
    (mapv println (map #(formatador_strings_usuario %) usuarios))
    (println "Digite o 'id' do usuario escolhido ou '0' para voltar ao menu inicial")
    (let [entrada (read)]
      (cond
        (= entrada 0) nil
        (= entrada 1) (menu-exerciccios-alimentos-geral)
        :else
          (do (println "Opção Inválida") (recur))
        )
      )
    )
  )

(defn menu-exercicios-alimentos-geral [])

(defn registrar-alimento []
  (println "Digite: nome porção(g) data(dd/MM/yyyy)")
  (let [nome   (read)
        porcao (read)
        data   (read)
        resp (post-json (str url-server "/alimento")
                        {:alimento nome :porcao porcao :data data})]
    (println "Resposta:" (:body resp))))

(defn registrar-exercicio []
  (println "Digite: (atividade) (duracao(min))")
  (let [atividade (read)
        duracao   (read)
        data      (read)
        resp (post-json (str url-server "/exercicio")
                        {:atividade atividade :duracao duracao :data data})]
    (println "Resposta:" (:body resp))))


(defn menu-usuario []
  (println "\n==== MENU APP NUTRI - USUARIO ==== ")
  (println "1. Registrar novo usuario")
  (println "2. Selecionar usuario")
  (println "3. Sair")
  (print "Escolha: ") (flush)
  (let [op (read)]
    (cond
      (= op 1) (do (registrar-usuario) (recur))
      (= op 2) (do (listar-usuarios)   (recur))
      (= op 3) (println "Encerrando o App Nutri.")
      :else    (do (println "Opção invalida.") (recur)))))

(defn -main [& _args]
  (println "Bem-vindos ao App Nutri ---------------------------------")
  (menu-usuario))