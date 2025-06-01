(ns api-nutri-app.exercicio)

(def lista_exercicios (atom []))

(defn limpar_lista_exercicios []
  (reset! lista_exercicios [])
  )

(defn registrar_exercicio [exercicio]
  (swap! lista_exercicios conj exercicio))

(defn consultar_exercicios []
  @lista_exercicios)

