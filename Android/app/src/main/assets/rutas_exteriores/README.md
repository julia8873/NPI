# Rutas exteriores

- `red_transporte.json`: lugares (residencia de ejemplo, ETSIIT y Edificio Auxiliar con sus contornos), línea 9 en
  los dos sentidos (trazado y paradas reales de OpenStreetMap, horarios simulados) y tramos a pie precalculados.
  Lo lee `RedTransporte` (`CapaLugares.cargarRed(context)` en la app).

## Fotos de la ruta

Cada paso de la guía enseña una foto del sitio. Mientras no exista, `FotoLugar` genera una tarjeta
"Foto pendiente" con el nombre del sitio, así que se pueden añadir poco a poco.

Convención: `res/drawable/ruta_ext_<id>_<n>.jpg` (o `.webp`/`.png`), con `n` = 1, 2, 3… si hay varias del mismo
sitio (la 1 es la principal). Todo en minúsculas. Horizontales 4:3, ~1200 px de ancho como mucho.

| Foto | `<id>` | Ejemplo de nombre |
|---|---|---|
| Puerta de la residencia Livensa Fuentenueva | `livensa_fuentenueva` | `ruta_ext_livensa_fuentenueva_1.jpg` |
| Parada Camino de Ronda 210 (subida, hacia Chana) | `p1349524447` | `ruta_ext_p1349524447_1.jpg` |
| Bus de la línea 9 | `bus_9` | `ruta_ext_bus_9_1.jpg` |
| Parada Av. de Andalucía – Bellas Artes (bajada) | `p5930737442` | `ruta_ext_p5930737442_1.jpg` |
| Camino y entrada de la ETSIIT | `etsiit` | `ruta_ext_etsiit_1.jpg`, `ruta_ext_etsiit_2.jpg` |
| Entrada del Edificio Auxiliar | `edificio_auxiliar` | `ruta_ext_edificio_auxiliar_1.jpg` |
| Parada Periodista Rafael Gómez Montero 3 (vuelta, hacia Los Rebites) | `p6043563032` | `ruta_ext_p6043563032_1.jpg` |
| Parada Camino de Ronda 177 (bajada de vuelta) | `p6043563041` | `ruta_ext_p6043563041_1.jpg` |
