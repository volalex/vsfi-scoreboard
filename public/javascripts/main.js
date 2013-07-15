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
    var forms = $(".form-inline");
    if(forms.length>0){
       forms.bind('submit',function (ev){
            ev.preventDefault();
            var params = $(this).serialize();
            var form = $(this);
           $.getJSON($(this).attr('action'),params,function (json){
               if(json.status=="success"){
                   form.children('input,select').each( function (i,e){
                       $(e).val('');
                   });
                   $(".notifications.top-left").notify({
                       message:{text:json.message},
                       type:"success"
                   }).show();
               }
               else{
                   $(".notifications.top-left").notify({
                       message:{text:json.message},
                       type:"error"
                   }).show();
               }
           });
        })
    }
});
