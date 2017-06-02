package codes.recursive.util

import org.javalite.activejdbc.Model
import spark.QueryParamsMap
import spark.Request

/**
 * Created by toddsharp on 4/18/17.
 */
class RequestUtil {
    static Map toParams(Request req) {
        return req.queryMap().toMap().collectEntries { k, v ->
            [(k): req.queryMap().get(k).value()]
        }
    }

    static def populateParams(Class clazz, Request req, Model instance) {
        def cols = clazz.getMetaModel().columnMetadata

        cols.each { it ->
            def col = it.value.columnName
            def exists = req.queryMap().get(col).value()
            if( exists?.size() ) {
                /* this switch will obviously grow in time */
                switch (it.value.typeName) {
                    case 'INT':
                    case 'BIT':
                        instance.set(col, req.queryMap().get(col).integerValue())
                        break;
                    case 'DATETIME':
                        instance.set(col, new Date(req.queryMap().get(col).value()))
                        break;
                    case 'VARCHAR':
                    case 'NVARCHAR':
                    case 'LONGTEXT':
                    case 'TEXT':
                        instance.set(col, req.queryMap().get(col).value())
                        break;
                }
            }
        }
        return instance
    }
}
