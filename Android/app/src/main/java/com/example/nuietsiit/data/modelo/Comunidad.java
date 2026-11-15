package com.example.nuietsiit.data.modelo;

import androidx.annotation.ColorRes;

import com.example.nuietsiit.R;

/** Un grupo de chat de la escuela (tabla COMUNIDAD_CHAT). */
public class Comunidad {

    public int id;
    public String nombre;
    public String plataforma;   // 'whatsapp'|'telegram'|'discord'
    public String enlace;       // no se abre: la app muestra una vista previa simulada
    public String descripcion;  // puede ser null
    public Integer idGrado;     // null = comunidad general para todos los grados

    // -------------------- Aspecto según la plataforma --------------------

    // Color de cada plataforma: el de su marca (colores_secciones.xml)
    @ColorRes
    public static int colorRes(String plataforma) {
        switch (plataforma) {
            case "whatsapp": return R.color.sec_whatsapp;
            case "telegram": return R.color.sec_telegram;
            default: return R.color.sec_discord;
        }
    }

    @ColorRes
    public static int tinteRes(String plataforma) {
        switch (plataforma) {
            case "whatsapp": return R.color.sec_whatsapp_tinte;
            case "telegram": return R.color.sec_telegram_tinte;
            default: return R.color.sec_discord_tinte;
        }
    }

    public static String nombrePlataforma(String plataforma) {
        switch (plataforma) {
            case "whatsapp": return "WhatsApp";
            case "telegram": return "Telegram";
            default: return "Discord";
        }
    }

    // -------------------- Datos de la simulación --------------------

    /** Número de miembros inventado, pero siempre el mismo para cada grupo. */
    public int miembrosSimulados() {
        return 35 + (id * 97) % 400;
    }

    /** Conversación de ejemplo para la vista previa del grupo: {autor, mensaje}. */
    public String[][] mensajesSimulados() {
        switch (plataforma) {
            case "whatsapp":
                return new String[][]{
                        {"Delegación", "Recordad que el jueves hay asamblea de estudiantes 📢"},
                        {"Lucía", "¿A qué hora era? 👀"},
                        {"Delegación", "A las 12:00 en el salón de actos"},
                        {"Pablo", "Allí estaremos 🙌"}
                };
            case "telegram":
                return new String[][]{
                        {"Admin", "📌 ¡Bienvenid@! Aquí compartimos todo lo útil del curso"},
                        {"Admin", "📎 Subidos los apuntes del tema 3 y exámenes de otros años"},
                        {"Marta", "¡Mil gracias! 🙏"}
                };
            default:
                return new String[][]{
                        {"Moderador", "👋 ¡Bienvenid@s! Leed las normas en #reglas"},
                        {"Álex", "¿Alguien se apunta al CTF del viernes? 🏴"},
                        {"Sara", "¡Yo! 🙋‍♀️ Hacemos equipo"}
                };
        }
    }
}
