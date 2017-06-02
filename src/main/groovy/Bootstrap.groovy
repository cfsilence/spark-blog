import codes.recursive.Contact
import codes.recursive.domain.Role
import codes.recursive.domain.User
import codes.recursive.domain.UserRole
import codes.recursive.domain.blog.Post
import codes.recursive.domain.blog.PostTag
import codes.recursive.domain.blog.Tag
import codes.recursive.service.BlogService
import codes.recursive.service.NotificationService
import codes.recursive.service.UserService
import codes.recursive.util.RequestUtil
import groovy.json.JsonOutput
import nz.net.ultraq.thymeleaf.LayoutDialect
import org.javalite.activejdbc.Base
import org.javalite.activejdbc.Messages
import org.mindrot.jbcrypt.BCrypt
import spark.ModelAndView
import spark.Spark
import spark.template.thymeleaf.ThymeleafTemplateEngine

import java.text.DateFormat
import java.text.SimpleDateFormat

import static spark.Spark.*

class Bootstrap {

    static ConfigObject loadConfig(String environment) {
        def configFile = new File("src/main/groovy/conf/config-${environment}.groovy")
        if( !configFile.exists() ) {
            throw new Exception("Config file missing.\n\t-> You have attempted to load a config file from '${configFile.canonicalPath}' but none was found.\n\t-> Please see '${configFile.canonicalPath.replace('config-'+environment+'.groovy', 'config-template.groovy')}' in that directory,\n\t-> make a copy, rename it for this environment and populate it as necessary.")
        }
        return new ConfigSlurper(environment).parse(configFile.toURI().toURL())
    }

