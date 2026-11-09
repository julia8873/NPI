#!/usr/bin/env bash
# Aplica el design system NUI a la app. Deshacer: bash DisenoNUI/revertir.sh
#   bash DisenoNUI/aplicar.sh          -> recursos (colores, medidas, estilos, drawables, fuentes, TramaAzulejo).
#                                         No cambia nada visible: solo los deja disponibles.
#   bash DisenoNUI/aplicar.sh --tema   -> además conecta el tema global (botones, toolbar, fuente de TODA la app).
set -euo pipefail
cd "$(dirname "$0")"
APP=../Android/app/src/main
LISTA=.aplicados   # ficheros copiados, para poder revertir

copiar() { # origen destino
  if [ -e "$2" ] && ! grep -qxF "$2" "$LISTA" 2>/dev/null; then
    echo "ERROR: $2 ya existe en la app y no es nuestro. No toco nada más." >&2; exit 1
  fi
  mkdir -p "$(dirname "$2")"; cp "$1" "$2"
  grep -qxF "$2" "$LISTA" 2>/dev/null || echo "$2" >> "$LISTA"
}

for f in res/values/*.xml res/color/*.xml res/drawable/*.xml res/anim/*.xml res/font/*; do copiar "$f" "$APP/$f"; done
copiar java/ui/TramaAzulejo.java "$APP/java/com/example/nuietsiit/ui/TramaAzulejo.java"
echo "Recursos aplicados ($(wc -l < "$LISTA") ficheros)."

if [ "${1:-}" = "--tema" ]; then
  for t in "$APP/res/values/themes.xml" "$APP/res/values-night/themes.xml"; do
    grep -q "NUI:INICIO" "$t" && continue
    # inserta el bloque justo antes del cierre del estilo Base.Theme.NUIETSIIT
    awk -v bloque="$(cat tema_nui.xml)" '
      /<style name="Base.Theme.NUIETSIIT"/ {dentro=1}
      dentro && /<\/style>/ {print bloque; dentro=0}
      {print}' "$t" > "$t.tmp" && mv "$t.tmp" "$t"
  done
  echo "Tema global conectado."
fi
