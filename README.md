[![Download](https://api.bintray.com/packages/pintowar/maven/groovy-sqlite/images/download.svg) ](https://bintray.com/pintowar/maven/groovy-sqlite/_latestVersion)
[![Build Status](https://travis-ci.org/pintowar/groovy-sqlite.svg?branch=master)](https://travis-ci.org/pintowar/groovy-sqlite)
[![Coverage Status](https://coveralls.io/repos/pintowar/groovy-sqlite/badge.svg?branch=master)](https://coveralls.io/r/pintowar/groovy-sqlite?branch=master)

# groovy-sqlite

Library for Groovy that mount queries with a simple configuration.

This library assumes you know exactly what 

# Versions

## 0.1.x

* JDK 1.6 or higher
* Requires [Groovy 2.4.0] (http://www.groovy-lang.org/) or higher

## SQL

### Example

``` groovy
@Grab(group="com.h2database", module='h2', version='1.4.182')
@Grab(group='com.github.groovy-sqlite', module='groovy-sqlite', version='0.1.5')
@GrabConfig(systemClassLoader=true)
import sqlite.sql.*
import groovy.sql.Sql

def sql = Sql.newInstance("jdbc:h2:mem:", "org.h2.Driver")
sql.execute("create table my_tab (id int primary key, name varchar(255), age int, birth date)")
sql.execute("insert into my_tab values (1, 'foo', 17, DATE '1998-05-01'), (2, 'bar', 44, DATE '1998-05-01'), (3, 'zaz', 29, DATE '1986-05-01')")

def result = new SqlBuilder(table: 'my_tab', params: ['$gt.age': '20', '$order.name':'']).queryAndData()

assert result.query == "SELECT * FROM my_tab WHERE age > ? ORDER BY name"
assert result.data == ['20']

assert sql.rows(result.query, result.data).size == 2
```
