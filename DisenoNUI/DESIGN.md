# Design system NUI · ETSIIT

Guía para personas y agentes que tocan la UI de la app Android (`Android/app/src/main/`).
Si eres un agente: lee esta página entera antes de editar un layout.

> **Estado: DISEÑO APROBADO (9 nov 2026), todavía NO aplicado a la app.** Todo vive en `NPI/DisenoNUI/`.
> Referencia visual (demo con menú, Biblioteca, Secretaría y piezas): https://claude.ai/artifact/KnTGoCEMkbhUckEjVFcGYp
> Se edita aquí. Para aplicarlo (solo cuando Izaro lo pida):
> - `bash DisenoNUI/aplicar.sh` copia los recursos a la app (no cambia nada visible).
> - `bash DisenoNUI/aplicar.sh --tema` además conecta el tema global (botones, toolbar y fuente de toda la app).
> - `bash DisenoNUI/revertir.sh` lo quita todo y deja la app como estaba.
> Mientras sea propuesta: no copiar ficheros a mano a `Android/` ni usar `nui_*` nuevos en layouts.

## Identidad: el campus que se puede tocar

La app trata la ETSIIT como un lugar físico, no como una web dentro del móvil.
Cada pantalla se construye con **objetos de oficina y biblioteca** (papel, fichas de catálogo, sellos, etiquetas)
y lleva un toque de **Granada**: el azulejo nazarí y sus colores (granada, cobalto, arrayán) junto al naranja de la escuela.

Tres reglas resumen el estilo:

1. **Objetos, no cajas.** Una tarjeta es un papel apoyado: esquinas casi rectas y sombra dura desplazada (sin difuminar).
   Un registro (libro, asignatura, trámite) es una ficha con línea roja de cabecera. Una confirmación es un sello.
2. **El azulejo es un acento.** Cenefas finas, una trama muy tenue de fondo o un detalle en una cabecera.
   Nunca como fondo a pantalla completa con opacidad alta ni detrás de texto largo.
3. **La interacción imita el objeto.** Se arrastra un papel, se sella un trámite, se pasa una ficha.
   La UI tiene que sugerir el gesto que la app espera.

### Estilo decidido: neobrutalismo granadino

Sombras **duras** (desplazadas, sin difuminar) y colores planos, con ornamento nazarí. Se descartó una versión
"clásica" con sombras suaves y filetes dorados en todo: no la reintroduzcas.

- **Cada pantalla es una sala.** Arriba, cabecera de yesería (`nui_yeseria`) rematada por una **arquería**
  (`TramaAzulejo` + `nui_arqueria`). Abajo, un **zócalo** de alicatado (`TramaAzulejo`, alto `nui_zocalo`)
  con un filete de oro encima.
- **Sombras:** papel = sombra dura beige (`nui_papel_sombra`); seleccionado = sombra naranja (`nui_papel_seleccionado`);
  **flotante o elegido** (botones flotantes, ficha elegida del hub) = sombra dura de **tinta** (`nui_flotante*`).
  Es el único sitio donde la sombra es negra.
- **Cada sección conserva sus objetos y sus gestos.** Se pueden recolocar funciones, nunca quitarlas:
  - Menú: hub 3×3 = **pared de alicatado** (azulejos alternos de color/papel, juntas de yesería) que se inclina
    hacia la ficha elegida; la elegida se agranda con contorno y sombra de tinta. Nivel de inclinación = **fuente**
    vista desde arriba. Novedades = **cartel clavado** con chincheta granada. Evento de hoy = **hoja de taco**.
    Acceso rápido = **billete troquelado** (línea discontinua). Chat = flotante cobalto.
  - Biblioteca: libros **de frente en baldas de madera** (`nui_madera`), con filete de oro y etiquetas
    PRESTADO/BÁSICA; **ojear** = el libro sale de la balda (queda el hueco punteado) y flota torcido.
  - Secretaría: "lo tuyo" = **papeles sobre la mesa**, levemente girados; sello circular con una granada;
    ¿Qué necesitas? = **rosa de los vientos** de 8 puntas; Pedir cita = **número de turno**; Mis gestiones =
    **carpeta**; catálogo = **cajón de fichero** de madera con fichas con pestaña.
