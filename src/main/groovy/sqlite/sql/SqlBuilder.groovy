package sqlite.sql

import groovy.transform.PackageScope
import groovy.transform.TypeChecked
import groovy.transform.builder.Builder

import static groovy.transform.PackageScopeTarget.FIELDS
import static groovy.transform.PackageScopeTarget.METHODS

/**
 * Created by thiago on 30/04/15.
 */
@TypeChecked
@Builder(builderClassName = "SqlBuilderCriteria", builderMethodName = "criteria", buildMethodName = "create")
@PackageScope([FIELDS, METHODS])
class SqlBuilder {
    private static final String BETWEEN = '^\\$(between|btw)\\.'
    private static final String NOT_BETWEEN = '^\\$not\\.(between|btw)\\.'
    private static final String LOWER_THAN = '^\\$lt\\.'
    private static final String GREATER_THAN = '^\\$gt\\.'
    private static final String LOWER_THAN_EQUAL = '^\\$lte\\.'
    private static final String GREATER_THAN_EQUAL = '^\\$gte\\.'
    private static final String IN = '^\\$in\\.'
    private static final String NOT_IN = '^\\$not\\.in\\.'
    private static final String EQ = '^\\$eq\\.'
    private static final String NOT_EQ = '^\\$not\\.eq\\.'
    private static final String STARTS = '^\\$starts\\.'

    private static final String ORDER = '^\\$(sort|order)\\.'

    String valueSep
    String defaultOperator
    String table
    List<String> columns
    List<String> groupColumns
    List<String> allowedParams
    Map<String, String> params
    Map<String, String> havingParams

    String conditionClause(String keyParam, String valueParam) {
        int totalParams = valueParam.split(getValueSep()).size()
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
            case ~(STARTS + '.+'): return "${keyParam.replaceAll(STARTS, '')} LIKE ?"
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
        List<String> args = valueParam.split(getValueSep()) as List<String>
        int totalParams = args.size()
        String btwMsg = "Between clause must have two arguments"
        switch (keyParam) {
            case ~(BETWEEN + '.+'): if (totalParams == 2) return args
            else throw new IllegalArgumentException(btwMsg)
            case ~(NOT_BETWEEN + '.+'): if (totalParams == 2) return args
            else throw new IllegalArgumentException(btwMsg)
            case ~(IN + '.+'): return args
            case ~(NOT_IN + '.+'): return args
            case ~(STARTS + '.+'): return [valueParam.replaceAll("%","")+"%"]
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
        [clause: ("${!result['clause'].isEmpty() ? condition : ''} " + result['clause'].join(" ${getDefaultOperator()} ")).trim(), data: result['data'].flatten()]
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

    Map<String, String> filterParams(Map<String, String> params, List<String> allowedParams) {
        allowedParams.isEmpty() ? params : params.findAll { k, v -> allowedParams.any { k.endsWith(it) } }
    }

    public Map<String, ?> queryAndData() {
        Map filtered = filterParams(getParams(), getAllowedParams())
        Map where = mountWhere(filtered)
        Map having = mountHaving(getHavingParams())
        String query = [mountSelect(getColumns()), mountTable(getTable()),
                        where['clause'], mountGroup(getGroupColumns()),
                        having['clause'], mountOrder(filtered)].findAll().join(' ').toString()
        [query: query, data: ((where['data'] as List<String>) + (having['data'] as List<String>))]
    }

    public String getValueSep() {
        valueSep ?: ","
    }

    public String getDefaultOperator() {
        defaultOperator ?: "AND"
    }

    public String getTable() {
        table ?: "<table>"
    }

    public List<String> getColumns() {
        columns ?: []
    }

    public List<String> getGroupColumns() {
        groupColumns ?: []
    }

    public List<String> getAllowedParams() {
        allowedParams ?: []
    }

    public Map<String, String> getParams() {
        params ?: [:]
    }

    public Map<String, String> getHavingParams() {
        havingParams ?: [:]
    }
}
