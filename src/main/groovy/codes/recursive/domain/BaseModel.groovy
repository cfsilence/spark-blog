package codes.recursive.domain

import org.javalite.activejdbc.Model

/**
 * Created by toddsharp on 4/14/17.
 */
abstract class BaseModel extends Model {
    BaseModel() {
        def mc = new ExpandoMetaClass(BaseModel, false, true)
        mc.initialize()
        this.metaClass = mc
    }

    def methodMissing(String name, args) {
        // Intercept method that starts with get|set.
        def prop = toUnderscore( name.replaceAll(/^(get|set)/, '') )

        if (name.startsWith("get")) {
            def result = this.get(prop)
            // Add new method to class with metaClass to reduce future lookups
            this.metaClass."$name" = { -> result }
            result
        }
        else if (name.startsWith("set"))
        {
            this.set(prop, args.size() ? args[0] : null)
        }
        else {
            throw new MissingMethodException(name, this.class, args)
        }
    }

    def toUnderscore = { it ->
        it.replaceAll( /([A-Z])/, /_$1/ ).toLowerCase().replaceAll( /^_/, '' )
    }

    @Override
    void beforeSave(){
        def cols = this.class.getMetaModel().columnMetadata
        if( cols.keySet().find{ it == 'date_created' } ) {
            if( !this.get('date_created') ) {
                this.set('date_created', new Date())
            }
        }
        if( cols.keySet().find{ it == 'last_updated' } ) {
            this.set('last_updated', new Date())
        }

    }
}
