package codes.recursive

/**
 * Created by toddsharp on 4/18/17.
 */
class Errors {
    private Map _errors = [:]
    def get(field) {
        return _errors.containsKey(field) ? _errors[field] : null
    }

    def add(field, message) {
        if( !_errors.containsKey(field) ) _errors[field] = ''
        _errors[field] = message
    }
}