$(document).ready(function() {
    let code = $(".codemirror-textarea")[0];
    let editor = CodeMirror.fromTextArea(code, {
        lineNumbers : true,
        // value: "function myScript(){return 100;}\n",
        mode: "text/x-java"
    });
});