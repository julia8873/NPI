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

public class SignUp extends BaseActivity {

    // Declaración de los componentes de la interfaz de usuario
    private EditText etNombre, etCorreo, etPassword;
    private Button btnCrearCuenta;

    // Instancia del Helper para la gestión de la base de datos SQLite
    private UsuariosDB BD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up);

        aplicarMargenesSistema();

        // Inicialización del Helper de la base de datos
        BD = new UsuariosDB(this);

        // Vinculación de los componentes Java con las vistas del XML
        etNombre = findViewById(R.id.etNombre);
        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);

        View btnVolver = findViewById(R.id.btnVolver);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                finish();
            });
        }

        View btnVolverLogin = findViewById(R.id.btnVolverLogin);
        if (btnVolverLogin != null) {
            btnVolverLogin.setOnClickListener(v -> {
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
                        .setMessage("¡Bienvenido al registro! Introduce tu nombre completo, correo institucional de la UGR y una contraseña. Podrás usar esta cuenta en toda la aplicación.")
                        .setPositiveButton("¡Entendido!", null)
                        .show();
            });
        }

        // Evento de clic para el botón de Registro (Crear Cuenta)
        btnCrearCuenta.setOnClickListener(view -> {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            registrarUsuario();
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
     * Procesa la validación de los campos y el registro del nuevo usuario en la base de datos.
     */
    private void registrarUsuario() {

        // Obtención del texto eliminando espacios en blanco innecesarios
        String nombre = etNombre.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validar que ninguno de los campos esté vacío
        if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty()) {
            Toast.makeText(SignUp.this, "Por favor, rellena todos los datos", Toast.LENGTH_LONG).show();
            return;
        }

        // Comprobación de existencia del correo en la base de datos
        if (BD.existeEstudiante(correo)) {
            Toast.makeText(SignUp.this, "Ya existe una cuenta asociada a este correo", Toast.LENGTH_LONG).show();
        } else {
            // Inserción del nuevo usuario
            boolean creado = BD.insertarEstudiante(correo, nombre, password);

            if (creado) {
                Toast.makeText(SignUp.this, "Cuenta creada con éxito", Toast.LENGTH_SHORT).show();

                // Guardamos el correo en SharedPreferences para iniciar sesión automáticamente
                SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("CORREO", correo);
                editor.apply();

                // Navegación directa al Menú Principal
                Intent intent = new Intent(SignUp.this, MenuPrincipal.class);
                intent.putExtra("CORREO", correo);

                // Limpiamos el historial para que no pueda volver atrás al registro
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                // Finaliza la pantalla de registro para evitar volver a ella pulsando el botón "Atrás"
                finish();
            } else {
                Toast.makeText(SignUp.this, "Error al crear la cuenta", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
