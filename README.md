# MDVTools 1.0.18

## MDVTools 1.0.18 - Objetos Imperecederos y Reliquias

- Escucha el evento `CustomDurabilityDamage` de MMOItems por reflexión.
- Cancela el desgaste custom cuando el item final tiene `unbreakable: true`.
- No depende del ID, nombre, tipo ni tier del modificador.
- También evita el coste manual de durabilidad de habilidades de MDVTools.
- No usa tareas por tick ni escanea inventarios.


## MDVTools 1.0.17 - Custom drops por profesión

- Cada entrada de `custom-drops.yml` puede usar `category: FARMING`, `MINING`, `WOODCUTTING`, `NONE` o `AUTO`.
- El bonus total de profesión + armadura + herramienta aumenta de forma relativa la chance base de los drops de su categoría.
- Los bloques adicionales de Rotura Múltiple y Tala Múltiple también hacen un sorteo individual de custom drops.
- Minería y tala mantienen además su sorteo absoluto independiente de `MDVHeadOres` para obtener una unidad extra del nodo/mineral.

Ejemplo: un drop `MINING` con `chance: 0.08` y bonus total `+50%` queda en `0.12%`. Esto no cambia el sorteo separado de MDVHeadOres.


## MDVTools 1.0.17 - Formas de minería, herramientas de profesión y MMOCore

### Formas nuevas para picos

La forma se define por lore y `Rotura Multiple` continúa indicando cuántos bloques **extra** puede romper como máximo.

```yml
# Línea tradicional: bloque normal + 2 extra
- '&3 &7■ &fRotura Multiple: 2'
- '&3 &7■ &fForma de Pico: Linea'

# Plano 3x3 completo: bloque normal + 8 extra
- '&3 &7■ &fRotura Multiple: 8'
- '&3 &7■ &fForma de Pico: Plano 3x3'

# Rectángulo 4x4 completo: bloque normal + 15 extra
- '&3 &7■ &fRotura Multiple: 15'
- '&3 &7■ &fForma de Pico: Rectangulo 4x4'

# Cubo 3x3x3 completo: bloque normal + 26 extra
- '&3 &7■ &fRotura Multiple: 26'
- '&3 &7■ &fForma de Pico: Cubo 3x3x3'
```

Alias reconocidos: `LINE/LINEA`, `PLANE/PLANO/CUADRADO/RECTANGULO/AREA` y `CUBE/CUBO`. El plano se orienta perpendicularmente hacia donde mira el jugador. Solo rompe materiales incluidos en `mining.allowed-blocks` y conserva las comprobaciones de protección.

### Bonus de drops en herramientas

Las mismas líneas utilizadas por las armaduras ahora pueden estar en herramientas:

```yml
- '&8Agricultura Drops raros: &a+2%'
- '&8Minerales Raros: &a+2%'
- '&8Nodos de arbol extra: &a+2%'
```

Las herramientas se leen exclusivamente desde la **mano principal**. La offhand y el inventario no aportan bonus. Con `require-matching-tool-type: true`, agricultura solo cuenta en azadas, minerales en picos y nodos de árbol en hachas.

### Bonus por profesiones de MMOCore

Los IDs configurados por defecto son los nombres de tus archivos:

- `mining`
- `farming`
- `woodcutting`

Valores provisionales incluidos:

- Minero: `+0.60%` por nivel.
- Agricultor: `+0.75%` por nivel.
- Leñador: `+0.50%` por nivel.

Todo es configurable en `profession-bonuses`. Los bonus de nivel se suman a armadura y herramienta.

### Placeholders

Requieren PlaceholderAPI:

```text
%mdvtools_mining_level%
%mdvtools_mining_bonus%
%mdvtools_mining_armor_bonus%
%mdvtools_mining_tool_bonus%
%mdvtools_mining_equipment_bonus%
%mdvtools_mining_total_bonus%
```

Cambia `mining` por `farming` o `woodcutting`. También funcionan `minero`, `agricultor` y `lenador`. Añade `_formatted` al final para recibir un valor como `+12.5%`.

## MDVTools 1.0.15 - Gracia inicial de movimiento en TPA

- El TPA mantiene una espera total configurable de 4 segundos.
- Durante el primer segundo después de aceptar, el solicitante todavía puede moverse sin cancelar.
- Después quedan 3 segundos en los que cualquier movimiento sí cancela el teletransporte.
- Recibir daño o atacar sigue cancelando inmediatamente para evitar abusos.
- Nueva opción: `tpa.movement-grace-seconds`.

## MDVTools 1.0.14 - Identificación probabilística

Añade pergaminos configurables para revelar objetos no identificados de MMOItems con probabilidad de éxito y tier máximo. El sistema reconoce los pergaminos por su TYPE + ID interno; el lore es solamente visual.

Los pergaminos de este módulo **no deben usar `can-identify: true`**, porque esa opción activa la identificación nativa garantizada de MMOItems.

