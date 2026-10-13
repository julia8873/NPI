package com.example.nuietsiit.activities.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.data.DBHelper;

public class AjustesCuenta  extends AppCompatActivity {

    // Declaración de los componentes de la interfaz de usuario
    private EditText etNombre, etPassword;
    private Button btnLogOut, btnGuardarCambios;

    // Instancia del Helper para la gestión de la base de datos SQLite
    DBHelper BD;

    // Variable para almacenar el correo del usuario actual
    private String correoUsuario;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ajustes_cuenta);

        // Inicialización de la base de datos
        BD = new DBHelper(this);

        // Obtención del correo pasado desde la pantalla anterior (MenuPrincipal)
        if (getIntent() != null && getIntent().hasExtra("CORREO")) {
            correoUsuario = getIntent().getStringExtra("CORREO");
        }

        // Vinculación de las variables Java con los elementos XML
        etNombre = findViewById(R.id.etNombre);
        etPassword = findViewById(R.id.etPassword);
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        btnLogOut = findViewById(R.id.btnLogOut);

        // Evento para guardar los datos actualizados
        btnGuardarCambios.setOnClickListener(view -> guardarCambios());

        // Evento para cerrar la sesión
        btnLogOut.setOnClickListener(view -> cerrarSesion());
    }

    /**
     * Valida y actualiza los datos del usuario en la base de datos.
     */
    private void guardarCambios() {
        String nuevoNombre = etNombre.getText().toString().trim();
        String nuevaPassword = etPassword.getText().toString().trim();

        // Validar que no estén vacíos ambos campos
        if (nuevoNombre.isEmpty() && nuevaPassword.isEmpty()) {
            Toast.makeText(AjustesCuenta.this, "Por favor, ingresa al menos un dato a cambiar", Toast.LENGTH_LONG).show();
            return;
        }

        // Comprobar que tenemos la referencia del usuario
        if (correoUsuario == null || correoUsuario.isEmpty()) {
            Toast.makeText(AjustesCuenta.this, "Error al identificar el usuario actual", Toast.LENGTH_SHORT).show();
            return;
        }

        // Actualizar datos en la base de datos
        // TERMINAR
        // boolean actualizado = BD.actualizarDatosUsuario(correoUsuario, nuevoNombre, nuevaPassword);
        boolean actualizado = false;

        if (actualizado) {
            Toast.makeText(AjustesCuenta.this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show();
            // Limpiar los campos tras guardar
            etNombre.setText("");
            etPassword.setText("");
        } else {
            Toast.makeText(AjustesCuenta.this, "Error al actualizar los datos", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Cierra la sesión del usuario y limpia la pila de navegación hacia la pantalla de Login.
     */
    private void cerrarSesion() {
        Intent intent = new Intent(AjustesCuenta.this, Login.class);

        // Limpia el historial de actividades para que el usuario no pueda regresar con el botón "Atrás"
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }
}
