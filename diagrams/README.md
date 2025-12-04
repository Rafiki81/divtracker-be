# DivTracker Architecture Diagrams

Diagramas de arquitectura generados con [diagrams](https://diagrams.mingrammer.com).

## Requisitos

```bash
# Instalar Graphviz (requerido por diagrams)
brew install graphviz

# Crear entorno virtual e instalar dependencias
cd diagrams
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Generar Diagramas

```bash
# Generar todos los diagramas
python generate_all.py

# O generar individualmente
python aws_architecture.py
python backend_components.py
python data_flow.py
python fcm_flow.py
```

## Diagramas Disponibles

| Archivo | Descripción |
|---------|-------------|
| `aws_architecture.py` | Arquitectura de infraestructura en AWS |
| `backend_components.py` | Componentes internos del backend Spring Boot |
| `data_flow.py` | Flujo de datos desde Finnhub hasta Android |
| `fcm_flow.py` | Flujo de notificaciones push con Firebase |

## Salida

Los diagramas se generan en formato PNG en la misma carpeta.
