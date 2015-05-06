package sqlite.sql

import groovy.sql.Sql
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by thiago on 30/04/15.
 */
class SqlBuilderExecTest extends Specification {
    @Shared
    def sql = Sql.newInstance("jdbc:h2:mem:", "org.h2.Driver")

    def setupSpec() {
        sql.execute("create table my_tab (id int primary key, name varchar(255), age int, birth date)")
        sql.execute("insert into my_tab values (1, 'foo', 17, DATE '1998-05-01'), (2, 'bar', 44, DATE '1998-05-01'), (3, 'zaz', 29, DATE '1986-05-01')")
    }

    def "WhereClause"() {
        given:
        Map result
        when:
        result = new SqlBuilder(table: table, columns: columns, params: params).queryAndData()
        then:
        rows == sql.rows(result.query, result.data).size

        where:
        columns      | table                                         | params                                   || rows
        []           | 'my_tab'                                      | [name: 'foo']                            || 1
        ['name']     | 'my_tab'                                      | ['$gt.age': '20', '$order': 'name']      || 2
        ['name']     | 'my_tab'                                      | ['$gt.age': '20', '$order.birth': 'asc'] || 2
        ['name']     | 'my_tab'                                      | ['$gt.age': '20', '$order.age': 'desc']  || 2
        []           | 'my_tab'                                      | ['$in.age': '29,44']                     || 2
        ['1 as zaz'] | 'my_tab a inner join my_tab b on a.id = b.id' | ['$lte.a.birth': '2015-05-01']           || 3
    }

}
