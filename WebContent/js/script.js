$(document).ready(function() {
    let code = $(".codemirror-textarea")[0];
    let editor = CodeMirror.fromTextArea(code, {
        mode: "text/x-java",
        theme: "material-darker",
        lineNumbers : true,
    });
    if (editor.getValue().trim() === "") {
        editor.setValue("// Insert your MiniJava code here: \n");
    }
    editor.setSize("100%", 500);
});
