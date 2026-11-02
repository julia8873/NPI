package com.example.nuietsiit.activities.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.main.MenuPrincipal;
import com.example.nuietsiit.data.UsuariosDB;

public class Login extends BaseActivity {

    // Declaración de los componentes de la interfaz de usuario
    private EditText etCorreo, etPassword;
    private Button btnLogin, btnSignUp;

    // Instancia del Helper para gestionar la base de datos SQLite
    private UsuariosDB BD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        aplicarMargenesSistema();

        // Inicialización de la base de datos
        BD = new UsuariosDB(this);

        // Vinculación de los componentes Java con las vistas del XML
        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);

        View btnVolver = findViewById(R.id.btnVolver);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                finish();
            });
        }

        View cardMascotaAyudante = findViewById(R.id.cardMascotaAyudante);
        if (cardMascotaAyudante != null) {
            cardMascotaAyudante.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_mascota_ayudante)
                        .setTitle("Asistente Virtual ETSIIT 🤖")
                        .setMessage("¡Hola! Para iniciar sesión introduce tu correo institucional y contraseña de la UGR. Si aún no tienes cuenta, pulsa en 'Crear cuenta nueva'.")
                        .setPositiveButton("¡Entendido!", null)
                        .show();
            });
        }

        // Evento de clic para el botón de Inicio de Sesión
        btnLogin.setOnClickListener(view -> {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            realizarLogin();
        });

        // Evento de clic para navegar a la pantalla de Registro (Sign Up)
        btnSignUp.setOnClickListener(view -> {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            Intent intent = new Intent(Login.this, SignUp.class);
            startActivity(intent);
        });
    }

    /**
     * Deja el contenido fuera de la barra de estado y de navegación (pantalla de borde a borde).
     */
    private void aplicarMargenesSistema() {
        View scroll = findViewById(R.id.scrollView);
        if (scroll != null) {
            ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, insets) -> {
                Insets barras = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(scroll);
        }
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
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Finaliza Login para evitar regresar a esta pantalla al pulsar el botón "Atrás"
            finish();
        }
    }
}
