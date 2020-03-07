(ns mobile-data-editor-poc)

(defn map-prev [data path k]
  [:view
   [:touchable
    {:on-press "update with new path + k"}
    (take 3 (keys (get-in data (conj path k))))]])

(defn coll-prev [data path k]
  [:view
   [:touchable
    {:on-press "update with new path + k"}
    (take 3 (get-in data (conj path k)))]])

(defn preview [data path k]
  (condp #(%1 %2) (get-in data (conj path k))
    map? (map-prev data path k)
    coll? (coll-prev data path k)
    string? "str"
    number? "number"
    ;; date?
    keyword? "keyword"
    "something else!"))

(defn map-comp [data path]
  [:view
   [:scrollable-list
    (->> (get-in data path)
         seq
         (map
          (fn [[k v]]
            [:view
             [:delete-button {:on-press "dissoc path + k"}]
             [:text-input {:on-update "rename-keys"}
              k]
             (preview data path k)])))
    [:add-button {:on-press "modal to enter key and choose type then assoc that in to data"}]]])

(defn coll-comp [data path]
  [:view
   [:scrollable-list
    (->> (get-in data path)
         (map-indexed
          (fn [index item]
            [:view
             [:delete-button {:on-press "subvec out item at index "}]
             ;; http://clojuredocs.org/clojure.core/subvec#example-58069d53e4b001179b66bdcd
             [:text index]
             (preview data path index)])))
    [:add-button {:on-press "modal to choose type then conj that in to data"}]]])

(defn editor [data path]
  (condp #(%1 %2) (get-in data path)
    map? (map-comp data path)
    coll? (coll-comp data path)
    "Error path did not result in a map or a coll"))

(editor {:a [1 2 {:c 3}]} [:a])
