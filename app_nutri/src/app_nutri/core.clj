;; core
(ns app-nutri.core
  (:require [clj-http.client :as http-client]
            [cheshire.core :as json])
  (:gen-class))

(def url-server "http://localhost:3000")

(defn formatador_strings [tipo mapa]
  (cond
    (= 1 tipo) (str "Id: " (:id mapa)
                    ", Altura (cm): " (:altura mapa)
                    ", Peso (kg): " (:peso mapa)
                    ", Idade: " (:idade mapa)
                    ", Sexo: " (:sexo mapa))

    (= 2 tipo) ("String Alimento")
    (= 3 tipo) ("String Exercicio")
    )
  )

(def formatador_strings_usuario (partial formatador_strings 1))

(defn post-json [url data]
  (let [response (http-client/post url
                                   {:body (json/generate-string data)
                                    :headers {"Content-Type" "application/json"}
                                    :content-type :json
                                    :accept :json})]
    (json/parse-string (:body response) true)))

(defn get-json [url]
  (let [resp (http-client/get url {:accept :json})]
    (json/parse-string (:body resp) true)))


(defn registrar-usuario []
  (println "Digite: altura(cm) peso(kg) idade sexo(M/F)")
  (let [altura (read)
        peso   (read)
        idade  (read)
        sexo   (read)
        resp (post-json (str url-server "/usuario/cadastra")
                        {:altura altura :peso peso :idade idade :sexo sexo})]
    (println "Usuario Criado:\n" (formatador_strings_usuario (last resp)))
    )
  )

(defn dados_usuario [id]
  (let [response (get-json (str url-server "/usuario/consulta/" id ))]
    (formatador_strings_usuario response)
    )
  )

(defn listar-alimentos-por-periodo []
  (read-line)
  (print "Digite a data inicial (dd/MM/yyyy): ")
  (flush)
  (let [data-inicio (read-line)]
    (print "Digite a data final (dd/MM/yyyy): ")
    (flush)
    (let [data-fim (read-line)
          url (str url-server
                   "/extrato/alimentos?"
                   "data-inicio=" data-inicio
                   "&data-fim="   data-fim)
          resp (get-json url)]
      (println "\n=== Alimentos Consumidos no Periodo ===")
      (doseq [alimento resp]
        (println alimento)))))



(defn listar-exercicios-por-periodo []
  (read-line)
  (print "Digite a data inicial (dd/MM/yyyy): ")
  (flush)
  (let [data-inicio (read-line)]
    (print "Digite a data final (dd/MM/yyyy): ")
    (flush)
    (let [data-fim (read-line)
          url (str url-server
                   "/extrato/exercicios?"
                   "data-inicio=" data-inicio
                   "&data-fim="   data-fim)
          resp (get-json url)]
      (println "\n=== Atividades Físicas no Período ===")
      (doseq [atividade resp]
        (println atividade)))))


(defn consultar-saldo-calorico []
  (println "Digite a data inicial (dd/MM/yyyy):")
  (let [ini (read-line)
        _   (println "Digite a data final (dd/MM/yyyy):")
        fim (read-line)
        resp (post-json
               (str url-server "/saldo")
               {:data-inicio ini
                :data-fim    fim})
        body (json/parse-string (:body resp) true)]
    (println "\n=== Saldo Calórico no Periodo ===")
    (println (:saldo-calorico body))))

(defn consultar-extrato-geral []
  (print "Digite a data inicial (dd/MM/yyyy): ")
  (flush)
  (let [data-inicio (read-line)]
    (print "Digite a data final (dd/MM/yyyy): ")
    (flush)
    (let [data-fim (read-line)
          url (str url-server
                   "/extrato?"
                   "data-inicio=" data-inicio
                   "&data-fim="   data-fim)
          resp (get-json url)]
      (println "\n=== Extrato de Alimentos e Exercícios ===")
      (doseq [item resp]
        (println item)))))


(defn registrar-alimento []
  (println "Digite: nome porcao(g) data(dd/MM/yyyy)")
  (let [nome   (read)
        porcao (read)
        data   (read-line)
        resp (post-json (str url-server "/alimento/cadastra")
                        {:alimento nome :porcao porcao :data data})]
    (println "Resposta:" (:body resp))))

(defn registrar-exercicio []
  (println "Digite: (atividade) (duracao(min))")
  (let [atividade (read)
        duracao   (read)
        data      (read-line)
        resp (post-json (str url-server "/exercicio/cadastra")
                        {:atividade atividade :duracao duracao :data data})]
    (println "Resposta:" (:body resp))))


(defn menu-exercicios-alimentos-geral []
  (println "\n==== MENU APP NUTRI - INTERACOES ALIMENTO E EXERCICIOS ==== ")
  (println "1. Registrar Consumo de Alimentos")
  (println "2. Registrar Atividade Fisica")
  (println "3. Listar Alimentos Consumidos por Periodo")
  (println "4. Listar Atividades Fisicas Realizadas por Periodo")
  (println "5. Consultar Saldo Calorico")
  (println "6. Consultar Extrato por Periodo")
  (println "7. Consultar Dados do Usuario")
  (println "8. Voltar ao menu anterior")
  (print   "Escolha: ") (flush)
  (let [op (read)]
    (cond
      (= op 1) (do (registrar-alimento) (recur))
      (= op 2) (do (registrar-exercicio) (recur))
      (= op 3) (do (listar-alimentos-por-periodo) (recur))
      (= op 4) (do (listar-exercicios-por-periodo) (recur))
      (= op 5) (do (consultar-saldo-calorico) (recur))
      (= op 6) (do (consultar-extrato-geral) (recur))
      (= op 7) (do (println "\n=== Dados do Usuario ===")
                   (println (dados_usuario 1))
                   (recur))
      (= op 8) (println "Retornando ao menu anterior.")
      :else
      (do (println "Opcao invalida, tente novamente.")
          (recur)))))



(defn listar-usuarios []
  (println "\n==== MENU APP NUTRI - LISTA DE USUARIOS ==== ")
  (let [usuarios (get-json (str url-server "/usuario/consulta"))]
    (mapv println (map #(formatador_strings_usuario %) usuarios))
    (println "Digite o 'id' do usuario escolhido ou '0' para voltar ao menu inicial\n")
    (let [entrada (read)]
      (cond
        (= entrada 0) nil
        (= entrada 1) (menu-exercicios-alimentos-geral)
        :else
        (do (println "Opcao Invalida") (recur))
        )
      )
    )
  )

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
      :else    (do (println "Opcao invalida.") (recur)))))

(defn -main [& _args]
  (println "Bem-vindos ao App Nutri ---------------------------------")
  (menu-usuario))