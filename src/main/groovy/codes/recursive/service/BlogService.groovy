package codes.recursive.service

import codes.recursive.domain.blog.Post
import codes.recursive.domain.blog.Tag
import org.javalite.activejdbc.Base

/**
 * Created by toddsharp on 4/14/17.
 */
class BlogService {

    def findById(Long id) {
        return Post.findById(id)
    }

    def save(Post post) {
        post.save()
        return post
    }

    def listTagged(String tag, Long max, Long offset) {
        def qry = """
            select 
                p.article,
                p.authored_by_id,
                p.date_created,
                p.id,
                p.is_published,
                p.keywords,
                p.last_updated,
                p.published_date,
                p.summary,
                p.title,
                p.`version`,
                pt.post_id,
                pt.tag_id,
                u.account_expired,
                u.account_locked,
                u.date_created,
                u.enabled,
                u.first_name,
                u.last_name,
                u.last_updated,
                u.`password`,
                u.password_expired,
                u.username,
                u.`version`
            from post p
                left outer join post_tag pt on pt.post_id = p.id
                left outer join tag t on t.id = pt.tag_id
                inner join user u on p.authored_by_id = u.id
            where t.name = ? 
            and p.published_date <= now() 
            and p.is_published = ?        
            order by published_date desc
            LIMIT ?,?
        """
        return Post.findBySQL(qry, tag, 1, offset, max)
    }

    def countTagged(String tag) {
        def qry = """
            select count('x') as total
            from post p
                left outer join post_tag pt on pt.post_id = p.id
                left outer join tag t on t.id = pt.tag_id
            where t.name = ? 
            and p.published_date <= now()
            and p.is_published = ?        
        """
        def result = Base.findAll(qry, tag, 1)
        return result.size() ? result.first().total : 0
    }

    def list(Long max, Long offset) {
        Post.findAll().orderBy('published_date desc').limit(max).offset(offset)
    }

    def count() {
        return Post.count()
    }

    def listPublic(Long max, Long offset) {
        Post.where('is_published = 1 and published_date <= ?', new Date()).limit(max).offset(offset).orderBy('published_date desc')
    }

    def searchPublished(Long max, Long offset, String searchString=null) {
        if( searchString ) {
            return Post.where('lcase(title) like ? and is_published = 1', "%${searchString}%".toString().toLowerCase()).limit(max).offset(offset).orderBy('published_date desc')
        }
        else {
            Post.where('is_published = 1').limit(max).offset(offset).orderBy('published_date desc')
        }
    }

    def countSearchPublished(String searchString=null) {
        if( searchString ) {
            return Post.count('lcase(title) like ? and is_published = 1', "%${searchString}%".toString().toLowerCase())
        }
        else {
            Post.count('is_published = 1')
        }
    }

    def searchDrafts(Long max, Long offset, String searchString=null) {
        if( searchString ) {
            return Post.where('lcase(title) like ? and is_published = 0', "%${searchString}%".toString().toLowerCase()).limit(max).offset(offset).orderBy('published_date desc')
        }
        else {
            return listDrafts(max, offset)
        }
    }

    def countSearchDrafts(String searchString=null) {
        if( searchString ) {
            return Post.count('lcase(title) like ? and is_published = 0', "%${searchString}%".toString().toLowerCase())
        }
        else {
            return countDrafts()
        }
    }

    def listDrafts(Long max, Long offset) {
        Post.where('is_published = 0').limit(max).offset(offset).orderBy('date_created desc')
    }

    def listDrafts() {
        Post.where('is_published = 0').orderBy('published_date desc')
    }

    def listPublic() {
        Post.where('is_published = 1 and published_date <= ?', new Date()).orderBy('published_date desc')
    }

    def countPublic() {
        Post.count('is_published = 1 and published_date <= ?', new Date())
    }

    def countDrafts() {
        Post.count('is_published = 0')
    }

    def listTags() {
        return Tag.findAll().orderBy('name')
    }

    def findTagByName(String name) {
        return Tag.where('name = ?', name)
    }

    def findTagById(Long id) {
        return Tag.findById(id)
    }

    def saveTag(Tag tag) {
        return tag.save()
    }
}