- Color por tema: naranja profundo = matrícula, granada = exámenes/convocatorias, cobalto = créditos,
  arrayán = títulos, madera = certificados/otros. Un solo color fuerte por tarjeta.
- El oro (`nui_oro`) es para filetes finos (libros, zócalo, borde del buscador sobre yesería). Nunca para texto.

## Reglas para agentes

- **Nunca escribas colores hex, dp o sp sueltos en layouts/drawables.** Usa `@color/nui_*`, `@dimen/nui_*`, `@style/...Nui...`.
  Si falta un token, añádelo en el fichero de tokens (abajo) con comentario, no lo inventes en el layout.
- **No crees drawables nuevos para fondos de tarjeta o botón.** Usa los componentes de esta guía.
  Si de verdad hace falta un componente nuevo, créalo como `drawable/nui_<nombre>.xml` + `Widget.Nui.<Nombre>`
  en `estilos_nui.xml` y añádelo a la tabla de componentes de este fichero.
- **Botones: `MaterialButton`** (`com.google.android.material.button.MaterialButton`). El tema ya los pinta;
  no pongas `android:background` ni `backgroundTint` a mano.
- **Toolbar: `MaterialToolbar`** (el tema no tiene ActionBar). Ya recibe `Widget.Nui.Toolbar` por defecto.
- Un solo botón principal (naranja) por pantalla. El resto: `Widget.Nui.Boton.Secundario` o `.Texto`.
- Texto sobre almagra (`nui_resaltado`) siempre en `nui_sobre_resaltado` (blanco); en tinta no llega a AA.
  Texto de acento sobre fondo claro: `nui_acento_profundo`.
- Áreas táctiles mínimas de `@dimen/nui_toque_min` (48dp).
- Iconos decorativos: `android:importantForAccessibility="no"`. Iconos que actúan: `android:contentDescription` en español.
- Ficheros compartidos (`themes.xml`, `colores_nui.xml`…): cambios mínimos y avisar al grupo.

## Dónde está cada cosa

Rutas dentro de `DisenoNUI/`; `aplicar.sh` las copia a la misma ruta bajo `Android/app/src/main/`.

| Qué | Fichero |
|---|---|
| Colores ya existentes en la app (no se tocan) | `Android/.../res/values/colores_nui.xml`: superficie, tinta, apagado, linea, acento, suave, tinte |
| Colores nuevos | `res/values/colores_nui_sistema.xml` (values-night los hereda: la app es solo clara) |
| Espaciado, radios, tamaños de texto | `res/values/dimens_nui.xml` |
| Estilos de texto y componentes | `res/values/estilos_nui.xml` |
| Tema global (roles Material 3 → tokens) | `tema_nui.xml`: bloque que `aplicar.sh --tema` inserta entre marcas `NUI:INICIO`/`NUI:FIN` en ambos `themes.xml` |
| Fuentes | `res/font/nui_titular.ttf`, `nui_texto.xml` (familia), `nui_codigo.ttf` |
| Drawables del sistema | `res/drawable/nui_*.xml` |
| Animaciones (transiciones) | `res/anim/nui_*.xml` |
| Trama de azulejo | `java/ui/TramaAzulejo.java` (paquete `com.example.nuietsiit.ui`) + `drawable/nui_azulejo.xml` |

**Para cambiar el aspecto de toda la app**, cambia el valor del token; no busques y reemplaces en layouts.
Para probar un cambio: `aplicar.sh --tema`, compilar, mirar, `revertir.sh`.
Fichero nuevo en `DisenoNUI/res/` → `aplicar.sh` lo recoge solo (values, color, drawable, anim, font).

## Tokens

### Color

