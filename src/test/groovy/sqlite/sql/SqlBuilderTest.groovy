package sqlite.sql

import spock.lang.Specification

/**
 * Created by thiago on 30/04/15.
 */
class SqlBuilderTest extends Specification {

    def "WhereClause"() {
        given:
        def sqlBuilder = new SqlBuilder()

        expect:
        clause == sqlBuilder.conditionClause(keyParam, valParam)

        where:
        keyParam           || valParam  || clause
        'name'             || "asd"     || "name = ?"
        '$between.age'     || "asd"     || "age BETWEEN ? AND ?"
        '$not.between.age' || "fsd"     || "age NOT BETWEEN ? AND ?"
        '$in.age'          || "sdf"     || "age IN (?)"
        '$not.in.age'      || "sdf,qwe" || "age NOT IN (?, ?)"
        '$lt.age'          || "hfd"     || "age < ?"
        '$gt.age'          || "asd"     || "age > ?"
        '$order.age'       || "gfd"     || ""
    }

    def "WhereData"() {
        given:
        def sqlBuilder = new SqlBuilder()

        expect:
        data == sqlBuilder.conditionData(keyParam, valParam)

        where:
        keyParam           || valParam  || data
        'name'             || "asd"     || ["asd"]
        'name'             || "asd,qwe" || ["asd,qwe"]
        '$between.age'     || "asd,fds" || ["asd", "fds"]
        '$not.between.age' || "fsd,asd" || ["fsd", "asd"]
        '$in.age'          || "sdf"     || ["sdf"]
        '$not.in.age'      || "sdf,qwe" || ["sdf", "qwe"]
        '$lt.age'          || "hfd"     || ["hfd"]
        '$gt.age'          || "asd"     || ["asd"]
        '$order.age'       || "gfd,qwe" || ["gfd,qwe"]
    }

    def "MountTable"() {
        given:
        def sqlBuilder = new SqlBuilder()

        expect:
        clause == sqlBuilder.mountTable(param)

        where:
        param         || clause
        'weird_table' || "FROM weird_table"
        'new_tab'     || "FROM new_tab"
    }

    def "MountSelect"() {
        given:
        def sqlBuilder = new SqlBuilder()

        expect:
        clause == sqlBuilder.mountSelect(param)

        where:
        param      || clause
        []         || "SELECT *"
        ['a', 'b'] || "SELECT a, b"
    }

    def "MountWhereClause"() {
        given:
        def sqlBuilder = new SqlBuilder()

        expect:
        clause == sqlBuilder.mountWhere(param).clause

        where:
        param                                 || clause
        [name: 'thiago']                      || "WHERE name = ?"
        [name: 'thiago', age: '22']           || "WHERE name = ? AND age = ?"
        [name: 'thiago', '$qt.age': '22']     || "WHERE name = ?" //non valid where prefix
        [name: 'thiago', '$gt.age': '22']     || "WHERE name = ? AND age > ?"
        [name: 'thiago', '$not.in.age': '22'] || "WHERE name = ? AND age NOT IN (?)"
        [name: 'thiago', '$in.age': '22,44']  || "WHERE name = ? AND age IN (?, ?)"
        ['$between.age': '22,44']             || "WHERE age BETWEEN ? AND ?"
        [:]                                   || ""
    }

    def "MountOrderClause"() {
        given:
        def sqlBuilder = new SqlBuilder()

        expect:
        clause == sqlBuilder.mountOrder(param)

        where:
        param                                          || clause
        [:]                                            || ""
        ['$order.age': 'asc']                          || "ORDER BY age ASC"
        ['$order.birth': 'desc']                       || "ORDER BY birth DESC"
        ['$order.name': 'asc', '$order.birth': 'desc'] || "ORDER BY name ASC, birth DESC"
        ['$order.name': '', '$order.birth': 'desc']    || "ORDER BY name, birth DESC"
    }

    def "MountWhereData"() {
        given:
        def sqlBuilder = new SqlBuilder()

        expect:
        data == sqlBuilder.mountWhere(param).data

        where:
        param                                 || data
        [name: 'thiago']                      || ['thiago']
        [name: 'outra, coisa']                || ['outra, coisa']
        [name: 'thiago', age: '22']           || ['thiago', '22']
        [name: 'thiago', '$qt.age': '22']     || ['thiago'] //non valid where prefix
        [name: 'thiago', '$gt.age': '22']     || ['thiago', '22']
        [name: 'thiago', '$not.in.age': '22'] || ['thiago', '22']
        [name: 'thiago', '$in.age': '22,44']  || ['thiago', '22', '44']
        ['$between.age': '22,44']             || ['22', '44']
    }

    def "FilterParams"() {
        given:
        def sqlBuilder = new SqlBuilder()

        expect:
        result == sqlBuilder.filterParams(params, allowed)

        where:
        params                                    | allowed         || result
        [name: 'thiago']                          | ['name']        || [name: 'thiago']
        [name: 'thiago', age: '22']               | ['age']         || [age: '22']
        [name: 'thiago', age: '22']               | []              || [name: 'thiago', age: '22']
        [name: 'thiago', '$qt.age': '22']         | ['age']         || ['$qt.age': '22'] //non valid where prefix
        [name: 'thiago', '$gt.age': '22']         | ['name', 'age'] || [name: 'thiago', '$gt.age': '22']
        [name: 'thiago', '$not.in.age': '22']     | ['name', 'age'] || [name: 'thiago', '$not.in.age': '22']
        [name: 'thiago', '$between.age': '22,44'] | ['name', 'age'] || [name: 'thiago', '$between.age': '22,44']

    }

    def "Query"() {
        given:
        def sqlBuilder = new SqlBuilder()

        expect:
        query == new SqlBuilder(table: table, columns: columns, params: params).queryAndData().query

        where:
        columns || table    || params        || query
        []      || 'my_tab' || [name: 'foo'] || "SELECT * FROM my_tab WHERE name = ?"
        []      || 'my_tab' || [name: 'foo'] || "SELECT * FROM my_tab WHERE name = ?"
    }
}
