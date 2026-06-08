# MDVTools 1.0.4

Herramientas de profesión para MDVCRAFT.

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

El `.jar` queda en `target/MDVTools-1.0.4.jar`.
