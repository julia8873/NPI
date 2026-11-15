# ETSIIT Go

Práctica de **Nuevos Paradigmas de Interacción** (curso 2026/27, ETSIIT – Universidad de Granada).

ETSIIT Go es una app Android para los estudiantes de la escuela: horario, profesorado, calendario, actividades, noticias, comedores, mapa, biblioteca, secretaría y un asistente con IA. Lo que hemos intentado es que no se use solo pulsando botones, sino con gestos y sensores del móvil: inclinarlo para pasar páginas, agitarlo para volver al menú, dejarlo en la mesa para estudiar, escanear códigos con la cámara, firmar con la huella, etc.

## Contenido

- [Qué hace la app](#qué-hace-la-app)
- [Lenguaje y versiones](#lenguaje-y-versiones)
- [Librerías](#librerías)
- [Qué hace falta instalar](#qué-hace-falta-instalar)
- [Cómo abrir el proyecto](#cómo-abrir-el-proyecto)
- [Cómo instalar la app en el móvil](#cómo-instalar-la-app-en-el-móvil)
- [Probar el QR y el escáner de libros](#probar-el-qr-y-el-escáner-de-libros)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Problemas habituales](#problemas-habituales)
- [Permisos](#permisos)
- [Autores](#autores)

## Qué hace la app

Hay algunos gestos que funcionan en toda la app:

- **Agitar el móvil** te devuelve al menú principal.
- **Inclinar a izquierda o derecha** pasa de ficha, de estante o de página (ninguna pantalla tiene scroll).
- Si pones el móvil **boca abajo o en el bolsillo** durante una clase, se silencia solo, y al sacarlo vuelve a sonar.
- La primera vez que entras en una pantalla sale un **tutorial** que te pide hacer el gesto para seguir. Se puede repetir con el botón **?** de arriba.

Y cada sección tiene lo suyo:

| Sección | Cómo se usa |
|---|---|
| Menú principal | Cuadrícula de secciones que se recorre inclinando el móvil |
| Asignaturas | La ruleta de cursos gira al girar el móvil, como un volante |
| Profesorado | Buscador que aguanta faltas de ortografía y ficha de cada profesor |
| Calendario | Eventos del curso y recordatorios |
| Actividades | Deslizas para apuntarte; si acercas el móvil hacia ti, la guardas para luego |
| Noticias | Avisos en tarjetas, los urgentes primero |
| Comedores | Ordenados por cercanía con el GPS; inclinando hacia atrás ves el menú vegetariano |
| Mapa | Mapa de OpenStreetMap con rutas a pie y en bus, brújula y guía por voz. Dentro del edificio, planos por planta y QR para saber dónde estás |
| Biblioteca | Los libros están en baldas que se tuercen al inclinar el móvil. Se pueden escanear ISBN con la cámara, y si dejas el móvil quieto en la mesa empieza un Pomodoro |
| Secretaría | Trámites paso a paso, cita previa, firma con el dedo o con la huella y justificante con QR. Es simulada: todo se guarda en el móvil y no se manda nada a la UGR |
| Asistente | Chat con Gemini que conoce los datos de la app |

## Lenguaje y versiones

- **Lenguaje:** Java (nivel de código Java 11), con las pantallas en XML.
- **Base de datos:** SQLite local (no hay servidor).
- **Gradle:** 9.6.0 (viene con el wrapper, no hay que instalarlo).
- **Android Gradle Plugin:** 9.4.1.
- **compileSdk / targetSdk:** 37.
- **minSdk:** 29, es decir, funciona a partir de **Android 10**.
- **JDK:** 17 o superior para compilar. Vale el que trae Android Studio.

Las versiones de todas las librerías están en [`Android/gradle/libs.versions.toml`](Android/gradle/libs.versions.toml).

## Librerías

No hay que descargar ninguna a mano: Android Studio (Gradle) las baja de Google Maven y Maven Central al sincronizar el proyecto. Aquí están los enlaces por si se quieren consultar.

| Librería | Versión | Para qué la usamos |
|---|---|---|
| [AppCompat](https://developer.android.com/jetpack/androidx/releases/appcompat) `androidx.appcompat:appcompat` | 1.6.1 | Base de las Activities |
| [Material Components](https://github.com/material-components/material-components-android) `com.google.android.material:material` | 1.10.0 | Barra superior, chips, paneles inferiores… |
| [ConstraintLayout](https://developer.android.com/jetpack/androidx/releases/constraintlayout) `androidx.constraintlayout:constraintlayout` | 2.1.4 | Layouts |
| [Activity](https://developer.android.com/jetpack/androidx/releases/activity) `androidx.activity:activity-ktx` | 1.8.0 | Pedir permisos y abrir la cámara |
| [Annotation](https://developer.android.com/jetpack/androidx/releases/annotation) `androidx.annotation:annotation` | 1.11.0 | Anotaciones como `@NonNull` |
| [Biometric](https://developer.android.com/jetpack/androidx/releases/biometric) `androidx.biometric:biometric` | 1.1.0 | Firmar con la huella en Secretaría |
| [ZXing Android Embedded](https://github.com/journeyapps/zxing-android-embedded) `com.journeyapps:zxing-android-embedded` | 4.3.0 | Leer y generar QR y códigos de barras |
| [osmdroid](https://github.com/osmdroid/osmdroid) `org.osmdroid:osmdroid-android` | 6.1.18 | Mapa de OpenStreetMap |
| [Gemini SDK](https://ai.google.dev/) `com.google.ai.client.generativeai:generativeai` | 0.9.0 | El asistente |
| [Guava](https://github.com/google/guava) `com.google.guava:guava` | 33.0.0-android | Necesaria para llamar a Gemini desde Java |

Para los tests: JUnit 4.13.2, AndroidX Test JUnit 1.1.5 y Espresso 3.5.1.

Aparte usamos varias APIs de Android que no necesitan nada extra: sensores (acelerómetro, giroscopio, rotación y luz), ubicación, texto a voz, vibración, notificaciones y audio (para el modo silencio).

## Qué hace falta instalar

- [**Android Studio**](https://developer.android.com/studio), una versión reciente. Ya trae el JDK, el SDK y el emulador.
- El **SDK de Android 37**. Si no lo tienes, Android Studio lo pide al abrir el proyecto.
- [**Git**](https://git-scm.com/downloads), solo si quieres clonar el repositorio (también se puede bajar en ZIP).
- Un **móvil con Android 10 o superior**. Recomendamos probarla en un móvil de verdad, porque casi todo depende de los sensores. En el emulador se pueden mover los sensores a mano, pero no es lo mismo.
- Una **clave de Gemini** ([se saca aquí](https://aistudio.google.com/apikey)), solo si se quiere usar el asistente.

## Cómo abrir el proyecto

1. Descargar el código:

   ```bash
   git clone https://github.com/julia8873/NPI.git
   ```

   o desde GitHub con **Code → Download ZIP**. La versión entregada es la de la rama `main`.

2. En Android Studio, **File → Open** y elegir la carpeta **`NPI/Android`**.
   Ojo: hay que abrir esa carpeta, no la raíz del repositorio, porque ahí es donde está el proyecto de Android.

3. Esperar a que acabe el *Gradle Sync*. La primera vez tarda un rato porque descarga Gradle y las librerías.

4. Si se quiere usar el asistente, añadir la clave en `Android/local.properties` (el fichero lo crea Android Studio):

   ```properties
   GEMINI_API_KEY=tu_clave
   ```

   Sin la clave el resto de la app funciona igual.

5. Conectar el móvil por USB con la depuración USB activada, o crear un emulador en *Device Manager*, y darle a **Run ▶**.

6. Dentro de la app, crear una cuenta en **Registrarse** y entrar.

## Cómo instalar la app en el móvil

Desde la terminal, dentro de `NPI/Android`:

```bash
./gradlew installDebug        # en Windows: gradlew.bat installDebug
```

O generar el APK con `./gradlew assembleDebug` (o *Build → Build APK(s)* en Android Studio). Queda en `Android/app/build/outputs/apk/debug/app-debug.apk` y se puede pasar al móvil e instalar desde ahí (hay que permitir instalar apps de origen desconocido).

## Probar el QR y el escáner de libros

Para no tener que ir a la escuela, en [`Documentacion/`](Documentacion) hay imágenes para probar la cámara desde la pantalla del ordenador:

- [`QR_ubicacion/`](Documentacion/QR_ubicacion): un QR por planta. Se usan en *Mapa → Planos ETSIIT → Iniciar ruta → "No, estoy dentro" → Escanear QR*. Más detalles en su [`LEEME.md`](Documentacion/QR_ubicacion/LEEME.md).
- [`ISBN_biblioteca/hoja_codigos.png`](Documentacion/ISBN_biblioteca/hoja_codigos.png): códigos de barras de libros del catálogo para el escáner de la Biblioteca. Se pueden leer varios a la vez.

## Estructura del repositorio

```text
NPI/
├── Android/                  proyecto de Android Studio (abrir esta carpeta)
│   ├── app/src/main/
│   │   ├── java/com/example/nuietsiit/
│   │   │   ├── activities/   pantallas: auth (login), main (menú, ajustes) y secciones
│   │   │   ├── data/         base de datos SQLite
│   │   │   ├── ui/           vistas reutilizables
│   │   │   └── util/         detectores de gestos, Gemini, notificaciones, tutorial
│   │   ├── res/              layouts, colores, textos e imágenes
│   │   └── assets/           planos de la escuela y rutas del mapa
│   └── gradle/libs.versions.toml
├── Documentacion/            memoria y material para probar (QR, ISBN)
├── Modelo 3D ETSIIT/         modelo de SketchUp/STL y capturas por planta
├── LeapMotion/               práctica 2 (en desarrollo)
└── Voz/                      práctica 2 (en desarrollo)
```

Todas las pantallas heredan de `BaseActivity`, que se encarga de lo común (agitar para volver, idioma y tutorial). Cada gesto tiene su propia clase en `util/` (`DetectorInclinacion`, `DetectorBocaAbajo`, `DetectorEnsenar`…) para poder reutilizarlo entre secciones.

## Problemas habituales

- **Falla el Gradle Sync:** comprobar la conexión y probar *File → Invalidate Caches → Restart*. Si se queja del JDK, en *Settings → Build Tools → Gradle* elegir un JDK 17 o superior.
- **"SDK location not found":** abrir el proyecto desde Android Studio para que cree `local.properties`, o crearlo con `sdk.dir=/ruta/al/Android/Sdk`.
- **Falta `android-37`:** instalarlo desde *Tools → SDK Manager*.
- **El asistente no contesta:** falta la clave de Gemini o no es válida. Después de ponerla hay que recompilar.
- **En el emulador los gestos no hacen nada:** moverlo desde *Extended controls (⋯) → Virtual sensors*, o probar en un móvil.

## Permisos

La app pide permisos solo cuando los necesita: ubicación (comedores y mapa), cámara (QR e ISBN), notificaciones (recordatorios), acceso a "No molestar" (silenciar en clase y modo estudio), vibración e Internet (mapa, portadas de libros y asistente). Si se deniega alguno, esa parte deja de funcionar pero el resto de la app sigue.

## Autores

- Izaro Cuerdo Larraza – [@izaro-c](https://github.com/izaro-c)
- Julia Wenjing Mirallez Martínez– [@julia8873](https://github.com/julia8873)
- Miguel Ángel Fernández Rodríguez – [@miguefdezz13](https://github.com/miguefdezz13)
- Nuria Ruiz Arroyo– [@nuriara13](https://github.com/nuriara13)
- Ramón Domech León – [@RamonDLUgr](https://github.com/RamonDLUgr)
