(ns api-nutri-app.usuario)

(def lista_usuarios (atom []))

(defn limpar_lista []
  (reset! lista_usuarios [])
  )

(defn registrar_usuario [usuario]
  (swap! lista_usuarios conj usuario))

(defn consultar_usuarios []
  @lista_usuarios)
