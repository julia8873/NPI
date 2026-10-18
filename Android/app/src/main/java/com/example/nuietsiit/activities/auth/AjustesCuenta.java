package com.example.nuietsiit.activities.auth;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.main.MenuPrincipal;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.IdiomasDB;
import com.example.nuietsiit.data.UsuariosDB;

import java.util.List;

public class AjustesCuenta extends AppCompatActivity {

    private EditText etNombre, etPassword;
    private AutoCompleteTextView etGrado, etIdioma;
    private Button btnLogOut, btnGuardarCambios;

    private DBHelper BD;
    private String correoUsuario;

    private Integer idGradoSeleccionado = null;
    private String codigoIdiomaSeleccionado = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ajustes_cuenta);

        BD = DBHelper.getInstance(this);

        if (getIntent() != null && getIntent().hasExtra("CORREO")) {
            correoUsuario = getIntent().getStringExtra("CORREO");
        }

        etNombre = findViewById(R.id.etNombre);
        etPassword = findViewById(R.id.etPassword);
        etGrado = findViewById(R.id.etGrado);
        etIdioma = findViewById(R.id.etIdioma);
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        btnLogOut = findViewById(R.id.btnLogOut);

        // 1. Cargar datos del usuario
        cargarDatosUsuario();

        // 2. Configurar los desplegables pasando la información inicial
        configurarDesplegables();

        btnGuardarCambios.setOnClickListener(view -> guardarCambios());
        btnLogOut.setOnClickListener(view -> cerrarSesion());
    }

    private void cargarDatosUsuario() {
        if (correoUsuario == null || correoUsuario.isEmpty()) return;

        UsuariosDB usuariosDB = new UsuariosDB(BD);

        // Cargar textos actuales
        etNombre.setText(usuariosDB.obtenerNombreUsuario(correoUsuario));

        String gradoActual = usuariosDB.obtenerGradoUsuario(correoUsuario);
        String idiomaActual = usuariosDB.obtenerIdiomaUsuario(correoUsuario);

        etGrado.setText(gradoActual, false);
        etIdioma.setText(idiomaActual, false);
    }

    private void configurarDesplegables() {
        // --- GRADOS ---
        UsuariosDB usuariosDB = new UsuariosDB(BD);
        List<String> nombresGrados = usuariosDB.listarGrados();
        List<Integer> idsGrados = usuariosDB.listarIdGrados();

        ArrayAdapter<String> adaptadorGrados = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                nombresGrados
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(Color.BLACK); // Color del campo cuando está cerrado
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(Color.BLACK); // Color de la lista de opciones
                }
                return view;
            }
        };

        etGrado.setAdapter(adaptadorGrados);
        etGrado.setDropDownBackgroundDrawable(new ColorDrawable(Color.WHITE));
        etGrado.setKeyListener(null);
        etGrado.setOnClickListener(v -> etGrado.showDropDown());
        etGrado.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) etGrado.showDropDown();
        });
        etGrado.setOnItemClickListener((parent, view, position, id) -> {
            String gradoSeleccionado = (String) parent.getItemAtPosition(position);
            int index = nombresGrados.indexOf(gradoSeleccionado);
            if (index != -1 && index < idsGrados.size()) {
                idGradoSeleccionado = idsGrados.get(index);
            }
        });

        // --- IDIOMAS ---
        IdiomasDB idiomasDB = new IdiomasDB(BD);
        List<String> nombresIdiomas = idiomasDB.listarIdiomas();
        List<String> codigosIdiomas = idiomasDB.listarCodigoIdiomas();

        ArrayAdapter<String> adaptadorIdiomas = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                nombresIdiomas
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(Color.BLACK); // Color del campo cuando está cerrado
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(Color.BLACK); // Color de la lista de opciones
                }
                return view;
            }
        };

        etIdioma.setAdapter(adaptadorIdiomas);
        etIdioma.setDropDownBackgroundDrawable(new ColorDrawable(Color.WHITE));
        etIdioma.setKeyListener(null);
        etIdioma.setOnClickListener(v -> etIdioma.showDropDown());
        etIdioma.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) etIdioma.showDropDown();
        });
        etIdioma.setOnItemClickListener((parent, view, position, id) -> {
            String idiomaSeleccionado = (String) parent.getItemAtPosition(position);
            int index = nombresIdiomas.indexOf(idiomaSeleccionado);
            if (index != -1 && index < codigosIdiomas.size()) {
                codigoIdiomaSeleccionado = codigosIdiomas.get(index);
            }
        });
    }

    private void guardarCambios() {
        String nuevoNombre = etNombre.getText().toString().trim();
        String nuevaPassword = etPassword.getText().toString().trim();

        if (correoUsuario == null || correoUsuario.isEmpty()) {
            Toast.makeText(AjustesCuenta.this, "Error al identificar el usuario actual", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nuevoNombre.isEmpty()) {
            Toast.makeText(AjustesCuenta.this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        UsuariosDB usuariosDB = new UsuariosDB(BD);
        boolean actualizado = usuariosDB.actualizarAjustesEstudiante(
                correoUsuario,
                nuevoNombre,
                nuevaPassword,
                idGradoSeleccionado,
                codigoIdiomaSeleccionado,
                null
        );

        if (actualizado) {
            Toast.makeText(AjustesCuenta.this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show();
            etPassword.setText("");
        } else {
            Toast.makeText(AjustesCuenta.this, "No se realizaron cambios o error al actualizar", Toast.LENGTH_SHORT).show();
        }
    }

    private void cerrarSesion() {
        Intent intent = new Intent(AjustesCuenta.this, MenuPrincipal.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}