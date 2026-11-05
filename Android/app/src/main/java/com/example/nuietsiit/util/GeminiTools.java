package com.example.nuietsiit.util;

import com.google.ai.client.generativeai.type.FunctionDeclaration;
import com.google.ai.client.generativeai.type.Schema;
import com.google.ai.client.generativeai.type.Tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Define las herramientas (Tools y FunctionDeclarations) expuestas a Gemini AI.
 */
public class GeminiTools {

    public static List<Tool> obtenerHerramientas() {
        List<FunctionDeclaration> funciones = new ArrayList<>();

        // 1. apuntarAActividad
        Map<String, Schema> propApuntar = new HashMap<>();
        propApuntar.put("idActividad", new Schema("INTEGER", "ID numérico de la actividad en la que inscribirse", null, null, null, null, null, null, null));
        Schema schemaApuntar = new Schema("OBJECT", "Parámetros de inscripción", null, null, null, propApuntar, Collections.singletonList("idActividad"), null, null);
        funciones.add(new FunctionDeclaration(
                "apuntarAActividad",
                "Inscribe al usuario activo en una actividad del campus o curso AbiertaUGR mediante su ID numérico",
                (List) Collections.singletonList(schemaApuntar),
                Collections.singletonList("idActividad")
        ));

        // 2. desapuntarDeActividad
        Map<String, Schema> propDesapuntar = new HashMap<>();
        propDesapuntar.put("idActividad", new Schema("INTEGER", "ID numérico de la actividad de la que desapuntarse", null, null, null, null, null, null, null));
        Schema schemaDesapuntar = new Schema("OBJECT", "Parámetros de cancelación", null, null, null, propDesapuntar, Collections.singletonList("idActividad"), null, null);
        funciones.add(new FunctionDeclaration(
                "desapuntarDeActividad",
                "Cancela la inscripción del usuario activo en una actividad del campus por su ID numérico",
                (List) Collections.singletonList(schemaDesapuntar),
                Collections.singletonList("idActividad")
        ));

        // 3. listarActividades
        funciones.add(new FunctionDeclaration(
                "listarActividades",
                "Consulta y lista las actividades del campus y cursos AbiertaUGR disponibles a partir de la fecha actual",
                Collections.emptyList(),
                Collections.emptyList()
        ));

        // 4. buscarProfesor
        Map<String, Schema> propProf = new HashMap<>();
        propProf.put("nombreProfesor", new Schema("STRING", "Nombre o apellido del profesor a buscar en el directorio de la ETSIIT", null, null, null, null, null, null, null));
        Schema schemaProf = new Schema("OBJECT", "Parámetros de búsqueda de profesor", null, null, null, propProf, Collections.singletonList("nombreProfesor"), null, null);
        funciones.add(new FunctionDeclaration(
                "buscarProfesor",
                "Busca información de contacto (email, despacho) y tutorías de un profesor por su nombre",
                (List) Collections.singletonList(schemaProf),
                Collections.singletonList("nombreProfesor")
        ));

        // 5. reservarTutoria
        Map<String, Schema> propTutoria = new HashMap<>();
        propTutoria.put("correoProfesor", new Schema("STRING", "Correo electrónico del profesor", null, null, null, null, null, null, null));
        propTutoria.put("fecha", new Schema("STRING", "Fecha de la tutoría en formato YYYY-MM-DD", null, null, null, null, null, null, null));
        propTutoria.put("idHorario", new Schema("INTEGER", "ID del horario de tutoría seleccionado", null, null, null, null, null, null, null));
        propTutoria.put("motivo", new Schema("STRING", "Motivo breve de la consulta o tutoría", null, null, null, null, null, null, null));
        Schema schemaTutoria = new Schema("OBJECT", "Parámetros de reserva de tutoría", null, null, null, propTutoria, Arrays.asList("correoProfesor", "fecha", "idHorario"), null, null);
        funciones.add(new FunctionDeclaration(
                "reservarTutoria",
                "Crea una reserva de tutoría con un profesor especificando el horario, la fecha y el motivo",
                (List) Collections.singletonList(schemaTutoria),
                Arrays.asList("correoProfesor", "fecha", "idHorario")
        ));

        // 6. consultarMenuComedor
        Map<String, Schema> propComedor = new HashMap<>();
        propComedor.put("nombreComedor", new Schema("STRING", "Nombre o ubicación del comedor (ej: Aynadamar, Fuentenueva, Severo Ochoa, PTS)", null, null, null, null, null, null, null));
        propComedor.put("fecha", new Schema("STRING", "Fecha del menú en formato YYYY-MM-DD", null, null, null, null, null, null, null));
        Schema schemaComedor = new Schema("OBJECT", "Parámetros de consulta de comedor", null, null, null, propComedor, Collections.singletonList("nombreComedor"), null, null);
        funciones.add(new FunctionDeclaration(
                "consultarMenuComedor",
                "Obtiene el menú del día (primeros, segundos y postres) de un comedor universitario",
                (List) Collections.singletonList(schemaComedor),
                Collections.singletonList("nombreComedor")
        ));

        // 7. buscarLibroBiblioteca
        Map<String, Schema> propLibro = new HashMap<>();
        propLibro.put("query", new Schema("STRING", "Título, autor o código ISBN del libro a buscar en la biblioteca", null, null, null, null, null, null, null));
        Schema schemaLibro = new Schema("OBJECT", "Parámetros de búsqueda de libros", null, null, null, propLibro, Collections.singletonList("query"), null, null);
        funciones.add(new FunctionDeclaration(
                "buscarLibroBiblioteca",
                "Busca libros disponibles en la biblioteca de la ETSIIT por título, autor o ISBN",
                (List) Collections.singletonList(schemaLibro),
                Collections.singletonList("query")
        ));

        // 8. consultarTramitesSecretaria
        Map<String, Schema> propTramite = new HashMap<>();
        propTramite.put("categoria", new Schema("STRING", "Categoría opcional (ej: matricula, titulos, reconocimientos, becas)", null, null, null, null, null, null, null));
        Schema schemaTramite = new Schema("OBJECT", "Parámetros de consulta de trámites", null, null, null, propTramite, Collections.emptyList(), null, null);
        funciones.add(new FunctionDeclaration(
                "consultarTramitesSecretaria",
                "Lista los trámites y procedimientos administrativos gestionables en la secretaría de la ETSIIT",
                (List) Collections.singletonList(schemaTramite),
                Collections.emptyList()
        ));

        return Collections.singletonList(new Tool(funciones, null));
    }
}
