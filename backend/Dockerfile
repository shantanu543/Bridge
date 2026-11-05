FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /srv/bridge
COPY pyproject.toml ./
COPY alembic.ini ./
COPY backend/ ./backend/
RUN pip install --no-cache-dir . \
    && groupadd --system bridge \
    && useradd --system --gid bridge --home-dir /nonexistent bridge

USER bridge
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
