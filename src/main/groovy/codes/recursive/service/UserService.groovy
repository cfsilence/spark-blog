package codes.recursive.service

import codes.recursive.domain.User
import org.mindrot.jbcrypt.BCrypt

/**
 * Created by toddsharp on 4/14/17.
 */
class UserService {

    User findById(Long id) {
        return User.findById(id)
    }

    def save(User user) {
        user.save()
        return user
    }

    User findByUsername(String username) {
        return User.where("username = ?", username).first()
    }

    Boolean auth(String username, String password) {
        User user = findByUsername(username)
        return BCrypt.checkpw(password, user.get('password'))
    }

    def list(Long max, Long offset) {
        User.findAll().limit(max).offset(offset)
    }

    def count() {
        return User.count()
    }
}
