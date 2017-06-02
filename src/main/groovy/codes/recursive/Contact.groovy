package codes.recursive

import org.javalite.activejdbc.Messages
import org.javalite.activejdbc.validation.EmailValidator
import org.javalite.activejdbc.validation.ValidationBuilder

class Contact extends NonPersistentEntity {
    String name
    String email
    String comments

    @Override
    def validate(){
        if( !name ) {
            errors().add('name', Messages.message('default.value.missing'))
        }
        if( !email ) {
            errors().add('email', Messages.message('default.value.missing'))
        }
        if( !comments ) {
            errors().add('comments', Messages.message('default.value.missing'))
        }
        if( name && name.size() > 100 ) {
            errors().add('name', Messages.message('max.length', 100))
        }
        if( email && email.size() > 250 ) {
            errors().add('email', Messages.message('max.length', 250))
        }
        if( comments && comments.size() > 4000 ) {
            errors().add('comments', Messages.message('max.length', 4000))
        }
        if( email ) {
            if( !isValidEmail(email) ) {
                errors().add('email', Messages.message('invalid.email'))
            }
        }
    }

}

