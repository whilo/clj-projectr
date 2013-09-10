(ns projectr.persist
  (:require [clojure.java.jdbc :as j]
            [clojure.java.jdbc.sql :as s]))

(def postgres-db {:subprotocol "postgresql"
                  :subname "//127.0.0.1:5432/projectr"
                  :user "void"
                  :password ""})

; example at http://clojure.github.io/java.jdbc/doc/clojure/java/jdbc/UsingDDL.html or
; https://en.wikibooks.org/wiki/Clojure_Programming/Examples/JDBC_Examples

; TODO use PostGIS types instead of real
(defn setup []
  (j/with-connection postgres-db
    (j/create-table :point [:id :serial "PRIMARY KEY"] [:lon :real] [:lat :real])
    (j/create-table :way [:id :serial "PRIMARY KEY"] [:points :bigint[]])))

#_(setup)

(defn add-point [p]
  (j/insert! postgres-db :point p))

#_(map add-point [{:id 500 :lon 8.30133 :lat 49.03822} {:id 501 :lon 8.30139 :lat 49.03743}])

(defn point-by-id [id]
  (j/query postgres-db
           (s/select #{:lon :lat} :point (s/where {:id id}))))

#_(point-by-id 500)

; construct JDBC4Array, ugly
(defn add-way [point-ids]
  (j/with-connection postgres-db
    (let [sql-array (.createArrayOf (j/connection) "bigint" (into-array point-ids))]
      (j/insert! postgres-db :way {:points sql-array}))))

#_(add-way [500 501])

; destructure JDBC4Array, ugly
(defn way-by-id [id]
  (j/with-connection postgres-db
    (j/with-query-results rs (into [] (s/select #{:points} :way (s/where {:id id})))
      (let [[sql-way] rs] ; destructuring first
        (assoc sql-way :points (into [] (.getArray (:points sql-way))))))))

#_(way-by-id 1)
