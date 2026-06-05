# MDVTools

Plugin para MDVCRAFT que añade herramientas de profesión leyendo el lore de items de MMOItems.

## Lore soportado

```yaml
- '&3 &7■ &fTala Multiple: 1'
- '&3 &7■ &fRotura Multiple: 2'
- '&3 &7■ &fMulti Cosecha: 2'
- '&3 &7■ &fAuto Replantar'
```

Los números indican bloques/cultivos extra.

Ejemplo: `Rotura Multiple: 2` rompe el bloque normal + 2 bloques extra.

## Comando

```txt
/mdvtools reload
```

## Compilar con GitHub Actions

1. Subir el contenido a un repositorio GitHub.
2. Ir a Actions.
3. Ejecutar `Build MDVTools`.
4. Descargar el artifact `MDVTools-jar`.
5. Subir el `.jar` a `/plugins/`.


## MDVTools 1.0.1

Añade parche anti-bloques fantasma para minado ultrarrápido.

Config principal:

```yaml
anti-ghost:
  enabled: true
  update-neighbors: true
  update-nearby-players: true
  player-radius: 8
  delays:
    - 1
    - 3
  only-tools-with-mdv-lore: false
```

Este parche no rompe bloques ni cambia drops. Solo reenvía al cliente el estado real del bloque después de minar.
