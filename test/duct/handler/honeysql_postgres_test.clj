(ns duct.handler.honeysql-postgres-test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [duct.core :as duct]
            [duct.database.sql :as db]
            [duct.handler.honeysql-postgres :as sql]
            [integrant.core :as ig]))

(duct/load-hierarchy)

(defn- create-connection []
  (let [uri (or (System/getenv "DATABASE_URL")
                "jdbc:postgresql://127.0.0.1/postgres")]
    {:connection (jdbc/get-connection {:connection-uri uri})}))

(defn- create-tables [f]
  (let [conn (create-connection)]
    (jdbc/execute! conn "DROP TABLE IF EXISTS posts")
    (jdbc/execute! conn "DROP TABLE IF EXISTS comments")
    (jdbc/execute! conn "CREATE TABLE posts (id SERIAL PRIMARY KEY, subject TEXT, body TEXT)")
    (jdbc/execute! conn "CREATE TABLE comments (id SERIAL PRIMARY KEY, post_id INT, body TEXT)")
    (jdbc/insert! conn :posts    {:id 1, :subject "Test", :body "Testing 1, 2, 3."})
    (jdbc/insert! conn :comments {:id 1, :post_id 1, :body "Great!"})
    (jdbc/insert! conn :comments {:id 2, :post_id 1, :body "Rubbish!"})
    (jdbc/query conn "SELECT setval('posts_id_seq', (SELECT MAX(id) FROM posts))")
    (jdbc/query conn "SELECT setval('comments_id_seq', (SELECT MAX(id) FROM comments))")
    (f)))

(use-fixtures :each create-tables)

(deftest derive-test
  (isa? ::sql/query     :duct.module.sql/requires-db)
  (isa? ::sql/query-one :duct.module.sql/requires-db)
  (isa? ::sql/execute   :duct.module.sql/requires-db)
  (isa? ::sql/insert    :duct.module.sql/requires-db))

