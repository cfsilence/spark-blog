package codes.recursive.domain.blog

import codes.recursive.domain.BaseModel
import codes.recursive.domain.Role
import codes.recursive.domain.User
import codes.recursive.domain.validation.CustomAttributeLengthValidator
import org.javalite.activejdbc.annotations.BelongsTo
import org.javalite.activejdbc.annotations.Table
import org.javalite.activejdbc.validation.length.AttributeLengthValidator
import org.javalite.activejdbc.validation.length.Max

@Table("post")
@BelongsTo(parent = User.class, foreignKeyName = "authored_by_id")
class Post extends BaseModel {
    static {
        validatePresenceOf('title', 'article', 'authored_by_id', 'published_date').message('default.value.missing')
        validateWith( new CustomAttributeLengthValidator('title').with(new Max(500))).message('max.length')
        validateWith( new CustomAttributeLengthValidator('keywords').with(new Max(500))).message('max.length')
        validateWith( new CustomAttributeLengthValidator('summary').with(new Max(500))).message('max.length')
    }
}
