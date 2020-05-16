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
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css">


</head>

<body>
	<div class="container">
		<div class="page-header">
			<h2>
				MiniJava Compiler <small class="text-muted">- Developed by
					Nicola Folchi</small>
			</h2>
		</div>
		<p>
			Here I showcase my MiniJava Compiler. You can write any MiniJava
			program in the following textarea and then click on the "Compile"
			button below. My compiler will then perform syntactic and contextual
			analysis. Then, if it identifies the program as valid, it will write
			MJAM code that can be utilized by the JVM. Here is the <a
				data-toggle="modal" data-target="#myModal"> MiniJava Grammar. </a>
		</p>

		<!-- The Modal -->
		<div class="modal" id="myModal">
			<div class="modal-dialog modal-dialog-centered">
				<div class="modal-content">

					<!-- Modal body -->
					<div class="modal-body">
						<img src="miniJavaGrammar.png" alt="MiniJava Grammar" height="120%" width="100%">
					</div>


				</div>
			</div>
		</div>
		<!--  end of modal -->
		
		<div class="row">
			<div class="column">
				<form action="parse" method="post">
					<textarea class="codemirror-textarea" name="code"></textarea>
					<br> <input type="submit" class="btn btn-primary"
						value="Compile">
				</form>
			</div>
			<div class="column">
				<div class="container">
					<form style="margin: 0; padding: 0;" action="mjam" method="get">
						<button type="button" class="btn btn-success"
							data-toggle="collapse" data-target="#compiler">Compiler
							Output</button>
						<input style="display: inline;" type="submit"
							class="btn btn-primary" value="Generated MJAM Instructions">
					</form>
					<div id="compiler" class="collapse show">
						<%
						System.out.println(System.getProperty("user.dir"));
						BufferedReader reader = new BufferedReader(new FileReader("CompilerOutputFiles/compilerOutput.txt"));
						StringBuilder sb = new StringBuilder();
						String line;

						while ((line = reader.readLine()) != null) {
							sb.append(line + "<br>");
						}
						out.println("<pre>" + sb.toString() + "</pre>");
						PrintWriter writer = new PrintWriter("CompilerOutputFiles/compilerOutput.txt");
						writer.print("");
						writer.close();
						reader.close();
						%>
					</div>
				</div>
			</div>
		</div>
		<footer class="page-footer font-small blue pt-4">
			<div class="footer-copyright text-center py-3">Copyright © 2020
				Nicola Folchi. All rights reserved.</div>
		</footer>
	</div>

	<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="lib/codemirror.js"></script>
    <script type="text/javascript" src="lib/clike.js"></script>
    <script type="text/javascript" src="js/script.js"></script>
</body>

</html>