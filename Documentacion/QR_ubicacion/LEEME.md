# QR de ubicación de la ETSIIT

Cada QR contiene el texto `NPI-ETSIIT:<id>` (por ejemplo `NPI-ETSIIT:p0_hall`).
La app lo usa en **Mapa → Planos ETSIIT → punto naranja → Iniciar ruta → "No, estoy dentro" → Escanear QR**.

| Fichero | Dónde pegarlo (provisional) |
|---|---|
| `qr_sotano_hall.png` | Hall del sótano |
| `qr_p0_hall.png` | Hall de la planta baja |
| `qr_p0_pasillo_este.png` | Pasillo este de la planta baja |
| `qr_p1_hall.png` … `qr_p5_hall.png` | Hall de cada planta |

La posición de cada QR en el plano se define en
`Android/app/src/main/java/com/example/nuietsiit/activities/secciones/mapa/RegistroPuntosQR.java`
(coordenadas 0..1 sobre el plano). Si pegáis un QR en otro sitio, cambiad ahí `x` e `y`.
Para añadir un QR nuevo: una línea `qr(...)` en ese fichero y un PNG con el texto `NPI-ETSIIT:<id>`.

## Fotos del visor de ruta
Nombre: `ruta_<id>_<grados>.jpg` en `Android/app/src/main/res/drawable-nodpi/`
(`<id>` = id del punto de partida, p. ej. `entrada` o `p0_hall`; `<grados>` múltiplo de 15:
0 = norte, 90 = este, 180 = sur, 270 = oeste). Todo en minúsculas.
Ahora solo existe `ruta_demo_0.jpg` (foto de prueba hacia el norte, se usa si el punto no tiene fotos propias).
