# Configuración opcional de Firebase

El proyecto funciona sin Firebase en modo offline. Para habilitar las puntuaciones y el ranking online, cada persona debe configurar su propio proyecto.

1. Crea un proyecto en [Firebase Console](https://console.firebase.google.com/).
2. Añade una aplicación Android con el paquete `com.example.blackjack`.
3. Habilita Authentication con el proveedor anónimo.
4. Crea Firestore Database.
5. Descarga el archivo `google-services.json`.
6. Copia ese archivo en `app/google-services.json`.
7. Ejecuta la aplicación.

El archivo real está ignorado por Git. No debe subirse al repositorio. Usa `app/google-services.json.example` como referencia de ubicación y paquete.

Si `app/google-services.json` no existe, Gradle omite el plugin de Google Services y la aplicación inicia directamente el juego offline.
