package com.example.pruebanpi;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

/**
 * DBHelper central: crea y actualiza todas las tablas de la base de datos.
 * Cada módulo tiene su propio fichero auxiliar con las funciones de acceso a datos.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String NOMBRE_BD = "bd_npi.db";
    private static final int VERSION_BD = 1;

    // constructor
    public DBHelper(@Nullable Context context) {
        super(context, NOMBRE_BD, null, VERSION_BD);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // -------------------------------------------------
        //                  IDIOMAS
        // -------------------------------------------------
        db.execSQL(
            "CREATE TABLE IDIOMA (" +
            "  codigoIdioma TEXT PRIMARY KEY," +
            "  nombreIdioma TEXT NOT NULL" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE TRADUCCION (" +
            "  entidad       TEXT NOT NULL," +
            "  idEntidad     TEXT NOT NULL," +
            "  campo         TEXT NOT NULL," +
            "  codigoIdioma  TEXT NOT NULL," +
            "  texto         TEXT NOT NULL," +
            "  PRIMARY KEY (entidad, idEntidad, campo, codigoIdioma)," +
            "  FOREIGN KEY (codigoIdioma) REFERENCES IDIOMA(codigoIdioma)" +
            ")"
        );

        // -------------------------------------------------
        //              USUARIOS
        // -------------------------------------------------
        // GRADO se crea antes que ESTUDIANTE porque ESTUDIANTE la referencia
        db.execSQL(
            "CREATE TABLE GRADO (" +
            "  idGrado      INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  nombreGrado  TEXT NOT NULL," +
            "  enlaceWeb    TEXT" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE ESTUDIANTE (" +
            "  correoEstudiante       TEXT PRIMARY KEY," +
            "  nombreEstudiante       TEXT NOT NULL," +
            "  contraseniaHash        TEXT NOT NULL," +
            "  enlaceCalendarioGoogle TEXT," +
            "  idGrado                INTEGER NOT NULL," +
            "  codigoIdioma           TEXT NOT NULL," +
            "  tokenNotificacion      TEXT," +
            "  FOREIGN KEY (idGrado)      REFERENCES GRADO(idGrado)," +
            "  FOREIGN KEY (codigoIdioma) REFERENCES IDIOMA(codigoIdioma)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE PROFESOR (" +
            "  correoProfesor    TEXT PRIMARY KEY," +
            "  nombreProfesor    TEXT NOT NULL," +
            "  contraseniaHash   TEXT NOT NULL," +
            "  enlacePaginaWeb   TEXT," +
            "  tokenNotificacion TEXT" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE ADMINISTRADOR (" +
            "  correoAdmin     TEXT PRIMARY KEY," +
            "  nombreAdmin     TEXT NOT NULL," +
            "  contraseniaHash TEXT NOT NULL," +
            "  rol             TEXT NOT NULL CHECK (rol IN ('secretaria','direccion','comunicacion'))" +
            ")"
        );

        // -------------------------------------------------
        //      ESPACIOS
        // -------------------------------------------------
        db.execSQL(
            "CREATE TABLE EDIFICIO (" +
            "  idEdificio      INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  nombreEdificio  TEXT NOT NULL," +
            "  direccion       TEXT NOT NULL," +
            "  latitud         REAL NOT NULL," +
            "  longitud        REAL NOT NULL" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE PLANTA (" +
            "  idPlanta               INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  idEdificio             INTEGER NOT NULL," +
            "  numeroPlanta           INTEGER NOT NULL," +
            "  altitudMetros          REAL," +
            "  presionReferenciaHpa   REAL," +
            "  imagenPlanoUrl         TEXT," +
            "  orientacionNorteGrados REAL" +
            "  CHECK (orientacionNorteGrados IS NULL OR (orientacionNorteGrados >= 0 AND orientacionNorteGrados < 360))," +
            "  FOREIGN KEY (idEdificio) REFERENCES EDIFICIO(idEdificio)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE ESPACIO (" +
            "  idEspacio    INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  idPlanta     INTEGER NOT NULL," +
            "  nombreEspacio TEXT NOT NULL," +
            "  tipoEspacio  TEXT NOT NULL CHECK (tipoEspacio IN " +
            "    ('aula','despacho','secretaria','comedor','biblioteca'," +
            "     'ascensor','aseo','laboratorio','otro'))," +
            "  capacidad    INTEGER," +
            "  coordX       REAL NOT NULL," +
            "  coordY       REAL NOT NULL," +
            "  descripcion  TEXT," +
            "  FOREIGN KEY (idPlanta) REFERENCES PLANTA(idPlanta)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE FOTO_ESPACIO (" +
            "  idFoto    INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  idEspacio INTEGER NOT NULL," +
            "  url       TEXT NOT NULL," +
            "  FOREIGN KEY (idEspacio) REFERENCES ESPACIO(idEspacio)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE NODO_NAVEGACION (" +
            "  idNodo    INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  idPlanta  INTEGER NOT NULL," +
            "  coordX    REAL NOT NULL," +
            "  coordY    REAL NOT NULL," +
            "  idEspacio INTEGER," +
            "  FOREIGN KEY (idPlanta)  REFERENCES PLANTA(idPlanta)," +
            "  FOREIGN KEY (idEspacio) REFERENCES ESPACIO(idEspacio)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE ARISTA_NAVEGACION (" +
            "  idNodoOrigen    INTEGER NOT NULL," +
            "  idNodoDestino   INTEGER NOT NULL," +
            "  distanciaMetros REAL NOT NULL," +
            "  tipoTramo       TEXT NOT NULL CHECK (tipoTramo IN ('pasillo','escalera','ascensor'))," +
            "  accesible       INTEGER NOT NULL CHECK (accesible IN (0,1))," +
            "  PRIMARY KEY (idNodoOrigen, idNodoDestino)," +
            "  FOREIGN KEY (idNodoOrigen)  REFERENCES NODO_NAVEGACION(idNodo)," +
            "  FOREIGN KEY (idNodoDestino) REFERENCES NODO_NAVEGACION(idNodo)" +
            ")"
        );

        // -------------------------------------------------
        //              DESPACHO_PROFESOR
        // -------------------------------------------------
        db.execSQL(
            "CREATE TABLE DESPACHO_PROFESOR (" +
            "  correoProfesor TEXT NOT NULL," +
            "  idEspacio      INTEGER NOT NULL," +
            "  PRIMARY KEY (correoProfesor, idEspacio)," +
            "  FOREIGN KEY (correoProfesor) REFERENCES PROFESOR(correoProfesor)," +
            "  FOREIGN KEY (idEspacio)      REFERENCES ESPACIO(idEspacio)" +
            ")"
        );

        // -------------------------------------------------
        //               ACADÉMICO
        // -------------------------------------------------
        db.execSQL(
            "CREATE TABLE ASIGNATURA (" +
            "  codigoAsignatura  TEXT PRIMARY KEY," +
            "  nombreAsignatura  TEXT NOT NULL," +
            "  cuatrimestre      INTEGER NOT NULL CHECK (cuatrimestre IN (1,2))," +
            "  creditosECTS      REAL NOT NULL," +
            "  enlaceGuiaDocente TEXT NOT NULL" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE ASIGNATURA_GRADO (" +
            "  codigoAsignatura TEXT NOT NULL," +
            "  idGrado          INTEGER NOT NULL," +
            "  curso            INTEGER NOT NULL CHECK (curso BETWEEN 1 AND 6)," +
            "  tipo             TEXT NOT NULL CHECK (tipo IN ('obligatoria','optativa'))," +
            "  PRIMARY KEY (codigoAsignatura, idGrado)," +
            "  FOREIGN KEY (codigoAsignatura) REFERENCES ASIGNATURA(codigoAsignatura)," +
            "  FOREIGN KEY (idGrado)          REFERENCES GRADO(idGrado)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE IMPARTE (" +
            "  codigoAsignatura TEXT NOT NULL," +
            "  correoProfesor   TEXT NOT NULL," +
            "  cursoAcademico   TEXT NOT NULL," +
            "  PRIMARY KEY (codigoAsignatura, correoProfesor, cursoAcademico)," +
            "  FOREIGN KEY (codigoAsignatura) REFERENCES ASIGNATURA(codigoAsignatura)," +
            "  FOREIGN KEY (correoProfesor)   REFERENCES PROFESOR(correoProfesor)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE MATRICULA (" +
            "  correoEstudiante TEXT NOT NULL," +
            "  codigoAsignatura TEXT NOT NULL," +
            "  cursoAcademico   TEXT NOT NULL," +
            "  grupo            TEXT NOT NULL," +
            "  PRIMARY KEY (correoEstudiante, codigoAsignatura, cursoAcademico)," +
            "  FOREIGN KEY (correoEstudiante) REFERENCES ESTUDIANTE(correoEstudiante)," +
            "  FOREIGN KEY (codigoAsignatura) REFERENCES ASIGNATURA(codigoAsignatura)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE SESION_CLASE (" +
            "  idSesion         INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  codigoAsignatura TEXT NOT NULL," +
            "  idEspacio        INTEGER," +
            "  diaSemana        INTEGER NOT NULL CHECK (diaSemana BETWEEN 1 AND 7)," +
            "  horaInicio       TEXT NOT NULL," +
            "  horaFin          TEXT NOT NULL," +
            "  grupo            TEXT NOT NULL," +
            "  tipoSesion       TEXT NOT NULL CHECK (tipoSesion IN ('teoria','practicas','seminario'))," +
            "  correoProfesor   TEXT," +
            "  cursoAcademico   TEXT NOT NULL," +
            "  FOREIGN KEY (codigoAsignatura) REFERENCES ASIGNATURA(codigoAsignatura)," +
            "  FOREIGN KEY (idEspacio)        REFERENCES ESPACIO(idEspacio)," +
            "  FOREIGN KEY (correoProfesor)   REFERENCES PROFESOR(correoProfesor)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE EXAMEN (" +
            "  idExamen              INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  codigoAsignatura      TEXT NOT NULL," +
            "  idEspacio             INTEGER," +
            "  fecha                 TEXT NOT NULL," +
            "  horaInicio            TEXT NOT NULL," +
            "  duracion              INTEGER NOT NULL," +
            "  comentariosProfesores TEXT," +
            "  cursoAcademico        TEXT NOT NULL," +
            "  convocatoria          TEXT NOT NULL CHECK (convocatoria IN ('ordinaria','extraordinaria'))," +
            "  FOREIGN KEY (codigoAsignatura) REFERENCES ASIGNATURA(codigoAsignatura)," +
            "  FOREIGN KEY (idEspacio)        REFERENCES ESPACIO(idEspacio)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE LIBRO (" +
            "  idLibro              INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  titulo               TEXT NOT NULL," +
            "  autores              TEXT NOT NULL," +
            "  isbn                 TEXT NOT NULL UNIQUE," +
            "  enlaceCatalogo       TEXT," +
            "  disponibleBiblioteca INTEGER NOT NULL CHECK (disponibleBiblioteca IN (0,1))," +
            "  ubicacionBiblioteca  TEXT" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE ASIGNATURA_LIBRO (" +
            "  codigoAsignatura TEXT NOT NULL," +
            "  idLibro          INTEGER NOT NULL," +
            "  tipoBibliografia TEXT NOT NULL CHECK (tipoBibliografia IN ('basica','complementaria'))," +
            "  PRIMARY KEY (codigoAsignatura, idLibro)," +
            "  FOREIGN KEY (codigoAsignatura) REFERENCES ASIGNATURA(codigoAsignatura)," +
            "  FOREIGN KEY (idLibro)          REFERENCES LIBRO(idLibro)" +
            ")"
        );

        // -------------------------------------------------
        //              TUTORÍAS
        // -------------------------------------------------
        db.execSQL(
            "CREATE TABLE TUTORIA_HORARIO (" +
            "  idHorario      INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  correoProfesor TEXT NOT NULL," +
            "  diaSemana      INTEGER NOT NULL CHECK (diaSemana BETWEEN 1 AND 7)," +
            "  horaInicio     TEXT NOT NULL," +
            "  horaFin        TEXT NOT NULL," +
            "  modalidad      TEXT NOT NULL CHECK (modalidad IN ('presencial','online','mixta'))," +
            "  idEspacio      INTEGER," +
            "  enlaceOnline   TEXT," +
            "  FOREIGN KEY (correoProfesor) REFERENCES PROFESOR(correoProfesor)," +
            "  FOREIGN KEY (idEspacio)      REFERENCES ESPACIO(idEspacio)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE TUTORIA_RESERVA (" +
            "  idReserva        INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  idHorario        INTEGER NOT NULL," +
            "  correoEstudiante TEXT NOT NULL," +
            "  fecha            TEXT NOT NULL," +
            "  motivo           TEXT," +
            "  estado           TEXT NOT NULL CHECK (estado IN ('pendiente','confirmada','cancelada'))," +
            "  enlaceOnline     TEXT," +
            "  FOREIGN KEY (idHorario)        REFERENCES TUTORIA_HORARIO(idHorario)," +
            "  FOREIGN KEY (correoEstudiante) REFERENCES ESTUDIANTE(correoEstudiante)" +
            ")"
        );


        db.execSQL(
            "CREATE UNIQUE INDEX idx_reserva_horario_fecha " +
            "ON TUTORIA_RESERVA (idHorario, fecha) " +
            "WHERE estado <> 'cancelada'"
        );

        // -------------------------------------------------
        //              COMEDORES
        // -------------------------------------------------
        db.execSQL(
            "CREATE TABLE COMEDOR (" +
            "  idComedor     INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  nombreComedor TEXT NOT NULL," +
            "  idEdificio    INTEGER," +
            "  direccion     TEXT NOT NULL," +
            "  latitud       REAL NOT NULL," +
            "  longitud      REAL NOT NULL," +
            "  FOREIGN KEY (idEdificio) REFERENCES EDIFICIO(idEdificio)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE COMEDOR_HORARIO (" +
            "  idComedor    INTEGER NOT NULL," +
            "  diaSemana    INTEGER NOT NULL CHECK (diaSemana BETWEEN 1 AND 7)," +
            "  horaApertura TEXT NOT NULL," +
            "  horaCierre   TEXT NOT NULL," +
            "  PRIMARY KEY (idComedor, diaSemana)," +
            "  FOREIGN KEY (idComedor) REFERENCES COMEDOR(idComedor)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE TARIFA_COMEDOR (" +
            "  idComedor   INTEGER NOT NULL," +
            "  tipoUsuario TEXT NOT NULL CHECK (tipoUsuario IN ('estudiante','personal','externo'))," +
            "  modalidad   TEXT NOT NULL CHECK (modalidad IN ('en_comedor','para_llevar'))," +
            "  precio      INTEGER NOT NULL," +
            "  PRIMARY KEY (idComedor, tipoUsuario, modalidad)," +
            "  FOREIGN KEY (idComedor) REFERENCES COMEDOR(idComedor)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE PLATO (" +
            "  idPlato     INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  nombrePlato TEXT NOT NULL," +
            "  descripcion TEXT," +
            "  fotoUrl     TEXT," +
            "  kcal        INTEGER NOT NULL," +
            "  proteinasG  REAL NOT NULL," +
            "  hidratosG   REAL NOT NULL," +
            "  grasasG     REAL NOT NULL" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE ALERGENO (" +
            "  idAlergeno     INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  nombreAlergeno TEXT NOT NULL" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE PLATO_ALERGENO (" +
            "  idPlato    INTEGER NOT NULL," +
            "  idAlergeno INTEGER NOT NULL," +
            "  PRIMARY KEY (idPlato, idAlergeno)," +
            "  FOREIGN KEY (idPlato)    REFERENCES PLATO(idPlato)," +
            "  FOREIGN KEY (idAlergeno) REFERENCES ALERGENO(idAlergeno)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE MENU (" +
            "  idMenu    INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  idComedor INTEGER NOT NULL," +
            "  fecha     TEXT NOT NULL," +
            "  modalidad TEXT NOT NULL CHECK (modalidad IN ('en_comedor','para_llevar'))," +
            "  UNIQUE (idComedor, fecha, modalidad)," +
            "  FOREIGN KEY (idComedor) REFERENCES COMEDOR(idComedor)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE MENU_PLATO (" +
            "  idMenu    INTEGER NOT NULL," +
            "  idPlato   INTEGER NOT NULL," +
            "  categoria TEXT NOT NULL CHECK (categoria IN ('primero','segundo','postre'))," +
            "  PRIMARY KEY (idMenu, idPlato)," +
            "  FOREIGN KEY (idMenu)  REFERENCES MENU(idMenu)," +
            "  FOREIGN KEY (idPlato) REFERENCES PLATO(idPlato)" +
            ")"
        );

        // -------------------------------------------------
        //              ACTIVIDADES
        // -------------------------------------------------
        db.execSQL(
            "CREATE TABLE ACTIVIDAD (" +
            "  idActividad    INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  titulo         TEXT NOT NULL," +
            "  descripcion    TEXT NOT NULL," +
            "  tipoActividad  TEXT NOT NULL CHECK (tipoActividad IN " +
            "    ('bibliomaker','dia_etsiit','abiertaugr','charla','otro'))," +
            "  fechaInicio    TEXT NOT NULL," +
            "  fechaFin       TEXT," +
            "  idEspacio      INTEGER," +
            "  enlaceInfo     TEXT," +
            "  correoAdmin    TEXT," +
            "  correoProfesor TEXT," +
            "  FOREIGN KEY (idEspacio)      REFERENCES ESPACIO(idEspacio)," +
            "  FOREIGN KEY (correoAdmin)    REFERENCES ADMINISTRADOR(correoAdmin)," +
            "  FOREIGN KEY (correoProfesor) REFERENCES PROFESOR(correoProfesor)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE CURSO_ABIERTAUGR (" +
            "  idActividad            INTEGER PRIMARY KEY," +
            "  creditos               REAL NOT NULL," +
            "  plazoInicioInscripcion TEXT NOT NULL," +
            "  plazoFinInscripcion    TEXT NOT NULL," +
            "  plazasTotales          INTEGER," +
            "  FOREIGN KEY (idActividad) REFERENCES ACTIVIDAD(idActividad)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE INSCRIPCION_ACTIVIDAD (" +
            "  correoEstudiante TEXT NOT NULL," +
            "  idActividad      INTEGER NOT NULL," +
            "  fechaInscripcion TEXT NOT NULL," +
            "  PRIMARY KEY (correoEstudiante, idActividad)," +
            "  FOREIGN KEY (correoEstudiante) REFERENCES ESTUDIANTE(correoEstudiante)," +
            "  FOREIGN KEY (idActividad)      REFERENCES ACTIVIDAD(idActividad)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE COMUNIDAD_CHAT (" +
            "  idComunidad     INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  nombreComunidad TEXT NOT NULL," +
            "  plataforma      TEXT NOT NULL CHECK (plataforma IN ('whatsapp','telegram','discord'))," +
            "  enlaceAcceso    TEXT NOT NULL," +
            "  descripcion     TEXT," +
            "  idGrado         INTEGER," +
            "  FOREIGN KEY (idGrado) REFERENCES GRADO(idGrado)" +
            ")"
        );

        // -------------------------------------------------
        //              SECRETARÍA
        // -------------------------------------------------
        db.execSQL(
            "CREATE TABLE PROCEDIMIENTO (" +
            "  idProcedimiento     INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  nombreProcedimiento TEXT NOT NULL," +
            "  categoria           TEXT NOT NULL CHECK (categoria IN " +
            "    ('matricula','titulos','reconocimiento_creditos','convocatorias','otro'))," +
            "  descripcion         TEXT NOT NULL," +
            "  requisitos          TEXT," +
            "  plazosInfo          TEXT," +
            "  enlaceSolicitud     TEXT," +
            "  enlaceCita          TEXT" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE PROCEDIMIENTO_PASO (" +
            "  idProcedimiento INTEGER NOT NULL," +
            "  orden           INTEGER NOT NULL," +
            "  descripcionPaso TEXT NOT NULL," +
            "  PRIMARY KEY (idProcedimiento, orden)," +
            "  FOREIGN KEY (idProcedimiento) REFERENCES PROCEDIMIENTO(idProcedimiento)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE PROCEDIMIENTO_DOCUMENTO (" +
            "  idDocumento     INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  idProcedimiento INTEGER NOT NULL," +
            "  nombreDocumento TEXT NOT NULL," +
            "  url             TEXT NOT NULL," +
            "  FOREIGN KEY (idProcedimiento) REFERENCES PROCEDIMIENTO(idProcedimiento)" +
            ")"
        );

        // -------------------------------------------------
        //              ANUNCIOS
        // -------------------------------------------------
        db.execSQL(
            "CREATE TABLE ANUNCIO_GENERAL (" +
            "  idAnuncioGeneral INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  titulo           TEXT NOT NULL," +
            "  contenido        TEXT NOT NULL," +
            "  categoria        TEXT NOT NULL CHECK (categoria IN ('beca','plazo','transporte','general'))," +
            "  prioridad        TEXT NOT NULL CHECK (prioridad IN ('baja','normal','alta'))," +
            "  fechaPublicacion TEXT NOT NULL," +
            "  fechaCaducidad   TEXT," +
            "  enlace           TEXT," +
            "  idGrado          INTEGER," +
            "  correoAdmin      TEXT NOT NULL," +
            "  FOREIGN KEY (idGrado)     REFERENCES GRADO(idGrado)," +
            "  FOREIGN KEY (correoAdmin) REFERENCES ADMINISTRADOR(correoAdmin)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE ANUNCIO_ASIGNATURA (" +
            "  idAnuncioAsignatura INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  titulo              TEXT NOT NULL," +
            "  contenido           TEXT NOT NULL," +
            "  tipoAviso           TEXT NOT NULL CHECK (tipoAviso IN ('cambio_aula','clase_anulada','otro'))," +
            "  fechaPublicacion    TEXT NOT NULL," +
            "  fechaCaducidad      TEXT," +
            "  codigoAsignatura    TEXT NOT NULL," +
            "  correoProfesor      TEXT NOT NULL," +
            "  idSesion            INTEGER," +
            "  fechaAfectada       TEXT," +
            "  idEspacioNuevo      INTEGER," +
            "  FOREIGN KEY (codigoAsignatura) REFERENCES ASIGNATURA(codigoAsignatura)," +
            "  FOREIGN KEY (correoProfesor)   REFERENCES PROFESOR(correoProfesor)," +
            "  FOREIGN KEY (idSesion)         REFERENCES SESION_CLASE(idSesion)," +
            "  FOREIGN KEY (idEspacioNuevo)   REFERENCES ESPACIO(idEspacio)" +
            ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS ANUNCIO_ASIGNATURA");
        db.execSQL("DROP TABLE IF EXISTS ANUNCIO_GENERAL");
        db.execSQL("DROP TABLE IF EXISTS PROCEDIMIENTO_DOCUMENTO");
        db.execSQL("DROP TABLE IF EXISTS PROCEDIMIENTO_PASO");
        db.execSQL("DROP TABLE IF EXISTS PROCEDIMIENTO");
        db.execSQL("DROP TABLE IF EXISTS COMUNIDAD_CHAT");
        db.execSQL("DROP TABLE IF EXISTS INSCRIPCION_ACTIVIDAD");
        db.execSQL("DROP TABLE IF EXISTS CURSO_ABIERTAUGR");
        db.execSQL("DROP TABLE IF EXISTS ACTIVIDAD");
        db.execSQL("DROP TABLE IF EXISTS MENU_PLATO");
        db.execSQL("DROP TABLE IF EXISTS MENU");
        db.execSQL("DROP TABLE IF EXISTS PLATO_ALERGENO");
        db.execSQL("DROP TABLE IF EXISTS ALERGENO");
        db.execSQL("DROP TABLE IF EXISTS PLATO");
        db.execSQL("DROP TABLE IF EXISTS TARIFA_COMEDOR");
        db.execSQL("DROP TABLE IF EXISTS COMEDOR_HORARIO");
        db.execSQL("DROP TABLE IF EXISTS COMEDOR");
        db.execSQL("DROP TABLE IF EXISTS TUTORIA_RESERVA");
        db.execSQL("DROP TABLE IF EXISTS TUTORIA_HORARIO");
        db.execSQL("DROP TABLE IF EXISTS ASIGNATURA_LIBRO");
        db.execSQL("DROP TABLE IF EXISTS LIBRO");
        db.execSQL("DROP TABLE IF EXISTS EXAMEN");
        db.execSQL("DROP TABLE IF EXISTS SESION_CLASE");
        db.execSQL("DROP TABLE IF EXISTS MATRICULA");
        db.execSQL("DROP TABLE IF EXISTS IMPARTE");
        db.execSQL("DROP TABLE IF EXISTS ASIGNATURA_GRADO");
        db.execSQL("DROP TABLE IF EXISTS ASIGNATURA");
        db.execSQL("DROP TABLE IF EXISTS DESPACHO_PROFESOR");
        db.execSQL("DROP TABLE IF EXISTS ARISTA_NAVEGACION");
        db.execSQL("DROP TABLE IF EXISTS NODO_NAVEGACION");
        db.execSQL("DROP TABLE IF EXISTS FOTO_ESPACIO");
        db.execSQL("DROP TABLE IF EXISTS ESPACIO");
        db.execSQL("DROP TABLE IF EXISTS PLANTA");
        db.execSQL("DROP TABLE IF EXISTS EDIFICIO");
        db.execSQL("DROP TABLE IF EXISTS ADMINISTRADOR");
        db.execSQL("DROP TABLE IF EXISTS PROFESOR");
        db.execSQL("DROP TABLE IF EXISTS ESTUDIANTE");
        db.execSQL("DROP TABLE IF EXISTS GRADO");
        db.execSQL("DROP TABLE IF EXISTS TRADUCCION");
        db.execSQL("DROP TABLE IF EXISTS IDIOMA");
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Activar claves foráneas cada vez que se abre la BD
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys = ON");
        }
    }
}
