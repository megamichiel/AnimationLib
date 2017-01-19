<h1>AnimationLib Configuration</h1>
  This page describes what you can do with AnimationLib's config file<br/>
<h3>Features</h3>
<ul>
  <li id="auto_download_placeholders"><b>Auto-Download-Placeholders</b><br/>
    <hr/>
      When set to 'true' and a placeholder is used with an unknown expansion, it will be automatically downloaded<br/>
    <br/>
  <li id="databases"><b>Databases</b><br/>
    Section<br/>
    <hr/>
      This contains several sections that each specify a database connection<br/>
      Use 'url', 'user' and 'password' to specify database information<br/>
      NOTE: If you're using a MySQL database, make sure the url starts with 'jdbc:mysql://'<br/>
      e.g.:<br/>
      <b>Databases:</b><br/>
        <b>example:</b><br/>
          <b>url:</b> 'jdbc:mysql://localhost/test'<br/>
          <b>user:</b> 'host'<br/>
          <b>password:</b> ''<br/>
      You can use this for <a href="#sql_queries">Sql-Queries</a> or with AnimatedMenu Plus's sql statements<br/>
    <br/>
  <li id="formula_locale"><b>Formula-Locale</b><br/>
    <hr/>
      The locale to use to display formula results, e.g.:<br/>
    <ul>
      <li><b>en_US:</b> 123,456 and 345,987.246</li>
      <li><b>de_DE:</b> 123.456 and 345.987,246</li>
      <li><b>fr_FR:</b> 123 456 and 345 987,246</li>
    </ul>
      Though I don't know how many of these are implemented (possibly all), a page containing a lot of locales can be found <a href="http://www.science.co.il/Language/Locale-codes.php">here</a><br/>
    <br/>
  <li id="formulas"><b>Formulas</b><br/>
    Section<br/>
    <hr/>
      Specifies various formulas to be used as placeholders<br/>
      You can either make the value of a formula some text (simple formula)<br/>
      Or you can use a section to specify 'value' (the formula) and 'format' (the <a href="https://docs.oracle.com/javase/tutorial/i18n/format/decimalFormat.html">number format</a> to use)<br/>
      Formulas account for operator precedence and allow groups<br/>
      Valid operators:<br/>
    <ul>
      <li>+, -, *, /, mod, ^, log, max, min</li>
      <li>'mod' returns the remainder after division, so 11 mod 3 results in 2 (11 is 3 times 3 with a remainder of 2)</li>
      <li>'max' and 'min' return the max and min value of the values on the left and right, respectively. 1 max 4 would give 4</li>
    </ul>
      You can also use special functions, using &lt;functionname&gt;(&lt;calculation&gt;), like sqrt(4):<br/>
    <ul>
      <li>sqrt, square, round, floor (round down), ceil (round up), ln, (a)sin, (a)cos, (a)tan, abs, random</li>
      <li>'random(n)' returns a value between 0 inclusively and n exclusively (with digits!)</li>
    </ul>
      e.g.:<br/>
      <b>Formulas:</b><br/>
        <b>sometext:</b> '(%player_health% + 3) * 5'<br/>
        <b>somesection:</b><br/>
          <b>format:</b> '&#35;'<br/>
          <b>value:</b> '(%player_health% - 5) / 3'<br/>
    <br/>
  <li id="sql_queries"><b>Sql-Queries</b><br/>
    Section<br/>
    <hr/>
      A section containing several sql queries for use in placeholders<br/>
      Use 'Refresh-Delay' here to specify the delay in seconds between automatic refreshes<br/>
      For each other key, you can use a section with these options:<br/>
    <ul>
      <li><b>Database:</b> The id (key) of the database as specified in <a href="#databases">Databases</a></li>
      <li><b>Query:</b> The SQL query to perform (e.g. select). This supports placeholders</li>
      <li><b>Default:</b> The value to use when the query has not yet been (successfully) refreshed</li>
      <li><b>Lifespan:</b> The amount of times a query result can be used before it is discarded and Default is used again until the next refresh</li>
      <li><b>Request-On-Join:</b> If this is set to 'true', the query will be refreshed when the player joins*</li>
      <li><b>Auto-Refresh:</b> If this is set to 'true', this query will be refreshed for all online players as per Refresh-Delay</li>
      <li><b>Script:</b></li>
        A string (or string list) that processes the result of the query.<br/>
        You can use 'sql', which is a <a href="http://docs.oracle.com/javase/7/docs/api/java/sql/ResultSet.html">ResultSet</a><br/>
        Example usage with query 'SELECT `Coins` FROM `Sometable` WHERE `UUID`='%player_uuid%' ':<br/>
        <b>Script:</b> '<font style="background: #DDD;">sql.next() ? sql.getInt("Coins") : 0;</font>'<br/>
        Note that the last line must return a value, but not start with 'return'<br/>
    </ul>
    <br/>
</ul>