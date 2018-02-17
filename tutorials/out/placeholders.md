# Placeholders #
AnimationLib isn't just for putting some PlaceholderAPI placeholders in your text  
There's tons of magical stuff you can use in it  
NOTE: Need to use the % symbol? Use \\% or %%!  
### Summary ###
- [Formulas](#user-content-formulas)
- [Unicode characters](#user-content-unicode characters)
- [Box](#user-content-box)

- #### Formulas ####
  Need to make any sort of calculation? I've got you covered!  
  Surround your formula with \\() to make it work  
  Formulas account for operator precedence and allow groups  
  Valid operators:  
    - +, -, *, /, mod, ^, log, max, min
    - 'mod' returns the remainder after division, so 11 mod 3 results in 2 (11 is 3 times 3 with a remainder of 2)
    - 'max' and 'min' return the max and min value of the values on the left and right, respectively. 1 max 4 would give 4

  You can also use special functions, using &lt;functionname&gt;(&lt;calculation&gt;), like sqrt(4):  
    - sqrt, square, round, floor (round down), ceil (round up), ln, (a)sin, (a)cos, (a)tan, abs, random
    - 'random(n)' returns a value between 0 inclusively and n exclusively (with decimal digits!)

  Some examples:  
    - \\(:#:%player_health% / %player_max_health% * 100)% health
    - \\(:#:%player_food_level% * 5)% food


- #### Unicode characters ####
  Use special unicode characters by specifying their id (in hexadecimal format)  
  There's 2 ways to do this:  
    - With &#92;uXXXX where XXXX is the hexadecimal index of the character in the unicode table
    - With &#92;xXX. This is basically the above but for a smaller range, could be prettier

  The website I use for finding characters is https://unicode-table.com/  

- #### Box ####
  Useful shortcut for putting in a filled box. Use \\o for it.  

