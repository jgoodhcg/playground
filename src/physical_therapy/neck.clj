(ns physical-therapy.neck)

(def exercises
  [{:name "Seated Cervical Retraction"
    :targets ["Cervical Extensors", "Suboccipitals"]}

   {:name "Sternocleidomastoid Stretch"
    :targets ["Sternocleidomastoid", "Scalenes"]}

   {:name "Seated Scalenes Stretch"
    :targets ["Scalenes", "Upper Trapezius"]}

   {:name "Thoracic Extension Mobilization on Foam Roll"
    :targets ["Thoracic Spine", "Rhomboids", "Middle Trapezius"]}

   {:name "Sidelying Thoracic Lumbar Rotation"
    :targets ["Thoracic Spine", "Lumbar Spine"]}

   {:name "Seated Thoracic Lumbar Extension"
    :targets ["Thoracic Spine", "Lumbar Spine"]}

   {:name "Prone Scapular Retraction on Swiss Ball"
    :targets ["Scapular Retractors", "Rhomboids", "Middle Trapezius"]}

   {:name "Prone Shoulder Extension on Swiss Ball"
    :targets ["Posterior Deltoid", "Triceps", "Latissimus Dorsi"]}

   {:name "Prone Middle Trapezius Strengthening on Swiss Ball"
    :targets ["Middle Trapezius", "Rhomboids"]}

   {:name "Prone Lower Trapezius Strengthening on Swiss Ball"
    :targets ["Lower Trapezius", "Serratus Anterior"]}

   {:name "Low Trap Setting at Wall"
    :targets ["Lower Trapezius", "Serratus Anterior"]}

   {:name "Full Plank"
    :targets ["Core", "Shoulders", "Chest"]}

   {:name "Seated Assisted Cervical Rotation with Towel"
    :targets ["Cervical Rotators", "Scalenes"]}

   {:name "First Rib Mobilization with Strap"
    :targets ["First Rib", "Scalenes"]}

   {:name "Seated Cervical Rotation with Nod"
    :targets ["Cervical Rotators", "Suboccipitals"]}

   {:name "Supine Dead Bug on Foam Roll with Heel Slide"
    :targets ["Core", "Abdominals"]}

   {:name "Seated Row Cable Machine"
    :targets ["Rhomboids", "Middle Trapezius", "Latissimus Dorsi"]}

   {:name "Scaption with Dumbbells"
    :targets ["Deltoids", "Supraspinatus"]}])

(defn random-exercises []
  (take 3 (shuffle exercises)))

(comment
  (->> (random-exercises) (map :name))
  ;; => ("Prone Scapular Retraction on Swiss Ball"
  ;;     "Low Trap Setting at Wall"
  ;;     "First Rib Mobilization with Strap")
  )
