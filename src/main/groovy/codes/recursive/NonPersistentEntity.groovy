package codes.recursive

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by toddsharp on 4/18/17.
 */
class NonPersistentEntity {
    Errors _errors = new Errors()

    def get(field) {
        return this[field]
    }

    def errors() {
        return _errors
    }

    def valid() {
        return _errors._errors.keySet().size() == 0
    }

    Boolean isValidEmail(email) {
        def rule = "\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}\\b"
        def pattern = Pattern.compile(rule, Pattern.CASE_INSENSITIVE)
        Matcher matcher = pattern.matcher((String) email)
        if(!matcher.matches()){
            return false
        }
        return true
    }

    def validate(){
    }
}
