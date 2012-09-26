(ns hits.web-test
  (:use hits.models.datomic)
  (:use hits.web.middleware)
  (:require [noir.core :as noir])
  (:require [datomic.api :only [q db] :as d])
  (:require [hiccup.core :as hicc])
  (:require [hiccup.form-helpers :as form])
  (:require [hits.models.git-schema :only [schema] :as git])
  (:require [hiccup.page-helpers :only [link-to] :as page])
  (:require [noir.response :as resp])
  (:require [noir.server :as server]))

(def start-repos [["eigenhombre" "namejen"]
                  ["hits" "hits"]])

(defn do-repos! [conn repos]
  (apply concat (map (fn [[name proj]] (add-repo-to-db! conn name proj)) repos)))

(defn setup-datomic! [uri]
    (d/create-database uri)
    (let [conn (d/connect uri)]
      @(d/transact conn git/schema)
      conn))

(def conn (let [c (setup-datomic! "datomic:mem://hits-live")]
            (do-repos! c start-repos)
            c))

(defn link-for [name repo] (format "/%s/%s" name repo))

(defn conditional-add-repo [user repo]
  (when-not (contains? (current-repos (d/db conn)) [user repo])
    (datomic.common/await-derefs (add-repo-to-db! conn user repo))))

(defn author_data [user repo]
  (author-activity user repo "" (d/db conn)))

(defn repolist []
  (map (fn [[name repo]]
         [:p (page/link-to {:class "proj",
                            :id (str name "/" repo)}
                           (link-for name repo)
                           (str name "/" repo))])
       (current-repos (d/db conn))))

(defn repoform []
  (form/form-to [:post "/author-data"]
                (form/label "user" "Owner: ")
                (form/text-field {:id "user"} "user" "")
                (form/label "repo" "Repo: ")
                (form/text-field "repo" "")
                (form/submit-button {:id "submit"} "Go")))

(noir/defpage [:post "/author-data"] {:keys [user repo]}
  (conditional-add-repo user repo)
  (resp/json  (author-activity user repo "" (d/db conn))))

(noir/defpage "/update" {:keys [user repo]}
  (conditional-add-repo user repo)
  (resp/json
        {:current_repos (current-repos (d/db conn))
         :author_data (author_data user repo)}))

(noir/defpage "/" []
  (hicc/html (page/include-js (str "//ajax.googleapis.com/ajax/libs/jquery/"
                                   "1.8.1/jquery.min.js"))
             (page/include-js "/js/callbacks.js")
             [:h1 "Welcome to HITS (Hands in the Soup)"]
             [:p [:b "Available repos:"]]
             [:div#repolist (repolist)]
             (repoform)
             [:p "Or visit /owner/repo of your choice"]
             [:div#rawdata]))

(noir/defpage "/:name/:repo" {:keys [name repo]}
  (when (not (contains? (current-repos (d/db conn)) [name repo]))
    (datomic.common/await-derefs (add-repo-to-db! conn name repo)))
  (hicc/html [:h1 (format "%s/%s" name repo)]
             (map (fn [[author counts]] [:pre author " " counts])
                  (reverse (sort-by second (author-activity name repo "" (d/db conn)))))))

;; run (web-main) at REPL to launch test server:
(defn web-main []
  (server/add-middleware wrap-slash)
  (let [port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port)))

(defn -main []
  (web-main))

