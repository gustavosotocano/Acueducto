# RNF-017 — Virtual threads y contexto de tenant sin ThreadLocal

| Campo | Valor |
|-------|-------|
| **Issue** | https://github.com/gustavosotocano/Acueducto/issues/33 |
| **ID** | RNF-017 |
| **Tipo** | Historia técnica / rendimiento y mantenibilidad |
| **Épica** | Arquitectura backend — calidad y mantenibilidad |
| **Prioridad** | Media |
| **QA scope** | API |
| **Estimación sugerida** | S |

---

## Historia de usuario

**Como** desarrollador del backend,  
**quiero** habilitar virtual threads en Spring Boot y reemplazar `ThreadLocal` en el contexto de tenant por `ScopedValue`,  
**para** mejorar la concurrencia I/O-bound con menor riesgo de fugas de memoria o comportamiento errático al escalar hilos virtuales.

---

## Contexto

### Estado actual

- Java **21**, Spring Boot **3.3.5**
- Tomcat con **platform threads** (pool clásico)
- `TenantContext` usa `ThreadLocal<String>` — único `ThreadLocal` propio del proyecto
- `TenantHeaderFilter` hace `set` / `clear()` en `try/finally` (correcto para platform threads)

### Por qué no basta solo activar virtual threads

Con virtual threads, `ThreadLocal` **sigue funcionando** por request si se limpia bien, pero:

| Riesgo con ThreadLocal + muchas virtual threads | Mitigación |
|-------------------------------------------------|------------|
| Memoria: una copia del valor por hilo virtual | `ScopedValue` con alcance estructurado |
| Olvidar `clear()` → retención de contexto | `ScopedValue.where(...).run(...)` cierra el alcance al salir |
| Recomendación de Java 21 para datos acotados al trabajo | Migrar `TenantContext` a **`ScopedValue`** |

Spring Security y el stack Boot 3.3 ya contemplan virtual threads; el cambio crítico **propio** es `TenantContext` + configuración.

---

## Objetivo

1. Activar `spring.threads.virtual.enabled=true`
2. Reemplazar `ThreadLocal` en `TenantContext` por **`ScopedValue<String>`**
3. Ajustar `TenantHeaderFilter` (y cualquier otro punto) al patrón `ScopedValue.where(...).run(...)`
4. Verificar que multitenant y JWT siguen correctos (RNF-005)
5. Documentar la decisión brevemente

---

## Alcance técnico

### 1. Configuración

En `application.yml` (base, no solo `local`):

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

Opcional: revisar `spring.datasource.hikari.maximum-pool-size` — más concurrencia puede presionar el pool JDBC.

### 2. Migración `TenantContext`

**Antes:** `ThreadLocal` + `set` / `get` / `clear`

**Después (orientativo):**

```java
private static final ScopedValue<String> CURRENT_TENANT = ScopedValue.newInstance();

public static String getRequiredTenantId() {
    if (!CURRENT_TENANT.isBound()) {
        throw new TenantRequiredException();
    }
    return CURRENT_TENANT.get();
}

public static Optional<String> getTenantId() {
    return CURRENT_TENANT.isBound()
            ? Optional.of(CURRENT_TENANT.get())
            : Optional.empty();
}

/** Solo para uso en filtros/interceptores que delimitan el request. */
public static void runWithTenant(String tenantId, Runnable action) {
    ScopedValue.where(CURRENT_TENANT, tenantId).run(action);
}
```

- Eliminar `setTenantId` / `clear` públicos si ya no aplican.
- No propagar tenant a hilos hijos con `Executor` sin documentar (futuro: `StructuredTaskScope` si hace falta).

### 3. `TenantHeaderFilter`

Envolver `filterChain.doFilter` dentro de `TenantContext.runWithTenant(...)` cuando haya header válido; sin `try/finally` manual de `clear`.

### 4. Inventario

- [ ] Confirmar que no quedan otros `ThreadLocal` propios (grep en `backend/`)
- [ ] Spring Security: confiar en Boot 3.3 + virtual threads; sin cambios salvo regresión en tests

---

## Criterios de aceptación

### Virtual threads

- [ ] `spring.threads.virtual.enabled=true` en configuración compartida
- [ ] La app arranca y atiende requests en local

### Tenant context

- [ ] `TenantContext` no usa `ThreadLocal`
- [ ] Usa `ScopedValue` con API acotada (`getRequiredTenantId`, `getTenantId`, `runWithTenant`)
- [ ] `TenantHeaderFilter` delimita el alcance con `runWithTenant`
- [ ] Sin regresión de aislamiento: tenant del header vs JWT (comportamiento actual)

### Calidad

- [ ] `./scripts/ci.sh backend` en verde
- [ ] Tests de integración multitenant existentes pasan (`CrossTenantAccessIT`, etc.)
- [ ] Nota en `docs/15-backend-modules-and-api.md` o ADR corto: virtual threads + ScopedValue

### Fuera de alcance

- Benchmark formal de RPS / load test (opcional follow-up)
- WebFlux / stack reactivo
- Cambiar pool Hikari salvo comentario en PR si se ajusta

---

## Plan de pruebas (dev)

```bash
./scripts/ci.sh backend
```

Manual:

1. Login + request con `X-Tenant-Id` → operación CRUD normal
2. Request sin tenant donde se exige → `TENANT_REQUIRED`
3. Header tenant ≠ tenant del JWT → error (comportamiento actual)
4. (Opcional) Verificar en logs/actuator que Tomcat usa virtual threads si hay endpoint de métricas

---

## Pasos para QA

**QA scope:** API — regresión multitenant y auth, sin cambio de contratos REST.

1. Flujo login + listar owners / water-rights con `X-Tenant-Id`
2. Cross-tenant: datos de otro tenant no visibles
3. Crear pago/factura con tenant correcto

---

## Valor

- Mejor preparación para concurrencia sin reescribir a reactivo
- Contexto de tenant alineado con prácticas Java 21 y virtual threads
- Menor riesgo de fugas por `ThreadLocal` olvidado

---

## Referencias

- [JEP 444 — Virtual Threads](https://openjdk.org/jeps/444)
- [JEP 446 — Scoped Values](https://openjdk.org/jeps/446)
- [Spring Boot 3.2+ Virtual Threads](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.spring-application.virtual-threads)
- Código: `shared/multitenancy/TenantContext.java`, `TenantHeaderFilter.java`
- RNF-005 — Seguridad del tenant

---

## Frase para el agente / board

`Dev: toma la RNF-017`
