# Cómo obtener el SHA-1 fingerprint para Firebase

## Método 1: Usando Gradle (Recomendado)

1. Abre una terminal en la raíz del proyecto
2. Ejecuta uno de estos comandos:

### Para Windows (PowerShell):
```powershell
cd android
.\gradlew signingReport
```

### Para Windows (CMD):
```cmd
cd android
gradlew signingReport
```

### Para Mac/Linux:
```bash
cd android
./gradlew signingReport
```

3. Busca en la salida algo como:
```
Variant: debug
Config: debug
Store: C:\Users\...\.android\debug.keystore
Alias: AndroidDebugKey
SHA1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
```

## Método 2: Usando keytool directamente

### Para Windows:
```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

### Para Mac/Linux:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

## Agregar SHA-1 a Firebase Console

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto: **sivareats-bb057**
3. Ve a **Configuración del proyecto** (ícono de engranaje)
4. En la sección **Tus aplicaciones**, selecciona tu app Android
5. Haz clic en **Agregar huella digital**
6. Pega el SHA-1 que obtuviste
7. Haz clic en **Guardar**

## Nota importante

- Necesitas agregar tanto el SHA-1 de **debug** como el de **release** (si tienes uno)
- Después de agregar el SHA-1, descarga el nuevo `google-services.json` y reemplázalo en `app/google-services.json`
- Puede tomar unos minutos para que los cambios surtan efecto

