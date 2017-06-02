package codes.recursive.domain

import org.javalite.activejdbc.annotations.Table

@Table("role")
class Role extends BaseModel {

	static final String ROLE_ADMIN = 'ROLE_ADMIN'
	static final String ROLE_AUTHOR = 'ROLE_AUTHOR'
	/*
	String authority
	*/
	static constraints = {
		authority blank: false, unique: true
	}

}