| Token | Valor | Uso |
|---|---|---|
| `nui_superficie` | #FFFFFF | Fondo de pantalla |
| `nui_suave` | #F7F4EF | Fondo secundario, bloques agrupados, campos |
| `nui_papel` | #FBF8F2 | Relleno de tarjetas papel |
| `nui_papel_sombra` | #E4DCCB | Sombra dura y borde del papel |
| `nui_linea` | #EBE8E2 | Separadores |
| `nui_tinta` | #14110F | Texto principal, iconos |
| `nui_apagado` | #7B766F | Texto secundario, pistas |
| `nui_resaltado` | #C0502E | **Almagra** (rojo-teja de la Alhambra): botón principal, selección, sombra de elegido, chips marcados. Sustituye a `nui_acento` en el sistema |
| `nui_resaltado_tinte` | #F6E4DC | Fondo de lo seleccionado (antes `nui_tinte`) |
| `nui_sobre_resaltado` | = superficie | Texto e iconos encima de almagra (blanco: contraste AA) |
| `nui_acento` | #FF7A2F | Naranja ETSIIT original: ya no se usa en el sistema NUI (solo pantallas sin migrar) |
| `nui_acento_profundo` | #8E3520 | Almagra oscura: texto/icono de acento sobre claro, pulsado |
| `nui_tinte` | #FFF0E4 | Tinte naranja original (pantallas sin migrar); en NUI usa `nui_resaltado_tinte` |
| `nui_granada` | #A3233A | Sellos, línea de ficha, error |
| `nui_cobalto` | #1F4E79 | Azulejo, enlaces, información |
| `nui_arrayan` | #3C6E47 | Éxito, completado |
| `*_tinte` (granada, cobalto, arrayan) | claros | Fondo suave del color correspondiente |
| `nui_yeseria` | #F3EADB | Cabecera de pantalla y arquería, juntas del hub |
| `nui_yeseria_texto` | #7A5A3A | Etiquetas sobre yesería |
| `nui_oro` | #C9A66B | Filetes finos: libros, zócalo, buscador. Nunca texto |
| `nui_madera` / `nui_madera_oscura` | #8B5E3C / #6B4429 | Baldas, cajón del fichero / canto de la balda |
| `nui_ok` / `nui_info` / `nui_aviso` / `nui_error` | alias | Estados. Usa estos en lógica de estado |
| `nui_velo` | #99000000 | Oscurecer fondos bajo diálogos o fotos |
| `nui_resalte` / `nui_resalte_texto` | acento 16 % / 32 % | Resaltado al tocar (ripple, `colorControlHighlight`) y texto seleccionado. Ya lo pone el tema; en `rippleColor` usa `nui_resalte` |
| `nui_tema_*` (+ `_tinte`) | alias | Color por tema: `matricula` (naranja profundo), `examenes` (granada), `creditos` (cobalto), `titulos` (arrayán), `certificados` (madera), `otros` (apagado). Icono/etiqueta en el fuerte, tesela en el tinte |

### Tipografía

| Estilo (`android:textAppearance`) | Fuente | Uso |
|---|---|---|
| `TextAppearance.Nui.Titular.XL` (32sp) | Zilla Slab Bold | Título de sección. Máx. 1 por pantalla |
| `TextAppearance.Nui.Titular` (24sp) | Zilla Slab Bold | Título de pantalla / tarjeta grande |
| `TextAppearance.Nui.Titular.M` (20sp) | Zilla Slab Bold | Título de tarjeta, toolbar |
| `TextAppearance.Nui.Cuerpo` (16sp) | Atkinson Hyperlegible | Texto normal (también es la fuente por defecto del tema) |
| `TextAppearance.Nui.Cuerpo.Fuerte` | Atkinson Bold | Énfasis |
| `TextAppearance.Nui.Cuerpo.Secundario` (14sp) | Atkinson, apagado | Subtítulos, metadatos |
| `TextAppearance.Nui.Etiqueta` (12sp) | Atkinson Bold, MAYÚSCULAS | Cabecera de grupo: "PRÉSTAMOS ACTIVOS" |
| `TextAppearance.Nui.Codigo` (14sp) | IBM Plex Mono | Datos de oficina: signaturas (`QA76.73 J38`), nº de expediente, aulas (`2.7`), horas, fechas |

