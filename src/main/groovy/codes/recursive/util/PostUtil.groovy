package codes.recursive.util

import codes.recursive.domain.Role
import codes.recursive.domain.User
import codes.recursive.domain.blog.Post

/**
 * Created by toddsharp on 4/17/17.
 */
class PostUtil {

    static Boolean isPublicallyAvailable(Post post) {
        return post.get('is_published') && post.getDate('published_date').before(new Date())
    }

    static Boolean canBeViewedBy(Post post, User user) {
        if( user.getAll(Role.class).find{ it.get('authority') == Role.ROLE_ADMIN } ) return true
        if (!isPublicallyAvailable(post)) return false
        return true
    }
}
