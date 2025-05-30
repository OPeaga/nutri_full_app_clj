(ns api-nutri-app.usuario
  (:require [api-nutri-app.time-util :as time-util])
  )

(def lista_usuarios (atom []))

(defn limpar_lista []
  (reset! lista_usuarios [])
  )

(defn registrar_usuario [usuario]
  (swap! lista_usuarios conj usuario))

(defn consultar_usuarios []
  @lista_usuarios)


;;(registrar_usuario {:id 1, :altura 176, :peso 75, :idade 23, :sexo "M", :data_cadastro (time-util/formata-data-string (java.time.LocalDateTime/now) )})
;;(registrar_usuario {:id 2, :altura 168, :peso 59, :idade 23, :sexo "F", :data_cadastro "31/05/2025" })
;;(println (consultar_usuarios))
