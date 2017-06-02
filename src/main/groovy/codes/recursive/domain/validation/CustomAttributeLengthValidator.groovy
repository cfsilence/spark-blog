package codes.recursive.domain.validation

import org.javalite.activejdbc.Model
import org.javalite.activejdbc.validation.ValidatorAdapter
import org.javalite.activejdbc.validation.length.LengthOption

/**
 * Created by toddsharp on 4/17/17.
 */
class CustomAttributeLengthValidator extends ValidatorAdapter {

    private final String attribute
    private LengthOption lengthOption

    private CustomAttributeLengthValidator(String attribute) {
        this.attribute = attribute
    }

    static CustomAttributeLengthValidator on(String attribute) {
        return new CustomAttributeLengthValidator(attribute)
    }

    @Override
    void validate(Model m) {
        Object value = m.get(attribute)
        if (!value) {
            return
        }
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Attribute must be a String");
        }

        if (!lengthOption.validate((String) (m.get(attribute)))) {
            m.addValidator(this, attribute)
        }
    }

    CustomAttributeLengthValidator with(LengthOption lengthOption) {
        this.lengthOption = lengthOption
        setMessage(lengthOption.getParametrizedMessage())
        return this
    }

    @Override
    String formatMessage(Locale locale, Object... params) {
        return super.formatMessage(locale, lengthOption.getMessageParameters())
    }
}

