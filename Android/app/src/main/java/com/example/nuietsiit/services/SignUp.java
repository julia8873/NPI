package com.example.nuietsiit.services;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;
public class SignUp extends AppCompatActivity {
    // Declaración de los componentes de la interfaz de usuario
    EditText etNombre, etCorreo, etPassword;
    Button btnCrearCuenta;


    // Instancia de la clase Helper para gestionar la base de datos SQLite
    DBHelper BD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up);

        // Inicialización de la base de datos
        BD = new DBHelper(this);

        // Vinculación de las variables de Java con los elementos del archivo de diseño XML
        etNombre = findViewById(R.id.etNombre);
        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);

        // Configuración del evento de clic para el botón de inicio de sesión
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);
        btnCrearCuenta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String nombre, correo, password;

                // Obtención del texto introducido por el usuario
                nombre = etNombre.getText().toString();
                correo = etCorreo.getText().toString();
                password = etPassword.getText().toString();

                if (nombre.equals("") || correo.equals("") || password.equals("")) {
                    Toast.makeText(SignUp.this, "Por favor, rellena todos los datos", Toast.LENGTH_LONG).show();
                } else {
                    if (BD.comprobarSiExisteCorreo(correo)) {
                        Toast.makeText(SignUp.this, "Ya existe una cuenta asociado a este correo", Toast.LENGTH_LONG).show();
                    } else {
                        BD.introducirDatos(nombre, correo, password);

                        // Iniciar sesión y pasar a la pantalla principal
                        Intent intent = new Intent(SignUp.this, MenuPrincipal.class);

                        // Pasamos datos a siguiente pantalla
                        intent.putExtra("CORREO", correo);
                        startActivity(intent);
                    }
                }
            }

        });
    }
}