(deftest prep-test
  (testing "query"
    (is (= (ig/prep {::sql/query
                     {:sql {:select [:*] :from [:comments]}}})
           {::sql/query
            {:db  (ig/ref :duct.database/sql)
             :sql {:select [:*] :from [:comments]}}})))

  (testing "query-one"
    (is (= (ig/prep {::sql/query
                     {:request '{{:keys [id]} :route-params}
                      :sql     '{:select [:subject :body] :from [:posts] :where [:= :id id]}}})
           {::sql/query
            {:db      (ig/ref :duct.database/sql)
             :request '{{:keys [id]} :route-params}
             :sql     '{:select [:subject :body] :from [:posts] :where [:= :id id]}}})))

  (testing "execute"
    (is (= (ig/prep {::sql/query
                     {:request '{{:keys [id]} :route-params, {:strs [body]} :form-params}
                      :sql     '{:update :comments :set {:body body} :where [:= :id id]}}})
           {::sql/query
            {:db      (ig/ref :duct.database/sql)
             :request '{{:keys [id]} :route-params, {:strs [body]} :form-params}
             :sql     '{:update :comments :set {:body body} :where [:= :id id]}}})))

  (testing "insert"
    (is (= (ig/prep {::sql/query
                     {:request  '{{:keys [pid]} :route-params, {:strs [body]} :form-params}
                      :sql      '{:insert-into :comments :columns [:post_id :body] :values [[pid body]] :returning [:id]}
                      :location "/posts{/pid}/comments{/id}"}})
           {::sql/query
            {:db       (ig/ref :duct.database/sql)
             :request  '{{:keys [pid]} :route-params, {:strs [body]} :form-params}
             :sql      '{:insert-into :comments :columns [:post_id :body] :values [[pid body]] :returning [:id]}
             :location "/posts{/pid}/comments{/id}"}}))))

(deftest query-test
  (testing "with destructuring"
    (let [config  {::sql/query
                   {:db      (db/->Boundary (create-connection))
                    :request '{{:keys [post-id]} :route-params}
                    :sql     '{:select [:body] :from [:comments] :where [:= :post_id post-id]}}}
          handler (::sql/query (ig/init config))]
      (is (= (handler {:route-params {:post-id 1}})
             {:status 200, :headers {}, :body [{:body "Great!"} {:body "Rubbish!"}]}))))

  (testing "without destructuring"
    (let [config  {::sql/query
                   {:db  (db/->Boundary (create-connection))
                    :sql {:select [:subject] :from [:posts]}}}
          handler (::sql/query (ig/init config))]
      (is (= (handler {})
             {:status 200, :headers {}, :body [{:subject "Test"}]}))))

  (testing "with renamed keys"
    (let [config  {::sql/query
                   {:db     (db/->Boundary (create-connection))
                    :sql {:select [:subject :body] :from [:posts]}
                    :rename {:subject :post/subject}}}
          handler (::sql/query (ig/init config))]
      (is (= (handler {})
             {:status 200, :headers {}, :body [{:post/subject "Test"
                                                :body "Testing 1, 2, 3."}]}))))

  (testing "with hrefs"
    (let [config  {::sql/query
                   {:db    (db/->Boundary (create-connection))
                    :sql   {:select [:id :subject] :from [:posts]}
                    :hrefs {:href "/posts{/id}"}}}
          handler (::sql/query (ig/init config))]
      (is (= (handler {})
             {:status 200, :headers {}, :body [{:id   1
                                                :href "/posts/1"
                                                :subject "Test"}]}))))

  (testing "with hrefs from request vars"
    (let [config  {::sql/query
                   {:db      (db/->Boundary (create-connection))
                    :request '{{:keys [pid]} :route-params}
                    :sql     {:select [:id :body] :from [:comments]}
                    :hrefs   {:href "/posts{/pid}/comments{/id}"}}}
          handler (::sql/query (ig/init config))]
      (is (= (handler {:route-params {:pid 1}})
             {:status  200
              :headers {}
              :body    [{:id 1, :href "/posts/1/comments/1", :body "Great!"}
                        {:id 2, :href "/posts/1/comments/2", :body "Rubbish!"}]}))))

  (testing "with removed keys"
    (let [config  {::sql/query
                   {:db     (db/->Boundary (create-connection))
                    :sql    {:select [:id :subject] :from [:posts]}
                    :hrefs  {:href "/posts{/id}"}
                    :remove [:id]}}
          handler (::sql/query (ig/init config))]
      (is (= (handler {})
             {:status 200, :headers {}, :body [{:href "/posts/1"
                                                :subject "Test"}]})))))

(deftest query-one-test
  (testing "with destructuring"
    (let [config  {::sql/query-one
                   {:db      (db/->Boundary (create-connection))
                    :request '{{:keys [id]} :route-params}
                    :sql     '{:select [:subject :body] :from [:posts] :where [:= :id id]}}}
          handler (::sql/query-one (ig/init config))]
      (is (= (handler {:route-params {:id 1}})
             {:status 200, :headers {}, :body {:subject "Test", :body "Testing 1, 2, 3."}}))
      (is (= (handler {:route-params {:id 2}})
             {:status 404, :headers {}, :body {:error :not-found}}))))

  (testing "with renamed keys"
    (let [config  {::sql/query-one
                   {:db      (db/->Boundary (create-connection))
                    :request '{{:keys [id]} :route-params}
                    :sql     '{:select [:subject :body] :from [:posts] :where [:= :id id]}
                    :rename  {:subject :post/subject}}}
          handler (::sql/query-one (ig/init config))]
      (is (= (handler {:route-params {:id 1}})
             {:status 200, :headers {}, :body {:post/subject "Test"
                                               :body "Testing 1, 2, 3."}}))))

  (testing "with hrefs"
    (let [config  {::sql/query-one
                   {:db      (db/->Boundary (create-connection))
                    :request '{{:keys [id]} :route-params}
                    :sql     '{:select [:id :subject] :from [:posts] :where [:= :id id]}
                    :hrefs   {:href "/posts{/id}"}}}
          handler (::sql/query-one (ig/init config))]
      (is (= (handler {:route-params {:id 1}})
             {:status 200, :headers {}, :body {:id      1
                                               :href    "/posts/1"
                                               :subject "Test"}}))))

  (testing "with hrefs from request vars"
    (let [config  {::sql/query-one
                   {:db      (db/->Boundary (create-connection))
                    :request '{{:keys [id]} :route-params}
                    :sql     '{:select [:subject] :from [:posts] :where [:= :id id]}
                    :hrefs   {:href "/posts{/id}"}}}
          handler (::sql/query-one (ig/init config))]
      (is (= (handler {:route-params {:id 1}})
             {:status 200, :headers {}, :body {:href "/posts/1"
                                               :subject "Test"}}))))

  (testing "with removed keys"
    (let [config  {::sql/query-one
                   {:db      (db/->Boundary (create-connection))
                    :request '{{:keys [id]} :route-params}
                    :sql     '{:select [:subject] :from [:posts] :where [:= :id id]}
                    :hrefs   {:href "/posts{/id}"}
                    :remove  [:id]}}
          handler (::sql/query-one (ig/init config))]
      (is (= (handler {:route-params {:id 1}})
             {:status 200, :headers {}, :body {:href "/posts/1"
                                               :subject "Test"}})))))

(deftest execute-test
  (let [db      (create-connection)
        config  {::sql/execute
                 {:db      (db/->Boundary db)
                  :request '{{:keys [id]} :route-params, {:strs [body]} :form-params}
                  :sql     '{:update :comments :set {:body body} :where [:= :id id]}}}
        handler (::sql/execute (ig/init config))]
    (testing "valid update"
      (is (= (handler {:route-params {:id 1}, :form-params {"body" "Average"}})
             {:status 204, :headers {}, :body nil}))
      (is (= (jdbc/query db ["SELECT * FROM comments WHERE id = ?" 1])
             [{:id 1, :post_id 1, :body "Average"}])))

    (testing "update of invalid ID"
      (is (= (handler {:route-params {:id 3}, :form-params {"body" "Average"}})
             {:status 404, :headers {}, :body {:error :not-found}})))))

(deftest insert-test
  (testing "with location"
    (let [db      (create-connection)
          config  {::sql/insert
                   {:db       (db/->Boundary db)
                    :request  '{{:keys [pid]} :route-params, {:strs [body]} :form-params}
                    :sql      '{:insert-into :comments :columns [:post_id :body] :values [[pid body]] :returning [:id]}
                    :location "/posts{/pid}/comments{/id}"}}
          handler (::sql/insert (ig/init config))]
      (is (= (handler {:route-params {:pid 1}, :form-params {"body" "New comment"}})
             {:status 201, :headers {"Location" "/posts/1/comments/3"}, :body nil}))
      (is (= (jdbc/query db ["SELECT * FROM comments WHERE id = ?" 3])
             [{:id 3, :post_id 1, :body "New comment"}]))))

  (testing "without location"
    (let [db     (create-connection)
          config {::sql/insert
                  {:db      (db/->Boundary db)
                   :request '{{:keys [pid]} :route-params, {:strs [body]} :form-params}
                   :sql     '{:insert-into :comments :columns [:post_id :body] :values [[pid body]]}}}
          handler (::sql/insert (ig/init config))]
      (is (= (handler {:route-params {:pid 1}, :form-params {"body" "New comment"}})
             {:status 201, :headers {}, :body nil}))
      (is (= (jdbc/query db ["SELECT * FROM comments WHERE id = ?" 3])
             [{:id 3, :post_id 1, :body "New comment"}])))))