### Medidas

Espaciado `nui_espacio_xs/s/m/l/xl/xxl` = 4/8/12/16/24/32dp. Margen lateral de pantalla: `nui_espacio_l`. Entre bloques: `nui_espacio_xl`.
Radios: `nui_radio_ficha` 2dp (fichas, documentos), `nui_radio_papel` 6dp (tarjetas, botones, campos),
`nui_radio_burbuja` 18dp (solo chat y bottom sheets). Sombra de papel: `nui_sombra_papel` 3dp.

## Componentes

| Componente | Cómo se usa | Cuándo |
|---|---|---|
| Papel | `style="@style/Widget.Nui.Papel"` en un contenedor | Tarjeta genérica |
| Papel pulsable | `style="@style/Widget.Nui.Papel.Pulsable"` | Tarjeta que se toca o se selecciona (`setSelected(true)` la pinta naranja) |
| Ficha | `style="@style/Widget.Nui.Ficha"` | Registros: libro, asignatura, profesor, trámite. Primera fila (≈32dp) = cabecera |
| Sello | `<TextView style="@style/Widget.Nui.Sello" android:text="Presentado"/>` | Confirmaciones "oficiales": trámite presentado, libro devuelto, cita confirmada |
| Etiqueta | `<TextView style="@style/Widget.Nui.Etiqueta"/>` | Categoría, estado, filtro |
| Campo | `<EditText style="@style/Widget.Nui.Campo"/>` | Campo simple. Con `TextInputLayout` el tema ya aplica `Widget.Nui.TextInputLayout` |
| Botón principal | `MaterialButton` sin estilo | Acción principal (uno por pantalla) |
| Botón secundario | `style="@style/Widget.Nui.Boton.Secundario"` | Alternativa |
| Botón texto | `style="@style/Widget.Nui.Boton.Texto"` | "Ver todo", "Cancelar" |
| Toolbar | `MaterialToolbar` sin estilo | Barra superior |
| Cabecera de sala | contenedor con `android:background="@color/nui_yeseria"` + arquería debajo | Arriba de cada pantalla de sección |
| Arquería | `<com.example.nuietsiit.ui.TramaAzulejo android:layout_height="@dimen/nui_arqueria" app:tesela="@drawable/nui_arqueria"/>` | Justo bajo la cabecera de yesería |
| Zócalo | `TramaAzulejo` con `android:layout_height="@dimen/nui_zocalo"` y una vista de 2dp `nui_oro` encima | Pie de cada pantalla |
| Flotante | `android:background="@drawable/nui_flotante"` (o `nui_flotante_cobalto`) | Botón flotante hecho con LinearLayout (Chat, Apuntar a los libros); ficha elegida del hub |
| Cenefa de azulejo | `<com.example.nuietsiit.ui.TramaAzulejo android:layout_height="@dimen/nui_cenefa"/>` | Separar zonas dentro de una pantalla |
| Trama de fondo | `TramaAzulejo` alto grande + `android:alpha="0.1"` | Detrás de una cabecera o zona vacía. Nunca detrás de párrafos |
| Chip de filtro | `Chip` sin estilo (el tema aplica `Widget.Nui.Chip`). No pongas `chipBackgroundColor` a mano | Filtros y vistas. Marcado = tinte + borde naranja |
| Chip cobalto | `style="@style/Widget.Nui.Chip.Cobalto"` | Acción especial junto a filtros (Explorar). Activo = cobalto lleno |
| Estantería | `android:background="@drawable/nui_estanteria"` + `BaldaDecoration` (madera) | Rejilla de libros |
| Libro | `ImageView` con `@drawable/nui_libro`, padding `nui_borde_sello` / `nui_libro_canto` | Portada de frente: filete de oro + sombra dura |
| Marca | `@drawable/nui_marca` (granada) / `nui_marca_basica` (arrayán), `TextAppearance.Nui.Etiqueta`, girada | PRESTADO / BÁSICA sobre un libro |
| Cajón de fichero | `android:background="@drawable/nui_cajon"` con fichas dentro | Catálogo de trámites |
| Tesela de icono | `@drawable/nui_tesela` + `backgroundTint` `*_tinte`; icono `nui_ic_*` teñido del color fuerte | Icono de un papel (cita, solicitud, plazo, todo en orden) |
| Objetos de mostrador | `nui_rosa_vientos`, `nui_turno` (+ número en `Codigo`), `nui_carpeta` (alto `nui_icono_objeto`) | ¿Qué necesitas?, Pedir cita, Mis gestiones |
| Aviso | `AvisoDeshacer.mostrar(...)` (Secretaría): `Widget.Nui.Aviso` + tesela con icono, `Etiqueta` de color, texto, `Boton.Secundario` "Deshacer · N" y mecha de almagra que se consume | Deshacer tras una acción destructiva (cancelar cita). Sustituye al Snackbar negro de Material |
| Troquel | `<View style="@style/Widget.Nui.Troquel" android:layout_width="match_parent"/>` | Línea de corte de un resguardo o billete (justificante de Secretaría) |
| Desplegable | `TextInputLayout` con `style="@style/Widget.Nui.TextInputLayout.Desplegable"` | Campo de elegir de una lista (ExposedDropdownMenu) |
| Transición de papel | `overridePendingTransition(R.anim.nui_papel_entra, R.anim.nui_quieto)` al abrir; `nui_quieto` / `nui_papel_sale` al cerrar | Cada pantalla nueva es un papel que se pone encima. Secretaría: `PantallaSecretaria` |
| Tesela estrella | `@drawable/nui_tesela_estrella` + `backgroundTint` `*_tinte`; icono teñido del color fuerte | Estrella nazarí de 8 puntas como fondo de icono (opciones de ¿Qué necesitas?, resultado) |
| Hoja de taco | Banda `nui_granada` con el día de la semana (`Etiqueta`, texto `nui_superficie`), número `Titular`, mes `Codigo`; fondo `nui_papel_estado` | Elegir un día: rejilla de 5 por fila (Pedir cita). Nada de chips cortados en scroll horizontal |
| Iconos | `nui_ic_cita`, `nui_ic_sobre`, `nui_ic_reloj_arena`, `nui_ic_hoja`, `nui_ic_escaner`, `nui_ic_camara`, `nui_ic_granada`, `nui_ic_matricula`, `nui_ic_examen`, `nui_ic_creditos`, `nui_ic_titulo`, `nui_ic_consulta` | Trazo de tinta; se tiñen con `tint`/`iconTint` |

