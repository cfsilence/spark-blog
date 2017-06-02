package codes.recursive.domain.blog

import codes.recursive.domain.BaseModel
import org.javalite.activejdbc.annotations.Table

@Table("tag")
class Tag extends BaseModel {
    /*
    String name
    Date dateCreated
    Date lastUpdated
    */

    static constraints = {
        name nullable: false, maxSize: 100, unique: true
    }
}
