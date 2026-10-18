package com.example.nuietsiit.activities.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.main.MenuPrincipal;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.UsuariosDB;

public class Login extends AppCompatActivity {

    // Declaración de los componentes de la interfaz de usuario
    private EditText etCorreo, etPassword;
    private Button btnLogin, btnSignUp;

    // Instancia del Helper para gestionar la base de datos SQLite
    private UsuariosDB BD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Inicialización de la base de datos
        BD = new UsuariosDB(this);

        // Vinculación de los componentes Java con las vistas del XML
        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);

        // Evento de clic para el botón de Inicio de Sesión
        btnLogin.setOnClickListener(view -> realizarLogin());

        // Evento de clic para navegar a la pantalla de Registro (Sign Up)
        btnSignUp.setOnClickListener(view -> {
            Intent intent = new Intent(Login.this, SignUp.class);
            startActivity(intent);
        });
    }

    /**
     * Procesa la validación de campos y la autenticación del usuario.
     */
    private void realizarLogin() {

        // Obtención de texto limpiando espacios en blanco innecesarios al inicio/final
        String correo = etCorreo.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validar que los campos no estén vacíos
        if (correo.isEmpty() || password.isEmpty()) {
            Toast.makeText(Login.this, "Por favor, rellena todos los datos", Toast.LENGTH_LONG).show();
            return;
        }

        // Comprobaciones contra la base de datos local
        if (!BD.existeEstudiante(correo)) {
            Toast.makeText(Login.this, "Usuario incorrecto", Toast.LENGTH_LONG).show();
        } else if (!BD.autenticarEstudiante(correo, password)) {
            Toast.makeText(Login.this, "Contraseña incorrecta", Toast.LENGTH_LONG).show();
        } else {
            // Guardamos el correo en SharedPreferences
            SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("CORREO", correo);
            editor.apply();

            // Inicio de sesión exitoso: Navegación al Menú Principal
            Intent intent = new Intent(Login.this, MenuPrincipal.class);
            intent.putExtra("CORREO", correo);
            startActivity(intent);

            // Finaliza Login para evitar regresar a esta pantalla al pulsar el botón "Atrás"
            finish();
        }
    }
}
