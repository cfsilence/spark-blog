package codes.recursive.domain

import org.javalite.activejdbc.annotations.Table
import org.javalite.activejdbc.validation.UniquenessValidator

@Table("user")
class User extends BaseModel {
	static{
		validatePresenceOf('password', 'username', 'first_name', 'last_name').message('default.value.missing')
		validateWith(new UniquenessValidator("username")).message('admin.user.not.unique')
	}
}
