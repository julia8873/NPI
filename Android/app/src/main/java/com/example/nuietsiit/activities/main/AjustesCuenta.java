package com.example.nuietsiit.activities.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.UsuariosDB;

import java.util.List;

public class AjustesCuenta extends BaseActivity {

    private TextView tvNombrePerfil, tvCorreoPerfil;
    private SwitchCompat switchAvisosClase, switchVegetariano;

    private DBHelper BD;
    private String correoUsuario;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ajustes_cuenta);

        aplicarMargenesSistema();

        BD = DBHelper.getInstance(this);

        if (getIntent() != null && getIntent().hasExtra("CORREO")) {
            correoUsuario = getIntent().getStringExtra("CORREO");
        }
        if (correoUsuario == null || correoUsuario.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
            correoUsuario = prefs.getString("CORREO", null);
        }

        tvNombrePerfil = findViewById(R.id.tvNombrePerfil);
        tvCorreoPerfil = findViewById(R.id.tvCorreoPerfil);
        switchAvisosClase = findViewById(R.id.switchAvisosClase);
        switchVegetariano = findViewById(R.id.switchVegetariano);

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
                        .setTitle(R.string.asistente_virtual_titulo)
                        .setMessage(R.string.ajustes_asistente_msg)
                        .setPositiveButton(R.string.btn_entendido, null)
                        .show();
            });
        }

        // Cargar datos del usuario
        cargarDatosUsuario();

        // Configurar clics en las tarjetas
        configurarAcciones();
    }

    private void aplicarMargenesSistema() {
        View scroll = findViewById(R.id.scrollViewAjustes);
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

    private void cargarDatosUsuario() {
        if (correoUsuario == null || correoUsuario.isEmpty()) {
            if (tvCorreoPerfil != null) tvCorreoPerfil.setText(R.string.sin_sesion_iniciada);
            if (tvNombrePerfil != null) tvNombrePerfil.setText(R.string.invitado);
            return;
        }

        if (tvCorreoPerfil != null) {
            tvCorreoPerfil.setText(correoUsuario);
        }

        UsuariosDB usuariosDB = new UsuariosDB(BD);
        String nombre = usuariosDB.obtenerNombreUsuario(correoUsuario);
        if (tvNombrePerfil != null) {
            if (nombre != null && !nombre.isEmpty()) {
                tvNombrePerfil.setText(nombre);
            } else {
                tvNombrePerfil.setText(R.string.tu_perfil);
            }
        }

        // Cargar interruptor de Avisos de clase
        SharedPreferences configPrefs = getSharedPreferences("ConfiguracionApp", MODE_PRIVATE);
        boolean avisosActivos = configPrefs.getBoolean("AVISOS_CLASE_HABILITADOS", true);
        if (switchAvisosClase != null) {
            switchAvisosClase.setChecked(avisosActivos);
        }

        // Cargar interruptor de Menú del comedor
        SharedPreferences sesionPrefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        boolean esVeg = usuariosDB.esUsuarioVegetariano(correoUsuario) || sesionPrefs.getBoolean("ES_VEGETARIANO", false);
        if (switchVegetariano != null) {
            switchVegetariano.setChecked(esVeg);
        }
    }

    private void configurarAcciones() {
        View cardCuenta = findViewById(R.id.cardCuenta);
        if (cardCuenta != null) {
            cardCuenta.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                mostrarDialogoEditarCuenta();
            });
        }

        View cardNotificaciones = findViewById(R.id.cardNotificaciones);
        if (cardNotificaciones != null) {
            cardNotificaciones.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                mostrarToastFaltaPorImplementar();
            });
        }

        View cardApariencia = findViewById(R.id.cardApariencia);
        if (cardApariencia != null) {
            cardApariencia.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                mostrarToastFaltaPorImplementar();
            });
        }

        View cardIdioma = findViewById(R.id.cardIdioma);
        if (cardIdioma != null) {
            cardIdioma.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                mostrarDialogoIdioma();
            });
        }

        View cardPrivacidad = findViewById(R.id.cardPrivacidad);
        if (cardPrivacidad != null) {
            cardPrivacidad.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                mostrarToastFaltaPorImplementar();
            });
        }

        View cardAyuda = findViewById(R.id.cardAyuda);
        if (cardAyuda != null) {
            cardAyuda.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                mostrarToastFaltaPorImplementar();
            });
        }

        if (switchAvisosClase != null) {
            switchAvisosClase.setOnCheckedChangeListener((buttonView, isChecked) -> {
                buttonView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                getSharedPreferences("ConfiguracionApp", MODE_PRIVATE)
                        .edit()
                        .putBoolean("AVISOS_CLASE_HABILITADOS", isChecked)
                        .apply();
                int msgRes = isChecked ? R.string.avisos_clase_activados : R.string.avisos_clase_desactivados;
                Toast.makeText(this, msgRes, Toast.LENGTH_SHORT).show();
            });
        }

        if (switchVegetariano != null) {
            switchVegetariano.setOnCheckedChangeListener((buttonView, isChecked) -> {
                buttonView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                getSharedPreferences("SesionUsuario", MODE_PRIVATE)
                        .edit()
                        .putBoolean("ES_VEGETARIANO", isChecked)
                        .apply();
                if (correoUsuario != null && !correoUsuario.isEmpty()) {
                    UsuariosDB usuariosDB = new UsuariosDB(BD);
                    usuariosDB.actualizarPreferenciaVegetariano(correoUsuario, isChecked);
                }
                int msgRes = isChecked ? R.string.menu_veg_activado : R.string.menu_normal_activado;
                Toast.makeText(this, msgRes, Toast.LENGTH_SHORT).show();
            });
        }

        View btnLogOut = findViewById(R.id.btnLogOut);
        if (btnLogOut != null) {
            btnLogOut.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                cerrarSesion();
            });
        }
    }

    private void mostrarToastFaltaPorImplementar() {
        Toast.makeText(this, R.string.falta_por_implementar, Toast.LENGTH_SHORT).show();
    }

    private void mostrarDialogoEditarCuenta() {
        if (correoUsuario == null || correoUsuario.isEmpty()) {
            Toast.makeText(this, R.string.inicia_sesion_editar_perfil, Toast.LENGTH_SHORT).show();
            return;
        }

        UsuariosDB usuariosDB = new UsuariosDB(BD);
        String nombreActual = usuariosDB.obtenerNombreUsuario(correoUsuario);
        String gradoActual = usuariosDB.obtenerGradoUsuario(correoUsuario);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_editar_perfil, null);
        EditText etDialogNombre = dialogView.findViewById(R.id.etDialogNombre);
        EditText etDialogPassword = dialogView.findViewById(R.id.etDialogPassword);
        AutoCompleteTextView etDialogGrado = dialogView.findViewById(R.id.etDialogGrado);

        etDialogNombre.setText(nombreActual);
        etDialogGrado.setText(gradoActual, false);

        List<String> nombresGrados = usuariosDB.listarGrados();
        List<Integer> idsGrados = usuariosDB.listarIdGrados();
        final Integer[] gradoSeleccionado = {null};

        ArrayAdapter<String> adaptadorGrados = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombresGrados);
        etDialogGrado.setAdapter(adaptadorGrados);
        etDialogGrado.setKeyListener(null);
        etDialogGrado.setOnClickListener(v -> etDialogGrado.showDropDown());
        etDialogGrado.setOnItemClickListener((parent, view, position, id) -> {
            if (position < idsGrados.size()) {
                gradoSeleccionado[0] = idsGrados.get(position);
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.editar_perfil_titulo)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_guardar, (dialog, which) -> {
                    String nuevoNombre = etDialogNombre.getText().toString().trim();
                    String nuevaPassword = etDialogPassword.getText().toString().trim();
                    if (!nuevoNombre.isEmpty()) {
                        usuariosDB.actualizarAjustesEstudiante(
                                correoUsuario,
                                nuevoNombre,
                                nuevaPassword,
                                gradoSeleccionado[0],
                                null,
                                null,
                                switchVegetariano != null && switchVegetariano.isChecked()
                        );
                        tvNombrePerfil.setText(nuevoNombre);
                        Toast.makeText(this, R.string.perfil_actualizado_exito, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void cerrarSesion() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.cerrar_sesion_titulo)
                .setMessage(R.string.cerrar_sesion_msg)
                .setPositiveButton(R.string.btn_si_salir, (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
                    prefs.edit().clear().apply();

                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"));

                    Intent intent = new Intent(AjustesCuenta.this, MenuPrincipal.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void mostrarDialogoIdioma() {
        String[] idiomasNombres = {"Español", "English"};
        String[] idiomasCodigos = {"es", "en"};

        String codigoActual = "es";
        if (correoUsuario != null && !correoUsuario.isEmpty()) {
            UsuariosDB usuariosDB = new UsuariosDB(BD);
            String codigoDb = usuariosDB.obtenerCodigoIdiomaUsuario(correoUsuario);
            if (codigoDb != null && !codigoDb.isEmpty()) {
                codigoActual = codigoDb;
            }
        } else {
            SharedPreferences prefs = getSharedPreferences("ConfiguracionApp", MODE_PRIVATE);
            codigoActual = prefs.getString("CODIGO_IDIOMA", "es");
        }

        int seleccionActual = 0;
        for (int i = 0; i < idiomasCodigos.length; i++) {
            if (idiomasCodigos[i].equals(codigoActual)) {
                seleccionActual = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.idioma)
                .setSingleChoiceItems(idiomasNombres, seleccionActual, (dialog, which) -> {
                    String nuevoCodigo = idiomasCodigos[which];

                    // 1. Guardar en SharedPreferences
                    getSharedPreferences("ConfiguracionApp", MODE_PRIVATE)
                            .edit()
                            .putString("CODIGO_IDIOMA", nuevoCodigo)
                            .apply();

                    // 2. Guardar en Base de Datos si hay sesión iniciada
                    if (correoUsuario != null && !correoUsuario.isEmpty()) {
                        UsuariosDB usuariosDB = new UsuariosDB(BD);
                        usuariosDB.actualizarIdiomaEstudiante(correoUsuario, nuevoCodigo);
                    }

                    dialog.dismiss();

                    // 3. Aplicar el idioma globalmente (AppCompat se encarga de reiniciar las actividades)
                    LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(nuevoCodigo);
                    AppCompatDelegate.setApplicationLocales(appLocale);

                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }
}