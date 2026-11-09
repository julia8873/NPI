#!/usr/bin/env bash
# Quita de la app todo lo que puso aplicar.sh. Deja intacto el resto de cambios del grupo.
set -euo pipefail
cd "$(dirname "$0")"
APP=../Android/app/src/main
LISTA=.aplicados

for t in "$APP/res/values/themes.xml" "$APP/res/values-night/themes.xml"; do
  sed -i '/NUI:INICIO/,/NUI:FIN/d' "$t"
done
if [ -f "$LISTA" ]; then
  while read -r f; do rm -f "$f"; done < "$LISTA"
  rm "$LISTA"
fi
rmdir "$APP/res/font" "$APP/res/anim" "$APP/java/com/example/nuietsiit/ui" 2>/dev/null || true
echo "Design system NUI retirado de la app."