### Plantilla de pantalla de sección

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="@color/nui_superficie">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent" android:layout_height="?attr/actionBarSize"
        android:background="@color/nui_yeseria"
        app:title="Biblioteca" app:navigationIcon="@drawable/ic_arrow_back" />

    <com.example.nuietsiit.ui.TramaAzulejo
        android:layout_width="match_parent" android:layout_height="@dimen/nui_arqueria"
        app:tesela="@drawable/nui_arqueria" />

    <ScrollView android:layout_width="match_parent" android:layout_height="0dp"
        android:layout_weight="1">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical" android:padding="@dimen/nui_espacio_l">

            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:textAppearance="@style/TextAppearance.Nui.Etiqueta"
                android:text="Préstamos activos" />

            <!-- Ficha de un libro -->
            <LinearLayout style="@style/Widget.Nui.Ficha"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/nui_espacio_s" android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="24dp"
                    android:textAppearance="@style/TextAppearance.Nui.Codigo"
                    android:text="QA76.73 J38" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/nui_espacio_m"
                    android:textAppearance="@style/TextAppearance.Nui.Titular.M"
                    android:text="Java: cómo programar" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:textAppearance="@style/TextAppearance.Nui.Cuerpo.Secundario"
                    android:text="Devolver antes del 21/11" />
            </LinearLayout>
        </LinearLayout>
    </ScrollView>

    <!-- Zócalo: filete de oro + alicatado -->
    <View android:layout_width="match_parent" android:layout_height="2dp"
        android:background="@color/nui_oro" />
    <com.example.nuietsiit.ui.TramaAzulejo
        android:layout_width="match_parent" android:layout_height="@dimen/nui_zocalo" />
