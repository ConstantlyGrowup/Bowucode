@echo off
echo Starting Chroma DB...
echo.
echo If you don't have Chroma installed, please run:
echo pip install chromadb
echo.
echo Starting Chroma on http://localhost:8000
chroma run --host localhost --port 8000 --path ./chroma_data
pause
