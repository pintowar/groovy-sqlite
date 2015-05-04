# groovy-sqlite

Library for Groovy that mount queries with a simple configuration.

This library assumes you know exactly what 

# Versions

## 0.1

* JDK 1.5 or higher
* Requires [Groovy 2.4.0] (http://www.groovy-lang.org/) or higher

## SQL

### Example

``` groovy

import sqlite.sql.*

def sql = Sql.newInstance("jdbc:h2:mem:", "org.h2.Driver")
sql.execute("create table my_tab (id int primary key, name varchar(255), age int, birth date)")
sql.execute("insert into my_tab values (1, 'foo', 17, DATE '1998-05-01'), (2, 'bar', 44, DATE '1998-05-01'), (3, 'zaz', 29, DATE '1986-05-01')")

def result = new SqlBuilder(table: 'my_tab', params: ['$gt.age': '20', '$order.desc': 'age']).queryAndData()

assert result.query == "SELECT * FROM my_tab WHERE age > ? ORDER BY age DESC"
assert result.data == ['20']

assert sql.rows(result.query, result.data).size == 2
```