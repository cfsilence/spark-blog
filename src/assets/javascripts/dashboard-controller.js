var DashboardController = {

    presentationModel: {
        selectedPost: {},
        drafts: [],
        posts: []
    },

    init: function(){
        $('.delete-post').on('click', function(e){
            e.preventDefault()
            var id = $( this ).data( 'id' )
            var postId = $( this ).data( 'postId' )
            var publish = !$( this ).data( 'isPublished' )
            var to = "/admin/post/publish?redirectTo=list" + '&postId=' + postId + '&publish=' + publish
            var confirmModal = $('#confirmPublishModal')
            if( publish ) {
                $('#confirmDelete').removeClass('btn-danger').addClass('btn-primary').find('#confirmDeleteLabel').html('Publish').closest('button').find('.fa').removeClass('fa-trash-o').addClass('fa-bullhorn')
                $('#publishAction').html('publish')
            }
            else {
                $('#confirmDelete').removeClass('btn-primary').addClass('btn-danger').find('#confirmDeleteLabel').html('Unpublish').closest('button').find('.fa').addClass('fa-trash-o').removeClass('fa-bullhorn')
                $('#publishAction').html('unpublish')
            }

            confirmModal.modal({show: 'false'})

            $('body').on('click', '#cancelDelete', function(){
                confirmModal.modal('hide')
            })

            $('body').on('click', '#confirmDelete', function(){
                window.location = to
            })

            $('#confirmDeleteModal').modal('show')
        })
        DashboardController.loadData()
        DashboardController.bindData()
        DashboardController.bindEvents()
        console.log('dashboard init')
    },

    bindEvents: function(){
        $(document).on('posts.loaded', function(){

        })
        $(document).on('drafts.loaded', function(){

        })
    },

    bindData: function(){
        rivets.bind($('body').get(), {model: DashboardController.presentationModel})
    },

    loadPosts: function(){
        DashboardController.postGrid = $('#postsTbl').bootgrid({
            ajax: true,
            ajaxSettings: {
                method: "GET",
                cache: false
            },
            url: '/admin/ajaxListPublished',
            rowCount: [5,10,25],
            labels: {
                infos: '{{ctx.start}} to {{ctx.end}} of {{ctx.total}}'
            },
            formatters: {
                postActions: function(col, row) {
                    var btn = '<button class="btn btn-sm btn-danger unpublish" data-row-id="'+row.id+'"><i class="fa fa-trash-o"></i> Unpublish</button>'
                    var edit = ' <a href="/admin/post/edit?id='+row.id+'" class="btn btn-sm btn-primary"><i class="fa fa-edit"></i> Edit</a>'
                    return btn + edit
                },
                published: function(col, row) {
                    return moment(row.published_date).format('MM/DD/YYYY hh:mm A')
                }
            }
        }).on("loaded.rs.jquery.bootgrid", function(){
            /* Executes after data is loaded and rendered */
            DashboardController.postGrid.find(".unpublish").on("click", function(e){
                var id = $(this).data("row-id")
                $.ajax({
                    url: '/admin/ajaxPublish?publish=false&id=' + id,
                    success: function(response){
                        DashboardController.postGrid.bootgrid('reload')
                        DashboardController.draftGrid.bootgrid('reload')
                    }
                })
            })
        }).on("click.rs.jquery.bootgrid", function (e, cols, row){
            DashboardController.presentationModel.selectedPost = row
        })
        /*
        $.ajax({url: '/admin/ajaxListPublished', success: function(response){
            DashboardController.posts.length = 0
            $(response.posts).each(function(i,e){
                DashboardController.posts.push(e)
            })
            $(document).trigger('posts.loaded')
        }})
        */
    },

    loadDrafts: function(){
        DashboardController.draftGrid = $('#draftsTbl').bootgrid({
            ajax: true,
            ajaxSettings: {
                method: "GET",
                cache: false
            },
            url: '/admin/ajaxListDrafts',
            rowCount: [5,10,25],
            labels: {
                infos: '{{ctx.start}} to {{ctx.end}} of {{ctx.total}}'
            },
            formatters: {
                draftActions: function(col, row) {
                    var btn = '<button class="btn btn-sm btn-default publish" data-row-id="'+row.id+'"><i class="fa fa-bullhorn"></i> Publish</button>'
                    var edit = ' <a href="/admin/post/edit?id='+row.id+'" class="btn btn-sm btn-primary"><i class="fa fa-edit"></i> Edit</a>'
                    return btn + edit
                }
            }
        }).on("loaded.rs.jquery.bootgrid", function(){
              /* Executes after data is loaded and rendered */
              DashboardController.draftGrid.find(".publish").on("click", function(e){
                  var id = $(this).data("row-id")
                  $.ajax({
                      url: '/admin/ajaxPublish?publish=true&id=' + id,
                      success: function(response){
                          DashboardController.postGrid.bootgrid('reload')
                          DashboardController.draftGrid.bootgrid('reload')
                      }
                  })
              })
        })
        /*
        $.ajax({url: '/admin/ajaxListDrafts', success: function(response){
            DashboardController.drafts.length = 0
            $(response.posts).each(function(i,e){
                DashboardController.drafts.push(e)
            })
            $(document).trigger('drafts.loaded')
        }})
        */
    },

    loadData: function(){
        DashboardController.loadPosts()
        DashboardController.loadDrafts()
    }

}
$().ready(function(){
    DashboardController.init()
})