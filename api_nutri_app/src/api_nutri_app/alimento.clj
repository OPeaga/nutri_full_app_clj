(ns api-nutri-app.alimento)

(def lista_alimento (atom []))

(defn registrar_alimento [alimento]
  (swap! lista_alimento conj alimento))

(defn consultar_lista []
  @lista_alimento)

(defn limpar_lista_alimentos []
  (reset! lista_alimento []))
