package com.example.tareas.data

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteQueryBuilder

/**
 * ContentProvider personalizado para exponer la base de datos local de tareas a otras aplicaciones (App B).
 * Cumple con el Punto 3 del Taller de Persistencia de la Universidad de Nariño.
 */
class TareaContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.example.tareas.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/tareas")

        const val TABLE_NAME = "tareas"

        private const val CODE_TAREAS = 1
        private const val CODE_TAREA_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "tareas", CODE_TAREAS)
            addURI(AUTHORITY, "tareas/#", CODE_TAREA_ID)
        }
    }

    private lateinit var database: AppDatabase

    override fun onCreate(): Boolean {
        context?.let {
            database = AppDatabase.getDatabase(it)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val db = database.openHelper.readableDatabase
        val match = uriMatcher.match(uri)

        val queryBuilder = SupportSQLiteQueryBuilder.builder(TABLE_NAME)
            .columns(projection)
            .orderBy(sortOrder ?: "fecha_creacion DESC")

        when (match) {
            CODE_TAREAS -> {
                val args: Array<Any?>? = selectionArgs?.map { it as Any? }?.toTypedArray()
                queryBuilder.selection(selection, args)
            }
            CODE_TAREA_ID -> {
                val id = ContentUris.parseId(uri)
                val finalSelection = if (selection.isNullOrEmpty()) "id = ?" else "id = ? AND ($selection)"
                val argsList = mutableListOf<Any?>(id)
                selectionArgs?.forEach { argsList.add(it) }
                queryBuilder.selection(finalSelection, argsList.toTypedArray())
            }
            else -> throw IllegalArgumentException("URI no soportada: $uri")
        }

        val cursor = db.query(queryBuilder.create())
        cursor.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uriMatcher.match(uri) != CODE_TAREAS) {
            throw IllegalArgumentException("URI inválida para inserción: $uri")
        }
        val db = database.openHelper.writableDatabase
        val id = db.insert(TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values ?: ContentValues())
        if (id > 0) {
            val itemUri = ContentUris.withAppendedId(CONTENT_URI, id)
            context?.contentResolver?.notifyChange(uri, null)
            context?.contentResolver?.notifyChange(itemUri, null)
            return itemUri
        }
        return null
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val db = database.openHelper.writableDatabase
        val contentValues = values ?: ContentValues()
        val count = when (uriMatcher.match(uri)) {
            CODE_TAREAS -> {
                val args: Array<Any?>? = selectionArgs?.map { it as Any? }?.toTypedArray()
                db.update(TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, contentValues, selection, args)
            }
            CODE_TAREA_ID -> {
                val id = ContentUris.parseId(uri)
                val finalSelection = if (selection.isNullOrEmpty()) "id = ?" else "id = ? AND ($selection)"
                val argsList = mutableListOf<Any?>(id)
                selectionArgs?.forEach { argsList.add(it) }
                db.update(TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, contentValues, finalSelection, argsList.toTypedArray())
            }
            else -> throw IllegalArgumentException("URI no soportada para actualización: $uri")
        }
        if (count > 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }
        return count
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = database.openHelper.writableDatabase
        val count = when (uriMatcher.match(uri)) {
            CODE_TAREAS -> {
                val args: Array<Any?>? = selectionArgs?.map { it as Any? }?.toTypedArray()
                db.delete(TABLE_NAME, selection, args)
            }
            CODE_TAREA_ID -> {
                val id = ContentUris.parseId(uri)
                val finalSelection = if (selection.isNullOrEmpty()) "id = ?" else "id = ? AND ($selection)"
                val argsList = mutableListOf<Any?>(id)
                selectionArgs?.forEach { argsList.add(it) }
                db.delete(TABLE_NAME, finalSelection, argsList.toTypedArray())
            }
            else -> throw IllegalArgumentException("URI no soportada para eliminación: $uri")
        }
        if (count > 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }
        return count
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            CODE_TAREAS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.tareas"
            CODE_TAREA_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.tareas"
            else -> null
        }
    }
}
