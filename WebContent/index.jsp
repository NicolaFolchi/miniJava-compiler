<%@ page language="java" import="java.io.*, java.util.*" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="utf-8">

    <title>MiniJava Compiler</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="MiniJava Compiler">
    <meta name="author" content="Nicola Folchi">

    <!-- <link rel="stylesheet" href="style.css"> -->
    <link rel="stylesheet" href="lib/codemirror.css">
    <link rel="stylesheet" href="lib/material-darker.css">

</head>

<body>
    <div class="row">
        <div class="column">
            <form action="parse" method="post">
                <textarea class="codemirror-textarea" name="code"></textarea>
                <input type="submit">
            </form>    
        </div>
        <div class="column">
            <%
            BufferedReader reader = new BufferedReader(new FileReader("compilerOutput.txt"));
            StringBuilder sb = new StringBuilder();
            String line;
        
            while((line = reader.readLine())!= null){
                sb.append(line+"<br>");
            }
            out.println(sb.toString());
            PrintWriter writer = new PrintWriter("compilerOutput.txt");
            writer.print("");
            writer.close();
            
            %>
        </div>
    </div>


	<script type="text/javascript" src="js/jquery.js"></script>
    <script type="text/javascript" src="lib/codemirror.js"></script>
    <script type="text/javascript" src="lib/clike.js"></script>
    <script type="text/javascript" src="js/script.js"></script>
</body>

</html>