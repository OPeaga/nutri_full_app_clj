(ns api-nutri-app.usuario)

(def lista_usuarios (atom []))

(defn limpar_lista_usuarios []
  (reset! lista_usuarios [])
  )

(defn registrar_usuario [usuario]
  (swap! lista_usuarios conj usuario)
  )

(defn consultar_usuarios []
  @lista_usuarios)

(defn getUsuario_por_id [id]
 (first (filter  #(= (:id %) id)  (consultar_usuarios)))
  )
