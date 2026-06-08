# MDVTools 1.0.3

Herramientas de profesión para MDVCRAFT.

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

## Config nueva

Si ya tenías `config.yml`, agrega manualmente:

```yml
equipment-bonuses:
  enabled: true
  max-total-bonus-percent: 100.0

  lore:
    agriculture-rare-drops: "Agricultura Drops raros"
    rare-minerals: "Minerales Raros"
    tree-node-extra: "Nodos de arbol extra"

  agriculture-rare-drops:
    enabled: true

  rare-minerals:
    enabled: true
    extra-amount: 1

  tree-node-extra:
    enabled: true
    extra-amount: 1

  mdvheadores:
    plugin-name: "MDVHeadOres"
```

## Compilación

Proyecto Maven para Java 21 / Paper API 1.21.6.

```bash
mvn clean package
```

El `.jar` queda en `target/MDVTools-1.0.3.jar`.
