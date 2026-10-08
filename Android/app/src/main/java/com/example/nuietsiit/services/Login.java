package com.example.nuietsiit.services;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import com.example.nuietsiit.R;

public class Login extends AppCompatActivity {

    // Declaración de los componentes de la interfaz de usuario
    EditText etCorreo, etPassword;
    Button btnLogin, btnSignUp;


    // Instancia de la clase Helper para gestionar la base de datos SQLite
    DBHelper BD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Inicialización de la base de datos
        BD = new DBHelper(this);

        // Vinculación de las variables de Java con los elementos del archivo de diseño XML
        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);

        // Configuración del evento de clic para el botón de inicio de sesión
        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String correo, password;

                // Obtención del texto introducido por el usuario
                correo = etCorreo.getText().toString();
                password = etPassword.getText().toString();

                if (correo.equals("") || password.equals("")) {
                    Toast.makeText(Login.this, "Por favor, rellena todos los datos", Toast.LENGTH_LONG).show();
                } else {
                    if (!BD.comprobarSiExisteCorreo(correo)) {
                        Toast.makeText(Login.this, "Usuario incorrecto", Toast.LENGTH_LONG).show();
                    } else if (!BD.comprobarSiExisteUsuario(correo, password)) {
                        Toast.makeText(Login.this, "Contraseña incorrecta", Toast.LENGTH_LONG).show();
                    } else {
                        // Iniciar sesión y pasar a la pantalla principal
                        Intent intent = new Intent(Login.this, MenuPrincipal.class);

                        // Pasamos datos a siguiente pantalla
                        intent.putExtra("CORREO", correo);
                        startActivity(intent);
                    }
                }
            }

        });

        // Configuración del evento de clic para el botón de inicio de sesión
        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Pasamos a la pantalla de crear cuenta
                Intent intent = new Intent(Login.this, SignUp.class);
                startActivity(intent);
            }

        });
    }
}
