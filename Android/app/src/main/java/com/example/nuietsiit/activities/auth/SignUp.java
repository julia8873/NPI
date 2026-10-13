package com.example.nuietsiit.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.main.MenuPrincipal;
import com.example.nuietsiit.data.DBHelper;

public class SignUp extends AppCompatActivity {

    // Declaración de los componentes de la interfaz de usuario
    private EditText etNombre, etCorreo, etPassword;
    private Button btnCrearCuenta;

    // Instancia del Helper para la gestión de la base de datos SQLite
    DBHelper BD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up);

        // Inicialización del Helper de la base de datos
        BD = new DBHelper(this);

        // Vinculación de los componentes Java con las vistas del XML
        etNombre = findViewById(R.id.etNombre);
        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);

        // Evento de clic para el botón de Registro (Crear Cuenta)
        btnCrearCuenta.setOnClickListener(view -> registrarUsuario());
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
        if (BD.comprobarSiExisteCorreo(correo)) {
            Toast.makeText(SignUp.this, "Ya existe una cuenta asociada a este correo", Toast.LENGTH_LONG).show();
        } else {
            // Inserción del nuevo usuario
            BD.introducirDatos(correo, nombre, password);

            Toast.makeText(SignUp.this, "Cuenta creada con éxito", Toast.LENGTH_SHORT).show();

            // Navegación directa al Menú Principal
            Intent intent = new Intent(SignUp.this, MenuPrincipal.class);
            intent.putExtra("CORREO", correo);
            startActivity(intent);

            // Finaliza la pantalla de registro para evitar volver a ella pulsando el botón "Atrás"
            finish();
        }
    }
}
