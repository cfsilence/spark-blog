package codes.recursive.util

import codes.recursive.domain.User

import java.text.BreakIterator
import java.text.SimpleDateFormat

/**
 * Created by toddsharp on 4/14/17.
 */
class FormattingUtil {

    static String truncatePost(String article, Integer contentLength=150) {
        String result
        def content = article
        content = content.replaceAll(/<!--.*?-->/, '').replaceAll(/<.*?>/, '').replaceAll('\\[(.*?)\\]', '')
        //Is content > than the contentLength?
        if (content.size() > contentLength) {
            BreakIterator bi = BreakIterator.getWordInstance()
            bi.setText(content)
            def first_after = bi.following(contentLength)
            result = content.substring(0, first_after) + "..."
        } else {
            result = content
        }
        return result.size() ? result : 'No preview available...'
    }

    static String formatFullName(User user) {
        return user.get('first_name') + ' ' + user.get('last_name')
    }

    static String dateFormat(Date date, String format) {
        return new SimpleDateFormat(format).format(date)
    }

    static String gist(id, githubUser) {
        return """<script src="https://gist.github.com/${githubUser}/${id}.js"></script>"""
    }

    static String spoiler(label, content) {
        return  """<div><a href="#" class="showSpoiler">${label ?: 'Spoiler'}</a><div class="spoiler">${content}</div></div>"""
    }

    static String youtube(id) {
        return """<iframe width="854" height="480" src="https://www.youtube.com/embed/${id}" frameborder="0" allowfullscreen></iframe>"""
    }

    static String render(String post, Map config) {
        def ret = ""
        def js = """
            \$().ready(function(){
                \$('.spoiler').hide()
                \$(document).on('click', '.showSpoiler', function(){
                    \$(this).closest('div').find('.spoiler').show()
                    \$(this).hide()
                    return false
                })
            })
        """
        ret += "<script>${js}</script>"
        ret += post
        ret = ret.replaceAll("\\[spoiler(.*?)\\](.*?)\\[/spoiler\\]", { full, label, content -> spoiler(label.tokenize('=').last(), content) })
        ret = ret.replaceAll("\\[gist(.*?)\\]", { full, word -> gist(word.tokenize('=').last(), config.codes.recursive.github.user) })
        ret = ret.replaceAll("\\[youtube(.*?)\\]", { full, word -> youtube(word.tokenize('=').last()) })
        return ret
    }

    static String adsense() {
        return """
            <script async src="//pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"></script>
            <!-- recursive-codes-responsive-1 -->
            <ins class="adsbygoogle"
                 style="display:block"
                 data-ad-client="ca-pub-1584600607873672"
                 data-ad-slot="5366767286"
                 data-ad-format="auto"></ins>
            <script>
            (adsbygoogle = window.adsbygoogle || []).push({});
            </script>
        """
    }

    static String disqus(String id, String serverURL, String pathInfo, String environment) {
        def url = serverURL + pathInfo
        return """
            <div id="disqus_thread"></div>
            <script>

            /**
            *  RECOMMENDED CONFIGURATION VARIABLES: EDIT AND UNCOMMENT THE SECTION BELOW TO INSERT DYNAMIC VALUES FROM YOUR PLATFORM OR CMS.
            *  LEARN WHY DEFINING THESE VARIABLES IS IMPORTANT: https://disqus.com/admin/universalcode/#configuration-variables*/
            var disqus_config = function () {
                this.page.url = '${url}';  // Replace PAGE_URL with your page's canonical URL variable
                this.page.identifier = '${id}'; // Replace PAGE_IDENTIFIER with your page's unique identifier variable
            };
            (function() { // DON'T EDIT BELOW THIS LINE
            var d = document, s = d.createElement('script');
            s.src = '//${environment == 'dev' ? 'local-recursive-codes' : 'recursive-codes'}.disqus.com/embed.js';
            s.setAttribute('data-timestamp', +new Date());
            (d.head || d.body).appendChild(s);
            })();
            </script>
            <noscript>Please enable JavaScript to view the <a href="https://disqus.com/?ref_noscript">comments powered by Disqus.</a></noscript>
        """
    }