    static void main(String[] args) {
        def environment = System.getProperty('environment') ?: 'dev'
        /*
        if (environment == 'dev') {
            String projectDir = System.getProperty("user.dir")
            String staticDir = "/src/main/resources/static"
            Spark.staticFiles.externalLocation(projectDir + staticDir)
        } else {
            Spark.staticFiles.location("/static");
        }
        */
        ResourceBundle.getBundle('activejdbc_messages')
        Spark.staticFiles.location("/static");

        def config = loadConfig(environment)

        BlogService blogService = new BlogService()
        UserService userService = new UserService()
        NotificationService notificationService = new NotificationService(config.awssdk.ses.accessKey, config.awssdk.ses.secretKey)

        ThymeleafTemplateEngine engine = new ThymeleafTemplateEngine()
        engine.templateEngine.addDialect(new LayoutDialect())


        def commonModel = [:]

        def flash = { request, key = null, value = null ->
            if (!request.session().attribute('flash')) request.session().attribute('flash', [:])
            if (!key && !value) {
                return request.session().attribute('flash')
            }
            if (key && value) {
                request.session().attribute('flash')[key] = [requests: 1, value: value]
            }
            return request.session().attribute('flash')[key]?.value
        }

        before "/*", { req, res ->
            req.session().maxInactiveInterval(60*30)

            def datasource = config.datasource

            if( !Base.hasConnection() ) {
                Base.open(datasource.driverClassName, datasource.url, datasource.username, datasource.password)
            }

            commonModel = [
                    config     : config << [environment: environment],
                    isLoggedIn : req.session().attribute('isLoggedIn') ?: false,
                    year       : Calendar.getInstance().get(Calendar.YEAR),
                    currentUser: userService.findById(req.session().attribute('currentUserId')) ?: null,
                    analytics  : environment != 'dev',
            ]
        }

        after "/*", { req, res ->
            flash(req).each {
                it.value.requests++
                if (it.value.requests > 2) {
                    flash(req).remove(it.key)
                }
            }
            if( Base.hasConnection() ) {
                Base.close()
            }
        }

        afterAfter "/*", { req, res ->
        }

        path "/admin", {
            before "/*", { req, res ->
                /* only checks for logged in user, doesn't check role at all...all users can access admin currently */
                if (!req.session().attribute('isLoggedIn')) {
                    res.redirect('/page/index')
                    return
                }
            }

            path "/post", {
                post "/edit", { req, res ->
                    def id = req.queryParams("id") as Long
                    def post = blogService.findById(id) ?: Post.create()
                    post = RequestUtil.populateParams(Post.class, req, post)
                    post.set('is_published', req.queryMap().get('is_published').value()?.toInteger() ?: 0)
                    post.validate()
                    if (post.valid) {
                        post = blogService.save(post)
                        post.getAll(PostTag.class).each { PostTag pt ->
                            pt.delete()
                        }
                        req.queryMap().get('tags').values().toList().each {
                            post.add(PostTag.create('post_id', post.get('id'), 'tag_id', it as Long))
                        }
                        flash(req, 'message', Messages.message('default.saved', 'Post'))
                        res.redirect('/admin/post/edit?id=' + post.get('id'))
                    } else {
                        def tagIds = req.queryMap().get('tags').values().toList()
                        def model = [
                                post  : post,
                                tagIds: tagIds,
                                error : flash(req, 'error', Messages.message('default.errors')),
                        ]
                        return engine.render(new ModelAndView(commonModel << model, "admin/post/edit"))
                    }
                }
                get "/edit", { req, res ->
                    def id = req.queryParams("id") as Long
                    def post = blogService.findById(id) ?: Post.create()
                    def tags = post.getAll(PostTag.class).collect { it.parent(Tag.class) }
                    def tagIds = tags.collect { it.id }
                    def model = [post: post, tagIds: tagIds, message: flash(req, 'message')]

                    return engine.render(new ModelAndView(commonModel << model, "admin/post/edit"))
                }
                get "/list", { req, res ->
                    def max = req.queryParams("max") as Long ?: 10
                    def offset = req.queryParams("offset") as Long ?: 0
                    return engine.render(new ModelAndView(commonModel << [
                            posts    : blogService.list(max, offset),
                            postCount: blogService.count(),
                            message  : flash(req, 'message'),
                            request  : req,
                    ], "admin/post/list"))
                }
                get "/publish", { req, res ->
                    def id = req.queryParams("postId") as Long
                    def post = blogService.findById(id)
                    def published = req.queryParams('publish').toBoolean()
                    post.set('is_published', published)
                    blogService.save(post)
                    flash(req, 'message', Messages.message(published ? 'admin.blog.post.published' : 'admin.blog.post.deleted'))
                    res.redirect('/admin/post/list')
                    return
                }
            }

            get "/ajaxListPublished", { req, res ->
                def max = req.queryParams("rowCount") as Long ?: 10
                def current = req.queryParams("current") as Long ?: 0
                def offset = (max * (current - 1))
                def searchString = req.queryParams("searchPhrase")
                def model = [
                        current: current,
                        rowCount: max,
                        rows: blogService.searchPublished(max, offset, searchString).collect{ it.attributes },
                        total: blogService.countSearchPublished(searchString),
                ]
                res.type("application/json")
                return JsonOutput.toJson(model)
            }

            get "/ajaxListDrafts", { req, res ->
                def max = req.queryParams("rowCount") as Long ?: 10
                def current = req.queryParams("current") as Long ?: 0
                def offset = (max * (current - 1))
                def searchString = req.queryParams("searchPhrase")

                def model = [
                        current: current,
                        rowCount: max,
                        rows: blogService.searchDrafts(max, offset, searchString).collect{ it.attributes },
                        total: blogService.countSearchDrafts(searchString),
                ]
                res.type("application/json")
                return JsonOutput.toJson(model)
            }

            get "/ajaxPublish", { req, res ->
                def id = req.queryParams("id") as Long
                def post = blogService.findById(id)
                def published = req.queryParams('publish').toBoolean()
                post.set('is_published', published)
                blogService.save(post)
                res.type("application/json")
                return JsonOutput.toJson([saved: true])
            }

            get "/dashboard", { req, res ->
                def model = [:]
                return engine.render(new ModelAndView(commonModel << model, "admin/dashboard"))
            }

            path "/user", {

                get "/list", { req, res ->
                    def max = req.queryParams("max") as Long ?: 10
                    def offset = req.queryParams("offset") as Long ?: 0
                    return engine.render(new ModelAndView(commonModel << [
                            users    : userService.list(max, offset),
                            userCount: userService.count(),
                            message  : flash(req, 'message'),
                            request  : req,
                    ], "admin/user/list"))
                }

                get "/edit", { req, res ->
                    def id = req.queryParams("id") as Long
                    def user = userService.findById(id) ?: User.create()
                    def model = [user: user, message: flash(req, 'message')]
                    return engine.render(new ModelAndView(commonModel << model, "admin/user/edit"))
                }

                post "/edit", { req, res ->
                    def id = req.queryParams("id") as Long
                    def user = userService.findById(id) ?: User.create()
                    user = RequestUtil.populateParams(User.class, req, user)
                    def password = req.queryMap().get('password').value()
                    if( password ) user.set('password', BCrypt.hashpw( password, BCrypt.gensalt() ))

                    user.validate()
                    if (user.valid) {
                        user = userService.save(user)
                        // for now, only one role and it's role admin by default
                        user.getAll(UserRole.class).each { UserRole ur ->
                            ur.delete()
                        }
                        user.add(UserRole.create('user_id', user.get('id') as Long, 'role_id', Role.where('authority = ?', Role.ROLE_ADMIN).first().get('id')))

                        flash(req, 'message', Messages.message('default.saved', 'User'))
                        res.redirect('/admin/user/edit?id=' + user.get('id'))
                    } else {
                        def model = [
                                user : user,
                                error: flash(req, 'error', Messages.message('default.errors')),
                        ]
                        return engine.render(new ModelAndView(commonModel << model, "admin/user/edit"))
                    }
                }

            }

            get "/ajaxSaveTag", { req, res ->
                def name = req.queryParams('tag')
                def exists = blogService.findTagByName(name)
                if (!exists) {
                    blogService.saveTag(Tag.create('name', name))
                }
                return JsonOutput.toJson([saved: true])
            }

            get "/ajaxListTags", { req, res ->
                def tags = blogService.listTags().collect { it.attributes }
                res.type("application/json")
                return JsonOutput.toJson(tags)
            }
        }

        path "/blog", {
            get "/feed", { req, res ->
                DateFormat pubDateFormatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                res.type("application/xml")
                def all = req.queryParams('all') ? true : false
                return engine.render(new ModelAndView(commonModel << [
                        posts    : all ? blogService.listPublic() : blogService.listPublic(25, 0),
                        formatter: pubDateFormatter,
                ], "feed"))

            }

            get "/post/:id", { req, res ->
                def id = req.params(":id") as Long
                def post = blogService.findById(id)
                def tags = post.getAll(PostTag.class).collect { it.parent(Tag.class) }.collect { it.name }
                return engine.render(new ModelAndView(commonModel << [post: post, tags: tags, pathInfo: req.pathInfo()], "post"))
            }
        }

        get "/", { req, res ->
            def max = req.queryParams("max") as Long ?: 10
            def offset = req.queryParams("offset") as Long ?: 0
            def posts = blogService.listPublic(max, offset)
            def totalPosts = blogService.countPublic()
            def model = [posts: posts, totalPosts: totalPosts, request: req]
            return engine.render(new ModelAndView(commonModel << model, "index"))
        }

        path "/page", {
            get "/index", { req, res ->
                def max = req.queryParams("max") as Long ?: 10
                def offset = req.queryParams("offset") as Long ?: 0
                def posts = blogService.listPublic(max, offset)
                def totalPosts = blogService.countPublic()
                def model = [posts: posts, totalPosts: totalPosts, request: req]
                return engine.render(new ModelAndView(commonModel << model, "index"))
            }

            get "/tagged/:tag", { req, res ->
                def max = req.queryParams("max") as Long ?: 10
                def offset = req.queryParams("offset") as Long ?: 0
                def tag = req.params(":tag")
                def posts = blogService.listTagged(tag, max, offset)
                def totalPosts = blogService.countTagged(tag)
                def model = [posts: posts, totalPosts: totalPosts, tag: tag, request: req]
                return engine.render(new ModelAndView(commonModel << model, "index"))
            }

            get "/about", { req, res ->
                res.redirect('/blog/post/2')
                return
            }

            post "/contact", { req, res ->
                def contact = new Contact(RequestUtil.toParams(req))
                if (!req.session().attribute('messagesSent')) {
                    req.session().attribute('messagesSent', 1)
                } else {
                    def sent = req.session().attribute('messagesSent')
                    req.session().attribute('messagesSent', sent + 1)
                }
                if (req.session().attribute('messagesSent') > 3) {
                    // fail silently - let them think they're spamming successfully
                    flash(req, 'message', Messages.message('contact.form.success'))
                    res.redirect('/page/contact')
                    return
                }
                contact.validate()
                if (contact.valid()) {
                    def msg = "Message from: ${contact.name} (${contact.email}).\n\nMessage: ${contact.comments}"
                    def result = notificationService.send(config.codes.recursive.email, config.codes.recursive.email, 'Blog Contact Page', msg)
                    flash(req, 'message', Messages.message('contact.form.success'))
                    res.redirect('/page/contact')
                    return
                } else {
                    return engine.render(new ModelAndView(commonModel << [
                            contact: contact,
                            error  : flash(req, 'error', Messages.message('default.errors')),
                    ], "contact"))
                }
                res.redirect('/page/contact')
                return
            }

            get "/contact", { req, res ->
                def model = [
                        contact: new Contact(RequestUtil.toParams(req)),
                        message: flash(req, 'message')
                ]
                return engine.render(new ModelAndView(commonModel << model, "contact"))
            }
        }

        get "/login", { req, res ->
            return engine.render(new ModelAndView(commonModel << [:], "login"))
        }

        post "/login", { req, res ->
            def username = req.queryParams('username')
            Boolean auth = userService.auth(username, req.queryParams('password'))
            req.session().attribute('isLoggedIn', auth)
            if (auth) {
                res.redirect('/admin/post/list')
                req.session().attribute('currentUserId', userService.findByUsername(username).get('id'))
                return
            }
            return engine.render(new ModelAndView(commonModel << [message: 'Username or password is incorrect'], "login"))
        }

        get "/logout", { req, res ->
            req.session().attribute('isLoggedIn', false)
            req.session().attribute('currentUserId', null)
            res.redirect('/page/index')
            return
        }

        get "/test-error", { req, res ->
            req.foo()
        }

        notFound { req, res ->
            return engine.render(new ModelAndView(commonModel << [:], "404"))
        }

        exception Exception.class, { e, req, res ->
            e.printStackTrace()
            flash(req, 'exception', e)
            //res.redirect('/500')
            //return
            // no redirect!  render the 500 page content directly into the response, so the URL doesn't change
            res.body(engine.render(new ModelAndView(commonModel << [exception: e], "500")))
        }

        get "/500", { req, res ->
            return engine.render(new ModelAndView(commonModel << [
                    exception: flash(req, 'exception')
            ], "500"))
        }

        get "/snippet", { req, res ->
            if( environment != 'dev' ) {
                // back the truck up
                res.redirect('/')
                return
            }
            def fileName = req.queryParams('fileName')
            def clazzName = fileName.tokenize('.').first()
            def clazz = Class.forName(clazzName)
            def lineNumber = req.queryParams('line') as Integer
            def packagePath = clazz?.package ? clazz?.package.toString().replace('.', File.pathSeparator) + '/' : ''
            def path = config.codes.recursive.sourcePath + packagePath + fileName
            File f = new File(path)
            FileInputStream fileInputStream = new FileInputStream(f)

            if( !f.exists() ) {
                return
            }
            def snip = ""
            try {
                fileInputStream.withReader { fileIn ->
                    def reader = new LineNumberReader(fileIn)
                    int last = lineNumber + 10
                    def range = (lineNumber - 10..last)
                    String currentLine = reader.readLine()

                    while (currentLine != null) {
                        int currentLineNumber = reader.lineNumber
                        if (currentLineNumber in range) {
                            boolean isErrorLine = currentLineNumber == lineNumber
                            if (isErrorLine) {
                                snip += '<b>' + currentLine + '</b>' + '\n'
                            }
                            else {
                                snip += currentLine + '\n'
                            }
                        }
                        else if (currentLineNumber > last) {
                            break
                        }
                        currentLine = reader.readLine()
                    }
                }
            }
            catch (e) {
            }
            finally {
                try {
                    input?.close()
                } catch (e) {
                }
            }
            return engine.render(new ModelAndView(commonModel << [snip: snip], "snippet"))

        }
    }
}