$(document).ready(function (){
    //enable popover
    $(".dashed-link").popover({
        html:true,
        placement:'bottom'
    });
    if($('#wmd-input').length>0){
        var converter = Markdown.getSanitizingConverter();
        var editor = new Markdown.Editor(converter);
        editor.run()
    }
});
