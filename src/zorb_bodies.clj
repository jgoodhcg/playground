(ns zorb-bodies)

(defn generate-assets [types-map]
  (let [gender-expressions (types-map :gender-expression)
        body-builds        (types-map :body-build)
        body-heights       (types-map :body-height)
        head-shapes        (types-map :head-shape)
        jaw-shapes         (types-map :jaw-shape)]
    {:head-assets (for [gender     gender-expressions
                        head-shape head-shapes
                        jaw-shape  jaw-shapes]
                    (str gender "_" head-shape "_" jaw-shape))
     :body-assets (for [gender      gender-expressions
                        body-build  body-builds
                        body-height body-heights]
                    (str gender "_" body-height "_" body-build))}))

;; Example usage:
(generate-assets
 {:gender-expression ["Masculine" "Feminine" "Androgynous"]
  :body-build ["Slender" "Average" "Robust"]
  :body-height ["Short" "Moderate" "Tall"]
  :head-shape ["Round" "Oval" "Square"]
  :jaw-shape ["Soft" "Defined" "Chiseled"]})
;; => {:head-assets
;;     ("Masculine_Round_Soft"
;;      "Masculine_Round_Defined"
;;      "Masculine_Round_Chiseled"
;;      "Masculine_Oval_Soft"
;;      "Masculine_Oval_Defined"
;;      "Masculine_Oval_Chiseled"
;;      "Masculine_Square_Soft"
;;      "Masculine_Square_Defined"
;;      "Masculine_Square_Chiseled"
;;      "Feminine_Round_Soft"
;;      "Feminine_Round_Defined"
;;      "Feminine_Round_Chiseled"
;;      "Feminine_Oval_Soft"
;;      "Feminine_Oval_Defined"
;;      "Feminine_Oval_Chiseled"
;;      "Feminine_Square_Soft"
;;      "Feminine_Square_Defined"
;;      "Feminine_Square_Chiseled"
;;      "Androgynous_Round_Soft"
;;      "Androgynous_Round_Defined"
;;      "Androgynous_Round_Chiseled"
;;      "Androgynous_Oval_Soft"
;;      "Androgynous_Oval_Defined"
;;      "Androgynous_Oval_Chiseled"
;;      "Androgynous_Square_Soft"
;;      "Androgynous_Square_Defined"
;;      "Androgynous_Square_Chiseled"),
;;     :body-assets
;;     ("Masculine_Short_Slender"
;;      "Masculine_Moderate_Slender"
;;      "Masculine_Tall_Slender"
;;      "Masculine_Short_Average"
;;      "Masculine_Moderate_Average"
;;      "Masculine_Tall_Average"
;;      "Masculine_Short_Robust"
;;      "Masculine_Moderate_Robust"
;;      "Masculine_Tall_Robust"
;;      "Feminine_Short_Slender"
;;      "Feminine_Moderate_Slender"
;;      "Feminine_Tall_Slender"
;;      "Feminine_Short_Average"
;;      "Feminine_Moderate_Average"
;;      "Feminine_Tall_Average"
;;      "Feminine_Short_Robust"
;;      "Feminine_Moderate_Robust"
;;      "Feminine_Tall_Robust"
;;      "Androgynous_Short_Slender"
;;      "Androgynous_Moderate_Slender"
;;      "Androgynous_Tall_Slender"
;;      "Androgynous_Short_Average"
;;      "Androgynous_Moderate_Average"
;;      "Androgynous_Tall_Average"
;;      "Androgynous_Short_Robust"
;;      "Androgynous_Moderate_Robust"
;;      "Androgynous_Tall_Robust")}
