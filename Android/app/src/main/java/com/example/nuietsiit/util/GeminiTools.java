package com.example.nuietsiit.util;

import com.google.ai.client.generativeai.type.FunctionDeclaration;
import com.google.ai.client.generativeai.type.Schema;
import com.google.ai.client.generativeai.type.Tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Define las herramientas (Tools y FunctionDeclarations) expuestas a Gemini AI.
 */
public class GeminiTools {

    public static List<Tool> obtenerHerramientas() {
        List<FunctionDeclaration> funciones = new ArrayList<>();

        // 1. apuntarAActividad
        Schema p1Act = new Schema("INTEGER", "ID numérico de la actividad en la que inscribirse", null, null, null, null, null, null, null);
        funciones.add(new FunctionDeclaration(
                "apuntarAActividad",
                "Inscribe al usuario activo en una actividad del campus o curso AbiertaUGR mediante su ID numérico",
                (List) Collections.singletonList(p1Act),
                Collections.singletonList("idActividad")
        ));

        // 2. desapuntarDeActividad
        Schema p1Des = new Schema("INTEGER", "ID numérico de la actividad de la que desapuntarse", null, null, null, null, null, null, null);
        funciones.add(new FunctionDeclaration(
                "desapuntarDeActividad",
                "Cancela la inscripción del usuario activo en una actividad del campus por su ID numérico",
                (List) Collections.singletonList(p1Des),
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
        Schema p1Prof = new Schema("STRING", "Nombre o apellido del profesor a buscar en el directorio de la ETSIIT", null, null, null, null, null, null, null);
        funciones.add(new FunctionDeclaration(
                "buscarProfesor",
                "Busca información de contacto (email, despacho) y tutorías de un profesor por su nombre",
                (List) Collections.singletonList(p1Prof),
                Collections.singletonList("nombreProfesor")
        ));

        // 5. reservarTutoria
        Schema p1TutCorreo = new Schema("STRING", "Correo electrónico del profesor", null, null, null, null, null, null, null);
        Schema p2TutFecha = new Schema("STRING", "Fecha de la tutoría en formato YYYY-MM-DD", null, null, null, null, null, null, null);
        Schema p3TutHorario = new Schema("INTEGER", "ID del horario de tutoría seleccionado", null, null, null, null, null, null, null);
        Schema p4TutMotivo = new Schema("STRING", "Motivo breve de la consulta o tutoría", null, null, null, null, null, null, null);
        funciones.add(new FunctionDeclaration(
                "reservarTutoria",
                "Crea una reserva de tutoría con un profesor especificando el horario, la fecha y el motivo",
                (List) Arrays.asList(p1TutCorreo, p2TutFecha, p3TutHorario, p4TutMotivo),
                Arrays.asList("correoProfesor", "fecha", "idHorario")
        ));

        // 6. consultarMenuComedor
        Schema p1ComedorName = new Schema("STRING", "Nombre o ubicación del comedor (ej: ETSIIT, Aynadamar, Fuentenueva, Bellas Artes, Cartuja). Si no se especifica, usar 'ETSIIT'", null, null, null, null, null, null, null);
        Schema p2ComedorFecha = new Schema("STRING", "Fecha del menú en formato YYYY-MM-DD. Si no se especifica, usar la fecha de hoy", null, null, null, null, null, null, null);
        Schema p3ComedorVeg = new Schema("BOOLEAN", "true si el usuario solicita específicamente el menú vegetariano", null, null, null, null, null, null, null);
        funciones.add(new FunctionDeclaration(
                "consultarMenuComedor",
                "Obtiene el menú del día (primeros, segundos y postres) de un comedor universitario para cualquier fecha",
                (List) Arrays.asList(p1ComedorName, p2ComedorFecha, p3ComedorVeg),
                Collections.emptyList()
        ));

        // 7. buscarLibroBiblioteca
        Schema p1Libro = new Schema("STRING", "Título, autor o código ISBN del libro a buscar en la biblioteca", null, null, null, null, null, null, null);
        funciones.add(new FunctionDeclaration(
                "buscarLibroBiblioteca",
                "Busca libros disponibles en la biblioteca de la ETSIIT por título, autor o ISBN",
                (List) Collections.singletonList(p1Libro),
                Collections.singletonList("query")
        ));

        // 8. consultarTramitesSecretaria
        Schema p1Tramite = new Schema("STRING", "Categoría opcional (ej: matricula, titulos, reconocimientos, becas)", null, null, null, null, null, null, null);
        funciones.add(new FunctionDeclaration(
                "consultarTramitesSecretaria",
                "Lista los trámites y procedimientos administrativos gestionables en la secretaría de la ETSIIT",
                (List) Collections.singletonList(p1Tramite),
                Collections.emptyList()
        ));

        return Collections.singletonList(new Tool(funciones, null));
    }
}
