# Taller de Persistencia en Android — Universidad de Nariño
**Materia:** Desarrollo de Aplicaciones Móviles  
**Docente:** MSc. Martha Nubia Carrillo Obando  

---

## 📌 Punto 2: Almacenamiento Local Permanente con Room (App A)
- **Patrón:** Offline-First con arquitectura MVVM (Entidad, DAO, Repositorio, UI Compose).
- **Entidad `Tarea`:**
  - `id` (Clave primaria autogenerada)
  - `titulo` (Texto de la tarea)
  - `descripcion` (Detalle opcional)
  - `estado_completado` (Booleano)
  - `fecha_creacion` (Timestamp en milisegundos)
  - `prioridad` (BAJA, MEDIA, ALTA)
  - `categoria` (Universidad, Trabajo, Proyectos, Personal, Urgente)
  - `sincronizado` (Bandera para sincronización en la nube)
  - `fecha_limite` (Fecha límite opcional)
- **Persistencia:** Base de datos SQLite gestionada a través de Room (`tareas_offline_db`).
- **Operaciones CRUD:** Inserción, lectura reactiva mediante `Flow<List<Tarea>>`, actualización individual y en lote, y eliminación.

---

## 📌 Punto 3: Compartición de Datos Inter-Aplicación con Content Providers

### 🏛️ Arquitectura del Ecosistema:
El ecosistema se compone de dos aplicaciones independientes instaladas en el dispositivo/emulador:

1. **App A (Esta aplicación - `com.example.tareas` / `com.aistudio.todo.cdxz`):**
   - **Rol:** Proveedora de Datos (*Server*).
   - **ContentProvider:** `com.example.tareas.data.TareaContentProvider`
   - **Autoridad:** `com.example.tareas.provider`
   - **URI de Contenido:** `content://com.example.tareas.provider/tareas`
   - **Permisos personalizados definidos en el Manifest:**
     - `com.example.tareas.READ_TAREAS`: Permiso para consultar las tareas guardadas.
     - `com.example.tareas.WRITE_TAREAS`: Permiso para crear o editar tareas remotamente.
   - **Métodos CRUD implementados en el Provider:**
     - `query()`: Expone un `Cursor` con proyección, selección y ordenamiento sobre la tabla de Room.
     - `insert()`: Inserta registros y notifica cambios a observadores mediante `contentResolver.notifyChange()`.
     - `update()`: Actualiza registros por ID o criterios y notifica observadores.
     - `delete()`: Elimina registros por ID o criterios y notifica observadores.
     - `getType()`: Retorna tipos MIME `vnd.android.cursor.dir/...` y `vnd.android.cursor.item/...`.

2. **App B (Aplicación Independiente Cliente - Lector):**
   - **Rol:** Consumidora de Datos (*Client*).
   - **Mecanismo:** `context.contentResolver.query(CONTENT_URI, null, null, null, null)`
   - **Permiso solicitado:** `<uses-permission android:name="com.example.tareas.READ_TAREAS" />`
   - **Declaración de visibilidad de paquete (Android 11+):**
     ```xml
     <queries>
         <provider android:authorities="com.example.tareas.provider" />
     </queries>
     ```

---

## 🧪 Pruebas y Validación de Seguridad (Rúbrica de Evaluación)

1. **Acceso exitoso con permiso:**
   - Al instalar la App A y la App B con el permiso `com.example.tareas.READ_TAREAS` declarado, la App B lee y muestra de inmediato las tareas persistidas en la App A.
2. **Seguridad (Revocación o ausencia de permiso):**
   - Si en el `AndroidManifest.xml` de la App B se comenta o retira `<uses-permission android:name="com.example.tareas.READ_TAREAS" />`, Android lanza un `SecurityException` de denegación de permisos, impidiendo la lectura no autorizada.
3. **Manejo de proveedor no instalado:**
   - Si se desinstala la App A, `contentResolver.query(...)` devuelve `null` o captura la excepción indicando que el proveedor no se encuentra disponible.
