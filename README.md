# KSS documentation parser

A KSS documentation parser (https://warpspire.com/kss/syntax/) written in
Clojure.

## Installation:

### deps.edn

`org.clojars.protocol55/kss {:mvn/version "0.1.0"}`

### Leiningen

`[org.clojars.protocol55/kss "0.1.0"]`

## Features

Supports the following KSS syntax:

- Header
- Description
- Deprecated/Experimental description prefix
- Modifiers
- Parameters
- Compatibility
- Markup
- Styleguide reference

Additionally supports the following:

- Weight
- Colors

## Usage

### Parsing

### `protocol55.kss.core/parse-lines`

Returns a lazy collection of KSS documentation maps from lines of css.

### `protocol55.kss.core/parse-string`

Returns a lazy collection of KSS documentation maps from a css string.

### `protocol55.kss.core/parse-stream`

Returns a lazy collection of KSS documentation maps from a java.io.BufferedReader.

### `protocol55.kss.core/parse-file`

Returns a collection of KSS documentation maps from java.io.File. Not lazy.

## Examples

### Basic

`styles/buttons.css`

```css
/*
A button suitable for giving stars to someone.


:hover             - Subtle hover highlight.
.stars-given       - A highlight indicating you’ve already given a star.
.stars-given:hover - Subtle hover highlight on top of stars-given styling.
.disabled          - Dims the button to indicate it cannot be used.



Styleguide 2.1.3.
*/
a.button.star{
  …
}
a.button.star.stars-given{
  …
}
a.button.star.disabled{
  …
}
```

```clojure
(require '[protocol55.kss.core :as kss])
(kss/parse-file (clojure.java.io/file "styles/buttons.css"))
```

```clojure
({:header "A button suitable for giving stars to someone.",
  :modifiers [{:name ":hover", :description "Subtle hover highlight."}
              {:name ".stars-given", :description "A highlight indicating you’ve already given a star."}
              {:name ".stars-given:hover", :description "Subtle hover highlight on top of stars-given styling."}
              {:name ".disabled", :description "Dims the button to indicate it cannot be used."}],
  :reference "2.1.3",
  :source {:base "fixtures", :name "basic.css", :path "fixtures/basic.css"}})
```

### Parameters

`styles/gradient.scss`

```scss
// Creates a linear gradient background, from top to bottom.
//
// $start - The color hex at the top.
// $end   - The color hex at the bottom.
//
// Compatible in IE6+, Firefox 2+, Safari 4+.
@mixin gradient($start, $end){
  …
}
```

```clojure
(require '[protocol55.kss.core :as kss])
(kss/parse-file (clojure.java.io/file "styles/gradient.scss"))
```

```clojure
({:header "Creates a linear gradient background, from top to bottom.",
  :parameters [{:name "$start", :default-value nil, :description "The color hex at the top."}
               {:name "$end", :default-value nil, :description "The color hex at the bottom."}],
  :compatibility "Compatible in IE6+, Firefox 2+, Safari 4+.",
  :source {:base "fixtures", :name "function.scss", :path "fixtures/function.scss"}})
```

### Colors

Example from https://github.com/kss-node/kss-node/blob/master/test/fixtures/property-colors.less

`styles/colors.scss`

```scss
// Complete Colors block
//
// Colors:
// @shortHexColor:      #f00            - short hexa color
// hex6:                #e7e7e7           - six digit hexa color
// --hex8:              #ff00FF00         - 8 digit hexa color with alpha and css custom property
// $rgbColor:           rgb(255,0,0)      - simple rgb
// percentRgb:          rgb(100%, 0%, 0%) - rgb with percent
// $color-links:        #0091aa           - links color
// rgbCliped:           rgb(300,0,0)      - simple rgb with red overflow
// negativeRGB:         rgb(255,-10,0)    - simple rgb with negative blue
// clipedPercentRGB:    rgb(110%, 0%, 0%) - simple rgb with percent with red overflow
// rgba:                rgba(255,0,0,1)
// rgbaPercent:         rgba(100%,0%,0%,1)
// rgbaFloat:           rgba(0,0,255,0.5)
// rgbaFloatZeroLess:   rgba(0,0,255,.5)
// rgbaPercentFloat:    rgba(100%, 50%, 0%, 0.1)
// hsl:                 hsl(0, 100%, 50%)
// hsl2:                hsl(120, 100%, 50%)
// hsl3:                hsl(120, 75%, 75%)
// hsl4:                hsl(120, 100%, 50%)
// hsla:                hsla(120, 100%, 50%, 1)
// hsla2:               hsla(240, 100%, 50%, 0.5)
// hsla3:               hsla(30, 100%, 50%, 0.1)
// hsla4:               hsla(30, 100%, 50%, .1)
// namedGrey:           grey              - color name
// #facdea - missing name
// ghostwhite
//
// Style guide: Colors.complete
```

```clojure
(require '[protocol55.kss.core :as kss])
(kss/parse-file (clojure.java.io/file "styles/colors.scss"))
```

```clojure
({:header "Complete Colors block",
  :colors [{:name "@shortHexColor", :css/color "#f00", :description "short hexa color"}
           {:name "hex6", :css/color "#e7e7e7", :description "six digit hexa color"}
           {:name "--hex8", :css/color "#ff00FF00", :description "8 digit hexa color with alpha and css custom property"}
           {:name "$rgbColor", :css/color "rgb(255,0,0)", :description "simple rgb"}
           {:name "percentRgb", :css/color "rgb(100%, 0%, 0%)", :description "rgb with percent"}
           {:name "$color-links", :css/color "#0091aa", :description "links color"}
           {:name "rgbCliped", :css/color "rgb(300,0,0)", :description "simple rgb with red overflow"}
           {:name "negativeRGB", :css/color "rgb(255,-10,0)", :description "simple rgb with negative blue"}
           {:name "clipedPercentRGB", :css/color "rgb(110%, 0%, 0%)", :description "simple rgb with percent with red overflow"}
           {:name "rgba", :css/color "rgba(255,0,0,1)", :description nil}
           {:name "rgbaPercent", :css/color "rgba(100%,0%,0%,1)", :description nil}
           {:name "rgbaFloat", :css/color "rgba(0,0,255,0.5)", :description nil}
           {:name "rgbaFloatZeroLess", :css/color "rgba(0,0,255,.5)", :description nil}
           {:name "rgbaPercentFloat", :css/color "rgba(100%, 50%, 0%, 0.1)", :description nil}
           {:name "hsl", :css/color "hsl(0, 100%, 50%)", :description nil}
           {:name "hsl2", :css/color "hsl(120, 100%, 50%)", :description nil}
           {:name "hsl3", :css/color "hsl(120, 75%, 75%)", :description nil}
           {:name "hsl4", :css/color "hsl(120, 100%, 50%)", :description nil}
           {:name "hsla", :css/color "hsla(120, 100%, 50%, 1)", :description nil}
           {:name "hsla2", :css/color "hsla(240, 100%, 50%, 0.5)", :description nil}
           {:name "hsla3", :css/color "hsla(30, 100%, 50%, 0.1)", :description nil}
           {:name "hsla4", :css/color "hsla(30, 100%, 50%, .1)", :description nil}
           {:name "namedGrey", :css/color "grey", :description "color name"}
           {:name nil, :css/color "#facdea", :description "missing name"}
           {:name nil, :css/color "ghostwhite", :description nil}],
  :reference "Colors.complete",
  :source {:base "test/fixtures", :name "property-colors.scss", :path "test/fixtures/property-colors.scss"}})
```

## Tests

Run `clj -Atest`.
