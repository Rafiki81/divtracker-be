#!/bin/bash

# Este script ahora delega al Makefile
# Usa: make run (recomendado) o make run-dev (con docker-compose)

echo "⚠️  Este script está deprecado. Usa el Makefile en su lugar:"
echo ""
echo "  make run       - Ejecuta con Testcontainers"
echo "  make run-dev   - Ejecuta con Docker Compose"
echo "  make help      - Muestra todos los comandos disponibles"
echo ""
echo "Ejecutando 'make run' en 3 segundos..."
sleep 3

make run