    static String googleAnalytics() {
        def js = """
              (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
              (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
              m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
              })(window,document,'script','https://www.google-analytics.com/analytics.js','ga');
    
              ga('create', 'UA-1124632-10', 'auto');
              ga('send', 'pageview');
            """
        return "<script>${js}</script>"
    }

    static String makeDescription(String article) {
        def truncated = truncatePost(article)
        truncated = truncated.replaceAll('[\\r\\n]', ' ')
        return truncated
    }

    static String link(String href, Map linkParams, Map attributes, String text) {
        def ats = attributes.collect{ k, v-> k + '="' + v.toString() + '"' }
        def link = '<a ' + 'href="' + href + '?' + linkParams.collect { it }.join('&') + '"' + ats.join(' ') + '>' + text + '</a>'
        return link
    }

    static String paginate(total, request) {
        def output = ''
        if (!total) {
            new Exception("Paginate is missing required attribute [total]")
        }
        def offset = request.queryParams('offset') ? request.queryParams('offset') as Integer : 0
        def max = request.queryParams('max') ? request.queryParams('max') as Integer : 10

        def maxsteps = 10

        def linkParams = [:]
        linkParams.putAll(RequestUtil.toParams(request))
        linkParams.offset = offset - max
        linkParams.max = max
        if (request.queryParams('sort')) linkParams.sort = request.queryParams('sort')
        if (request.queryParams('order')) linkParams.order = request.queryParams('order')

        def linkTagAttrs = [:]

        output += '<ul class="pagination">'

        // determine paging variables
        def steps = maxsteps > 0
        int currentstep = (offset / max) + 1
        int firststep = 1
        int laststep = Math.round(Math.ceil(total / max))

        // display previous link when not on firststep unless omitPrev is true
        if (currentstep > firststep) {
            linkTagAttrs.class = 'prevLink'
            linkParams.offset = offset - max
            output += '<li>' + link(request.pathInfo(), linkParams, linkTagAttrs.clone(), 'Previous') + '</li>'
        }

        // display steps when steps are enabled and laststep is not firststep
        if (steps && laststep > firststep) {
            linkTagAttrs.class = 'step'

            // determine begin and endstep paging variables
            int beginstep = currentstep - Math.round(maxsteps / 2) + (maxsteps % 2)
            int endstep = currentstep + Math.round(maxsteps / 2) - 1

            if (beginstep < firststep) {
                beginstep = firststep
                endstep = maxsteps
            }
            if (endstep > laststep) {
                beginstep = laststep - maxsteps + 1
                if (beginstep < firststep) {
                    beginstep = firststep
                }
                endstep = laststep
            }

            // display firststep link when beginstep is not firststep
            if (beginstep > firststep) {
                linkParams.offset = 0
                output += '<li>' + link(request.pathInfo(), linkParams, linkTagAttrs.clone(), firststep.toString()) + '</li>'
            }

            // display paginate steps
            (beginstep..endstep).each { i ->
                if (currentstep == i) {
                    output += "<li class=\"active\"><a href=\"#\">${i.toString()}</a></li>"
                } else {
                    linkParams.offset = (i - 1) * max
                    output += '<li>' + link(request.pathInfo(), linkParams, linkTagAttrs.clone(), i.toString()) + '</li>'
                }
            }

            // display laststep link when endstep is not laststep
            if (endstep < laststep) {
                linkParams.offset = (laststep - 1) * max
                output += '<li>' + link(request.pathInfo(), linkParams, linkTagAttrs.clone(), laststep) + '</li>'
            }
        }

        // display next link when not on laststep unless omitNext is true
        if (currentstep < laststep) {
            linkTagAttrs.class = 'nextLink'
            linkParams.offset = offset + max
            output += '<li>' + link(request.pathInfo(), linkParams, linkTagAttrs.clone(), 'Next') + '</li>'
        }

        output += '</ul>'
        return output
    }

    static Boolean stringContains(String str, String subStr) {
        return str?.toLowerCase()?.contains(subStr?.toLowerCase())
    }
}
