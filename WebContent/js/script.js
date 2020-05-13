$(document).ready(function() {
    let code = $(".codemirror-textarea")[0];
    let editor = CodeMirror.fromTextArea(code, {
        mode: "text/x-java",
        theme: "material-darker",
        lineNumbers : true,
    }).setValue("// Insert your MiniJava code here: \n");
    code.setSize(9000, "1000%");
});