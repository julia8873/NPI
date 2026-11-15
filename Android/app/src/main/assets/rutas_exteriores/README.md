# Rutas exteriores

- `red_transporte.json`: lugares (residencia de ejemplo, ETSIIT y Edificio Auxiliar con sus contornos), línea 9 en
  los dos sentidos (trazado y paradas reales de OpenStreetMap, horarios simulados) y tramos a pie precalculados.
  Lo lee `RedTransporte` (`CapaLugares.cargarRed(context)` en la app).

## Fotos de la ruta

`fotos_ruta.json`: puntos de foto de cada tramo a pie (salida, cada giro, cada ~60 m como mucho y llegada), con
posición, rumbo (hacia dónde mira la foto), giro e instrucción. Lo genera `osm/puntos_foto.py` (fuera del repo;
las instrucciones se corrigen allí, en `CORRECCIONES`, y se regenera) y lo lee `FotosRutaExterior`.
El mismo script crea `osm/fotos_ruta.html` (mapa con los puntos numerados para llevar en el móvil) y `fotos_ruta.gpx`.

Mientras falte una foto, `FotoLugar` genera una tarjeta provisional (instrucción + flecha del giro, o icono + nombre),
así que se pueden añadir poco a poco, sin tocar código: basta con copiarlas a `res/drawable/`.

Formato: horizontales 4:3, ~1200 px de ancho como mucho, `.jpg` (o `.webp`/`.png`), todo en minúsculas,
hechas en el punto y mirando en el sentido de la marcha (la flecha del mapa).

- Puntos del camino: `ruta_ext_<id del punto>.jpg`, p. ej. `ruta_ext_bellasartes_etsiit_03.jpg` (lista en el HTML).
- Lugares, paradas y bus: `ruta_ext_<id>_<n>.jpg`, con `n` = 1, 2, 3… (la 1 es la principal):

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
