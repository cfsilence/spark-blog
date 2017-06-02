package codes.recursive.domain.blog

import codes.recursive.domain.BaseModel
import org.javalite.activejdbc.annotations.Table

@Table("post_tag")
class PostTag extends BaseModel {

	static belongsTo = [post: Post, tag: Tag]

}
