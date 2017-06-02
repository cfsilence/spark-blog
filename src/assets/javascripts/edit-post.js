$(document).ready(function(){
    $('.datepicker').datetimepicker({ format: dateFormat})

    editor = new wysihtml.Editor(
            "wysihtml-textarea", {
                toolbar: "toolbar",
                parserRules: wysihtmlParserRules
            }
    )
    editor.on( "load", function() {
        // Trick browser into showing HTML5 required validation popups.
        $('#wysihtml-textarea').addClass('nicehide');
    } );

    $('#addTagBtn').on('click', function(){
        $('#newTagModal').modal({show: true})
        $('#newTag').val('')
    })

    $('#saveNewTagBtn').on('click', function(){
        var tagEl = $('#newTag')
        if( !tagEl.val().length ) {
            tagEl.closest('.form-group').addClass('has-error')
        }
        else {
            tagEl.closest('.form-group').removeClass('has-error')

            $.ajax({
                url:   '/admin/ajaxSaveTag?tag=' + tagEl.val(),
                success: function(result){
                    $('#newTagModal').modal('hide')
                    currentTags = $('#tags').val()
                    listTags()
                },
                error: function(){
                    alert('An error occurred trying to save this tag.  Please try again.')
                }
            })
        }
    })

    $('#viewSourceBtn').on('click', function(){
        if( $('.nicehide').size() > 0 ) {
            $('.nicehide').addClass('nonicehide').removeClass('nicehide')
        }
    })

    $('#btnSubmit').on('click', function(){
        if( $('.nonicehide').size() > 0 ) {
            $('.nonicehide').addClass('nicehide').removeClass('nonicehide')
        }
    })

    listTags()

    setInterval(function(){
        // keep the session alive so that it doesn't expire in the middle of a blog post
        $.ajax({url: '/'})
    }, 30000)

    $('.help-modal-trigger').on('click', function(){
        $('#helpModal').modal('show')
    })
})

listTags = function(){
    $.ajax(
            {
                url: '/admin/ajaxListTags',
                success: function(result){
                    var sel = $('#tags')
                    sel.html('')

                    $(result).each(function(i,e){
                        var opt = $('<option value="' + e.id + '">' + e.name + '</option>')
                        if( $.inArray(e.id, currentTags) != -1 ) {
                            opt.attr('selected', 'selected')
                        }
                        sel.append(opt)
                    })
                },
                error: function(){
                    alert('An error occurred trying to list tags.  Please try again.')
                }
            }
    )
}