Si ya existe una carpeta `plugins/MDVTools`, no se borra: se añade manualmente el bloque `identification:` del `config.yml` incluido y se ejecuta `/mdvtools reload`.


Herramientas de profesión para MDVCRAFT.


## Cambios 1.0.5

Agrega `weapon-swap-lock`, un bloqueo corto al cambiar a un arma MMOItems para evitar abuso de habilidades por hotbar swap.

Funcionamiento:

1. El jugador cambia a un arma configurada en `mmoitems-types`.
2. Durante 40 ticks por defecto, no puede usar clicks del arma.
3. Se bloquean clicks/interacciones, golpe melee y disparo vanilla de arco/ballesta mientras dura el bloqueo.
4. No afecta consumibles, comida, pociones, materiales ni items normales.
5. Al terminar, reproduce un sonido y partícula sutil.

El sistema es por eventos (`PlayerItemHeldEvent`, `PlayerSwapHandItemsEvent`, `PlayerInteractEvent`, `EntityDamageByEntityEvent`, `EntityShootBowEvent`), sin loops por tick ni escaneo permanente de jugadores.

### Config nueva 1.0.5

Si ya tenías `config.yml`, agrega manualmente el bloque `weapon-swap-lock` del `config.yml` incluido en esta versión.

## Cambios 1.0.4

Agrega pasiva configurable para ballestas con lore:

```yml
- '&6&lPasiva: &eRecarga Automática al Impacto'
```

Funcionamiento:

1. El jugador dispara una ballesta con el lore configurado.
2. MDVTools marca el proyectil disparado.
3. Si el proyectil golpea una entidad, intenta recargar automáticamente la ballesta que el jugador tenga en mano principal o secundaria.
4. Consume 1 flecha al recargar para evitar munición infinita.

El sistema es por eventos (`EntityShootBowEvent` y `ProjectileHitEvent`), sin loops por tick ni escaneo permanente de jugadores.

## Config nueva 1.0.4

Si ya tenías `config.yml`, agrega manualmente:

```yml
crossbow-auto-reload:
  enabled: true

  lore-lines:
    - "Recarga Automatica al Impacto"
    - "Recarga Automática al Impacto"

  only-on-entity-hit: true
  consume-arrow: true
  ammo-material: ARROW
  cooldown-ms: 150
  projectile-ttl-ticks: 400
  require-player-online: true

  sound:
    enabled: true
    value: "item.crossbow.loading_end"
    volume: 0.8
    pitch: 1.3

  particles:
    enabled: true
    particle: "CRIT"
    amount: 8
```

## Cambios 1.0.3

Agrega bonus leídos desde lore de armaduras/items equipados:

```yml
- '&8Agricultura Drops raros: &a+2%'
- '&8Minerales Raros: &a+2%'
- '&8Nodos de arbol extra: &a+2%'
```

### Agricultura Drops raros

Afecta los drops de `custom-drops.yml` en cultivos. Es un bonus relativo.

Ejemplo:

- Chance base: `0.25%`
- Bonus total equipado: `+8%`
- Chance final: `0.25 * 1.08 = 0.27%`

### Minerales Raros

Afecta vetas custom de MDVHeadOres. Es chance absoluta de dropear `+1` item extra del mismo mineral custom.

Ejemplo: `Minerales Raros: +10%` = 10% de obtener 1 mineral extra al romper una veta custom.

### Nodos de arbol extra

Afecta nodos custom de árbol de MDVHeadOres. Es chance absoluta de dropear `+1` item extra del mismo nodo.

Ejemplo: `Nodos de arbol extra: +10%` = 10% de obtener 1 drop extra al romper un nodo custom.

## Compilación

Proyecto Maven para Java 21 / Paper API 1.21.6.

```bash
mvn clean package
```

El `.jar` queda en `target/MDVTools-1.0.18.jar`.


## MDVTools 1.0.9

### Weapon swap lock

- Nuevo `weapon-swap-lock.ignore-return-to-same-weapon-after-non-weapon`.
- Si el jugador cambia de arma a consumible/item no arma y vuelve a la misma arma, no se aplica un bloqueo nuevo.
- Si vuelve a un arma distinta, el bloqueo se aplica normal.
- Pensado para permitir tomar una poción/maná y volver al arma principal sin castigar la habilidad.


## two-handed-ability-lock

Bloquea habilidades de MMOItems/MythicLib cuando el arma principal es de dos manos y la offhand está ocupada. No bloquea ataques básicos ni consumibles; solo cancela el casteo de habilidades. Puede detectar `two-handed` por NBT de MMOItems o por lore configurable como `Dos Manos`.


## ability-durability-cost

Cobra 1 punto de durabilidad custom de MMOItems cuando un jugador castea una habilidad de MMOItems/MythicLib desde el item en mano principal. No agrega mensajes propios y solo afecta items con `max-durability` custom de MMOItems.
