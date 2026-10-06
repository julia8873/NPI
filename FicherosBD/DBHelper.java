package com.example.pruebanpi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {
    private static final String NOMBRE_BD = "bd_registro.db";
    private static final int VERSION_BD = 1;

    // constructor
    public DBHelper(@Nullable Context context) {
        super(context, NOMBRE_BD, null, VERSION_BD);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table usuarios(correoUsuario TEXT primary key, nombreUsuario TEXT, contrasenia TEXT)");

    }

    // cada vez que se actualice la versión de la BD, se ejecuta
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists usuarios");
    }

    public boolean introducirDatos(String correoUsuario, String nombreUsuario, String contrasenia){
        // acceder a bd en modo escritura
        SQLiteDatabase db = this.getWritableDatabase();
        // ContentValues actua como diccionario de pares clave-valor
        ContentValues contentValues = new ContentValues();
        contentValues.put("correoUsuario", correoUsuario);
        contentValues.put("nombreUsuario", nombreUsuario);
        contentValues.put("contrasenia", contrasenia);
        // insertar la tupla
        long resultado = db.insert("usuarios", null, contentValues);

        // si -1 -> error
        return resultado != -1;

    }

    // comprobar si existe el correo, solo uno por usuario
    public boolean comprobarSiExisteCorreo(String correoUsuario) {
        SQLiteDatabase db = this.getReadableDatabase();
        // en cuanto haya uno, ya devuelve false
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM usuarios WHERE correoUsuario = ?", new String[]{correoUsuario})) {
            return cursor.getCount() > 0;
        }
    }
    
    // comprobar si existe el usuario
    public boolean comprobarSiExisteUsuario(String correoUsuario, String contrasenia) {
        SQLiteDatabase db = this.getReadableDatabase();
        // en cuanto haya uno, ya devuelve false
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM usuarios WHERE correoUsuario = ? and contrasenia = ?", new String[]{correoUsuario, contrasenia})) {
            return cursor.getCount() > 0;
        }
    }
}