</LinearLayout>
```
(Necesita `xmlns:app="http://schemas.android.com/apk/res-auto"` en la raíz para `app:title`.)

## Migrar una pantalla existente (receta para agentes)

1. Lee solo el layout de la pantalla y los drawables que usa.
2. Sustituye cada hex por su token con la tabla de abajo. Si un color no está en la tabla, elige el token por **uso**, no por parecido.
3. Sustituye `textSize`/`textColor`/`textStyle` repetidos por un `textAppearance` Nui.
4. Sustituye fondos de tarjeta propios (`bg_card_*`, `fondo_*`, `cuadro_*`) por `Widget.Nui.Papel` / `Ficha`.
   No borres el drawable antiguo si otro layout lo usa (`grep -r "@drawable/<nombre>" res/`).
5. `Button` → `MaterialButton` sin fondo manual.
6. Compila: `bash ./gradlew -q :app:assembleDebug 2>&1 | grep -E "rror|FAIL" | head`.
7. Busca hex que queden: `grep -nE '#[0-9A-Fa-f]{6,8}' res/layout/<pantalla>.xml`.

### Tabla de equivalencias (colores que hay hoy en el código)

| Hex en el código | Token |
|---|---|
| `#FFFFFF` | `nui_superficie` (fondo) o `nui_papel` (tarjeta) |
| `#000000`, `#1A1A1A`, `#333333`, `#0F172A` | `nui_tinta` |
| `#666666`, `#777777`, `#8A8A8E`, `#9E9E9E`, `#64748B` | `nui_apagado` |
| `#EEEEEE`, `#D0D0D0`, `#F5F5F5` | `nui_linea` (borde) o `nui_suave` (fondo) |
| `#FF8C42`, `#FF9800`, `#F25C19` | `nui_acento` (fondo) o `nui_acento_profundo` (texto/icono) |
| `#FFE3D1`, `#FFE7D6`, `#FFFBF7` | `nui_tinte` |
| `#C62828` | `nui_error` |
| `#6366F1` (violeta) | `nui_cobalto` |
| `#99000000` | `nui_velo` |
| `#00000000` | `nui_transparente` |
| `#33FFFFFF` (blanco translúcido sobre foto/naranja) | se mantiene; si se repite, crear token `nui_brillo` |

## Notas del tema

- La fuente por defecto va en `android:textAppearanceSmall/Medium/Large` (`TextAppearance.Nui.Base.*`), **no** en `android:fontFamily`
  del tema: un `fontFamily` global tapa la fuente de cualquier `textAppearance` (los titulares saldrían sin Zilla).
- `elevationOverlayEnabled=false`: M3 teñía de naranja las superficies elevadas (chips dentro de hojas inferiores).
- En una fila horizontal `wrap_content`, los hijos `match_parent` de alto miden 0: usa `wrap_content` + `minHeight` común.

## Qué evitar

- Degradados violeta/azul, sombras difuminadas, todo con radio 16dp+: es el aspecto genérico que no queremos.
- Sombra negra en tarjetas normales: el negro es solo para flotantes y la ficha elegida.
- Quitar los objetos físicos de una sección (libros, papeles, fichas) para dejar listas planas.
- Más de un color de identidad fuerte en la misma tarjeta (naranja **o** granada **o** cobalto).
- Emojis como iconos o en textos de botones y títulos: usa iconos `nui_ic_*`.
- Filas de chips cortadas por el borde: si no caben, que salten de línea (ChipGroup sin `singleLine`) o usa una rejilla.
- Azulejo a pantalla completa o con `alpha` alto detrás de texto.
- Tinta sobre almagra, o volver a usar el naranja neón `nui_acento` en pantallas NUI.
