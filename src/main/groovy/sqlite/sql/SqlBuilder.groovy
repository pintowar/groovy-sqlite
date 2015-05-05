package sqlite.sql

import groovy.transform.TypeChecked

/**
 * Created by thiago on 30/04/15.
 */
@TypeChecked
class SqlBuilder {
    private static final String BETWEEN = '^\\$between\\.'
    private static final String NOT_BETWEEN = '^\\$not\\.between\\.'
    private static final String LOWER_THAN = '^\\$lt\\.'
    private static final String GREATER_THAN = '^\\$gt\\.'
    private static final String LOWER_THAN_EQUAL = '^\\$lte\\.'
    private static final String GREATER_THAN_EQUAL = '^\\$gte\\.'
    private static final String IN = '^\\$in\\.'
    private static final String NOT_IN = '^\\$not\\.in\\.'
    private static final String EQ = '^\\$eq\\.'
    private static final String NOT_EQ = '^\\$not\\.eq\\.'

    private static final String ORDER = '^\\$order\\.'

    String valueSep = ','
    String defaultOperator = "AND"
    String table
    List<String> columns = []
    List<String> groupColumns = []
    Map<String, String> params = [:]
    Map<String, String> havingParams = [:]

    String conditionClause(String keyParam, String valueParam) {
        int totalParams = valueParam.split(valueSep).size()
        switch (keyParam) {
            case ~(BETWEEN + '.+'): return "${keyParam.replaceAll(BETWEEN, '')} BETWEEN ? AND ?"
            case ~(NOT_BETWEEN + '.+'): return "${keyParam.replaceAll(NOT_BETWEEN, '')} NOT BETWEEN ? AND ?"
            case ~(LOWER_THAN + '.+'): return "${keyParam.replaceAll(LOWER_THAN, '')} < ?"
            case ~(GREATER_THAN + '.+'): return "${keyParam.replaceAll(GREATER_THAN, '')} > ?"
            case ~(LOWER_THAN_EQUAL + '.+'): return "${keyParam.replaceAll(LOWER_THAN_EQUAL, '')} <= ?"
            case ~(GREATER_THAN_EQUAL + '.+'): return "${keyParam.replaceAll(GREATER_THAN_EQUAL, '')} >= ?"
            case ~(IN + '.+'): return "${keyParam.replaceAll(IN, '')} IN (${(['?'] * totalParams).join(', ')})"
            case ~(NOT_IN + '.+'): return "${keyParam.replaceAll(NOT_IN, '')} NOT IN (${(['?'] * totalParams).join(', ')})"
            case ~(EQ + '.+'): return "${keyParam.replaceAll(EQ, '')} IS ?"
            case ~(NOT_EQ + '.+'): return "${keyParam.replaceAll(NOT_EQ, '')} IS NOT ?"
            case ~('^\\$.*'): return ""

            default: return "$keyParam = ?"
        }
    }

    String orderClause(String keyParam, String valueParam) {
        String order = valueParam.trim().isEmpty() ? '' : valueParam.toLowerCase() == 'desc' ? "DESC" : "ASC"
        switch (keyParam) {
            case ~(ORDER + '.+'): return "${keyParam.replaceAll(ORDER, '')} ${order}".trim()

            default: return ""
        }
    }

    List<String> conditionData(String keyParam, String valueParam) {
        List<String> args = valueParam.split(valueSep) as List<String>
        int totalParams = args.size()
        String btwMsg = "Between clause must have two arguments"
        switch (keyParam) {
            case ~(BETWEEN + '.+'): if (totalParams == 2) return args
            else throw new IllegalArgumentException(btwMsg)
            case ~(NOT_BETWEEN + '.+'): if (totalParams == 2) return args
            else throw new IllegalArgumentException(btwMsg)
            case ~(IN + '.+'): return args
            case ~(NOT_IN + '.+'): return args
            case ~('^\\$.*'): return [valueParam]

            default: return [valueParam]
        }
    }

    Map<String, ?> mountCondition(String condition, Map<String, String> params) {
        Map<String, List<String>> result = (params.inject([clause: [], data: []]) { acc, entry ->
            String aux = conditionClause(entry.key, entry.value)
            [clause: (aux.isEmpty() ? acc['clause'] : (acc['clause'] + [aux])),
             data  : (aux.isEmpty() ? acc['data'] : (acc['data'] + conditionData(entry.key, entry.value)))]
        } as Map<String, List<String>>)
        [clause: ("${!result['clause'].isEmpty() ? condition : ''} " + result['clause'].join(" ${defaultOperator} ")).trim(), data: result['data'].flatten()]
    }

    Map<String, ?> mountWhere(Map<String, String> params) { mountCondition("WHERE", params) }

    String mountTable(String table) {
        "FROM ${table}"
    }

    String mountSelect(List<String> columns) {
        "SELECT ${columns.isEmpty() ? '*' : columns.join(', ')}"
    }

    String mountGroup(List<String> columns) {
        columns.isEmpty() ? "" : "GROUP BY ${columns.join(', ')}"
    }

    Map<String, ?> mountHaving(Map<String, String> params) {
        params.isEmpty() ? [clause: "", data: []] : mountCondition("HAVING", params)
    }

    String mountOrder(Map<String, String> params) {
        List<String> result = (params.inject([]) { acc, entry ->
            String aux = orderClause(entry.key, entry.value)
            aux ? (acc + [aux]) : acc
        }.findAll() as List<String>)
        result.isEmpty() ? "" : "ORDER BY ${result.join(', ')}"
    }

    Map<String, ?> queryAndData() {
        Map where = mountWhere(params)
        Map having = mountHaving(havingParams)
        String query = [mountSelect(this.columns), mountTable(this.table),
                        where['clause'], mountGroup(groupColumns),
                        having['clause'], mountOrder(params)].findAll().join(' ').toString()
        [query: query, data: ((where['data'] as List<String>) + (having['data'] as List<String>))]
    }
}
