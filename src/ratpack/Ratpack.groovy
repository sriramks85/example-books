import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import ratpack.example.books.Book
import ratpack.groovy.sql.SqlModule
import ratpack.h2.H2Module

import static ratpack.form.Forms.form
import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack

ratpack {
    modules {
        register new H2Module()
        register new SqlModule()
    }

    handlers { Sql sql ->

        sql.executeInsert("create table if not exists books (id int primary key auto_increment, title varchar(255), content varchar(255))")

        get {
            def books = sql.rows("select id, title, content from books order by id").collect { GroovyRowResult result ->
                new Book(result.id, result.title, result.content)
            }
            render groovyTemplate("listing.html", title: "Books", books: books, msg: request.queryParams.msg ?: "")
        }

        handler("create") {
            byMethod {
                get {
                    render groovyTemplate("create.html", title: "Create Book")
                }
                post {
                    def form = parse form()
                    def ids = sql.executeInsert("insert into books (title, content) values ($form.title, $form.content)")
                    def id = ids[0][0]
                    redirect "/?msg=Book+$id+created"
                }
            }
        }

        handler("update/:id") {
            def id = pathTokens.asLong("id")
            def row = sql.firstRow("select title, content from books where id = $id order by id")
            def book = new Book(id, row.title, row.content)
            if (row == null) {
                clientError(404)
            } else {
                byMethod {
                    get {
                        render groovyTemplate("update.html", title: "Update Book", book: book)
                    }
                    post {
                        def form = parse form()
                        sql.executeUpdate("update books set title = $form.title, content = $form.content where id = $id")
                        redirect "/?msg=Book+$id+updated"
                    }
                }
            }
        }

        post("delete/:id") {
            def id = pathTokens.id
            def row = sql.firstRow("select title, content from books where id = $id order by id")
            if (row == null) {
                clientError(404)
            } else {
                sql.executeUpdate("delete from books where id = $id")
                redirect "/?msg=Book+$id+deleted"
            }
        }

        assets "public"
    }

}